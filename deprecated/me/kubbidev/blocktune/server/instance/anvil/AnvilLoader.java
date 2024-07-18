package me.kubbidev.blocktune.server.instance.anvil;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.*;
import me.kubbidev.blocktune.server.MinecraftServer;
import me.kubbidev.blocktune.server.instance.Chunk;
import me.kubbidev.blocktune.server.instance.IChunkLoader;
import me.kubbidev.blocktune.server.instance.Instance;
import me.kubbidev.blocktune.server.instance.Section;
import me.kubbidev.blocktune.server.instance.block.TunedBlock;
import me.kubbidev.blocktune.server.instance.block.TunedBlockHandler;
import me.kubbidev.blocktune.server.util.ArrayUtil;
import me.kubbidev.blocktune.server.util.async.AsyncUtil;
import me.kubbidev.blocktune.server.util.chunk.ChunkUtil;
import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AnvilLoader implements IChunkLoader {

    private final ReentrantLock fileCreationLock = new ReentrantLock();
    private final Map<String, RegionFile> alreadyLoaded = new ConcurrentHashMap<>();
    private final Path path;
    private final Path regionPath;

    private static class RegionCache extends ConcurrentHashMap<IntIntImmutablePair, Set<IntIntImmutablePair>> {
    }

    /**
     * Represents the chunks currently loaded per region. Used to determine when a region file can be unloaded.
     */
    private final RegionCache perRegionLoadedChunks = new RegionCache();
    private final ReentrantLock perRegionLoadedChunksLock = new ReentrantLock();

    // thread local to avoid contention issues with locks
    private final ThreadLocal<Int2ObjectMap<CompoundBinaryTag>> blockStateId2ObjectCacheTLS = ThreadLocal.withInitial(Int2ObjectArrayMap::new);

    public AnvilLoader(@NotNull Path path) {
        this.path = path;
        this.regionPath = path.resolve("blocktune_region");
    }

    public AnvilLoader(@NotNull String path) {
        this(Path.of(path));
    }

    @Override
    public @NotNull CompletableFuture<@Nullable Chunk> loadChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        if (!Files.exists(this.path)) {
            // no world folder
            return CompletableFuture.completedFuture(null);
        }
        try {
            return loadMCA(instance, chunkX, chunkZ);
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            return CompletableFuture.completedFuture(null);
        }
    }

    private @NotNull CompletableFuture<@Nullable Chunk> loadMCA(Instance instance, int chunkX, int chunkZ) throws IOException {
        RegionFile mcaFile = getMCAFile(chunkX, chunkZ);
        if (mcaFile == null) {
            return CompletableFuture.completedFuture(null);
        }
        CompoundBinaryTag chunkData = mcaFile.readChunkData(chunkX, chunkZ);
        if (chunkData == null) {
            return CompletableFuture.completedFuture(null);
        }
        // load the chunk data (assuming it is fully generated)
        Chunk chunk = instance.getChunkSupplier().createChunk(instance, chunkX, chunkZ);
        synchronized (chunk) { // todo: boo, synchronized
            String status = chunkData.getString("status");

            // TODO: Should we handle other statuses?
            if (status.isEmpty() || "minecraft:full".equals(status)) {
                // TODO: Parallelize block and block entities loading
                // blocks
                loadSections(chunk, chunkData);

                // block entities
                loadBlockEntities(chunk, chunkData);
            } else {
                instance.getPlugin().getLogger().warning("Skipping partially generated chunk at "
                        + chunkX + ", " + chunkZ + " with status " + status);
            }
        }

        // cache the index of the loaded chunk
        this.perRegionLoadedChunksLock.lock();
        try {
            int regionX = ChunkUtil.toRegionCoordinate(chunkX);
            int regionZ = ChunkUtil.toRegionCoordinate(chunkZ);
            var chunks = this.perRegionLoadedChunks.computeIfAbsent(new IntIntImmutablePair(regionX, regionZ), r -> new HashSet<>()); // region cache may have been removed on another thread due to unloadChunk
            chunks.add(new IntIntImmutablePair(chunkX, chunkZ));
        } finally {
            this.perRegionLoadedChunksLock.unlock();
        }
        return CompletableFuture.completedFuture(chunk);
    }

    private @Nullable RegionFile getMCAFile(int chunkX, int chunkZ) {
        int regionX = ChunkUtil.toRegionCoordinate(chunkX);
        int regionZ = ChunkUtil.toRegionCoordinate(chunkZ);
        return this.alreadyLoaded.computeIfAbsent(RegionFile.getFileName(regionX, regionZ), n -> {
            Path regionPath = this.regionPath.resolve(n);
            if (!Files.exists(regionPath)) {
                return null;
            }
            this.perRegionLoadedChunksLock.lock();
            try {
                Set<IntIntImmutablePair> previousVersion = this.perRegionLoadedChunks.put(new IntIntImmutablePair(regionX, regionZ), new HashSet<>());
                assert previousVersion == null : "The AnvilLoader cache should not already have data for this region.";
                return new RegionFile(regionPath);
            } catch (IOException e) {
                MinecraftServer.getExceptionManager().handleException(e);
                return null;
            } finally {
                this.perRegionLoadedChunksLock.unlock();
            }
        });
    }

    private void loadSections(@NotNull Chunk chunk, @NotNull CompoundBinaryTag chunkData) {
        for (BinaryTag sectionTag : chunkData.getList("sections", BinaryTagTypes.COMPOUND)) {
            CompoundBinaryTag sectionData = (CompoundBinaryTag) sectionTag;

            int sectionY = sectionData.getInt("Y", Integer.MIN_VALUE);
            Preconditions.checkArgument(sectionY != Integer.MIN_VALUE, "Missing section Y value");
            int yOffset = Chunk.CHUNK_SECTION_SIZE * sectionY;

            if (sectionY < chunk.getMinSection() || sectionY >= chunk.getMaxSection()) {
                // vanilla stores a section below and above the world for lighting, throw it out.
                continue;
            }

            Section section = chunk.getSection(sectionY);

            {   // blocks
                CompoundBinaryTag blockStatesTag = sectionData.getCompound("block_states");
                ListBinaryTag blockPaletteTag = blockStatesTag.getList("palette", BinaryTagTypes.COMPOUND);
                TunedBlock[] convertedPalette = loadBlockPalette(chunk, blockPaletteTag);
                if (blockPaletteTag.size() == 1) {
                    // one solid block, no need to check the data
                    section.blockPalette().fill(convertedPalette[0].stateId());
                } else if (blockPaletteTag.size() > 1) {
                    long[] packedStates = blockStatesTag.getLongArray("data");
                    Preconditions.checkArgument(packedStates.length != 0, "Missing packed states data");
                    int[] blockStateIndices = new int[Chunk.CHUNK_SECTION_SIZE * Chunk.CHUNK_SECTION_SIZE * Chunk.CHUNK_SECTION_SIZE];
                    ArrayUtil.unpack(blockStateIndices, packedStates, packedStates.length * 64 / blockStateIndices.length);

                    for (int y = 0; y < Chunk.CHUNK_SECTION_SIZE; y++) {
                        for (int z = 0; z < Chunk.CHUNK_SECTION_SIZE; z++) {
                            for (int x = 0; x < Chunk.CHUNK_SECTION_SIZE; x++) {
                                try {
                                    int blockIndex = y * Chunk.CHUNK_SECTION_SIZE * Chunk.CHUNK_SECTION_SIZE + z * Chunk.CHUNK_SECTION_SIZE + x;
                                    int paletteIndex = blockStateIndices[blockIndex];
                                    TunedBlock block = convertedPalette[paletteIndex];

                                    chunk.setBlock(x, y + yOffset, z, block);
                                } catch (Exception e) {
                                    MinecraftServer.getExceptionManager().handleException(e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private TunedBlock[] loadBlockPalette(@NotNull Chunk chunk, @NotNull ListBinaryTag paletteTag) {
        TunedBlock[] convertedPalette = new TunedBlock[paletteTag.size()];
        for (int i = 0; i < convertedPalette.length; i++) {
            CompoundBinaryTag paletteEntry = paletteTag.getCompound(i);
            String blockName = paletteEntry.getString("Name");
            if (blockName.equals("blocktune:air")) {
                convertedPalette[i] = TunedBlock.AIR;
            } else {
                TunedBlock block = Objects.requireNonNull(TunedBlock.fromNamespaceId(blockName), "Unknown block " + blockName);
                // properties
                Map<String, String> properties = new HashMap<>();
                CompoundBinaryTag propertiesNBT = paletteEntry.getCompound("Properties");
                for (Map.Entry<String, ? extends BinaryTag> property : propertiesNBT) {
                    if (property.getValue() instanceof StringBinaryTag propertyValue) {
                        properties.put(property.getKey(), propertyValue.value());
                    } else {
                        chunk.getInstance().getPlugin().getLogger().warning("Fail to parse block state properties "
                                + propertiesNBT + ", expected a string for "
                                + property.getKey() + ", but contents were " + TagStringIOExt.writeTag(property.getValue()));
                    }
                }
                if (!properties.isEmpty()) {
                    block = block.withProperties(properties);
                }

                // handler
                TunedBlockHandler handler = MinecraftServer.getBlockManager().getHandler(block.name());
                if (handler != null) {
                    block = block.withHandler(handler);
                }

                convertedPalette[i] = block;
            }
        }
        return convertedPalette;
    }

    private void loadBlockEntities(@NotNull Chunk loadedChunk, @NotNull CompoundBinaryTag chunkData) {
        for (BinaryTag blockEntityTag : chunkData.getList("block_entities", BinaryTagTypes.COMPOUND)) {
            CompoundBinaryTag blockEntity = (CompoundBinaryTag) blockEntityTag;

            int x = blockEntity.getInt("x");
            int y = blockEntity.getInt("y");
            int z = blockEntity.getInt("z");
            TunedBlock block = loadedChunk.getBlock(x, y, z);

            // load the block handler if the id is present
            if (blockEntity.get("id") instanceof StringBinaryTag blockEntityId) {
                TunedBlockHandler handler = MinecraftServer.getBlockManager().getHandlerOrDummy(blockEntityId.value());
                block = block.withHandler(handler);
            }

            loadedChunk.setBlock(x, y, z, block);
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> saveChunk(@NotNull Chunk chunk) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();

        // find the region file or create an empty one if missing
        RegionFile mcaFile;
        this.fileCreationLock.lock();
        try {
            mcaFile = getMCAFile(chunkX, chunkZ);
            if (mcaFile == null) {
                int regionX = ChunkUtil.toRegionCoordinate(chunkX);
                int regionZ = ChunkUtil.toRegionCoordinate(chunkZ);
                String regionFileName = RegionFile.getFileName(regionX, regionZ);
                try {
                    Path regionFile = this.regionPath.resolve(regionFileName);
                    if (!Files.exists(regionFile)) {
                        Files.createDirectories(regionFile.getParent());
                        Files.createFile(regionFile);
                    }

                    mcaFile = new RegionFile(regionFile);
                    this.alreadyLoaded.put(regionFileName, mcaFile);
                } catch (IOException e) {
                    chunk.getInstance().getPlugin().getLogger().severe("Failed to create region file for " + chunkX + ", " + chunkZ);
                    MinecraftServer.getExceptionManager().handleException(e);
                    return AsyncUtil.VOID_FUTURE;
                }
            }
        } finally {
            this.fileCreationLock.unlock();
        }

        try {
            CompoundBinaryTag.Builder chunkData = CompoundBinaryTag.builder();

            chunkData.putInt("DataVersion", MinecraftServer.DATA_VERSION);
            chunkData.putInt("xPos", chunkX);
            chunkData.putInt("zPos", chunkZ);
            chunkData.putInt("yPos", chunk.getMinSection());
            chunkData.putString("status", "minecraft:full");
            chunkData.putLong("LastUpdate", chunk.getInstance().getWorldAge());

            saveSectionData(chunk, chunkData);

            mcaFile.writeChunkData(chunkX, chunkZ, chunkData.build());
        } catch (IOException e) {
            chunk.getInstance().getPlugin().getLogger().severe("Failed to save chunk " + chunkX + ", " + chunkZ);
            MinecraftServer.getExceptionManager().handleException(e);
        }

        return AsyncUtil.VOID_FUTURE;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private void saveSectionData(@NotNull Chunk chunk, @NotNull CompoundBinaryTag.Builder chunkData) {
        ListBinaryTag.Builder<CompoundBinaryTag> sections = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
        ListBinaryTag.Builder<CompoundBinaryTag> blockEntities = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);

        // block arrays reused for each chunk
        List<BinaryTag> blockPaletteEntries = new ArrayList<>();
        IntList blockPaletteIndices = new IntArrayList();

        // map block indices by state id to avoid doing a deep comparison on every block tag
        int[] blockIndices = new int[Chunk.CHUNK_SECTION_SIZE * Chunk.CHUNK_SECTION_SIZE * Chunk.CHUNK_SECTION_SIZE];

        synchronized (chunk) {
            for (int sectionY = chunk.getMinSection(); sectionY < chunk.getMaxSection(); sectionY++) {
                CompoundBinaryTag.Builder sectionData = CompoundBinaryTag.builder();
                sectionData.putByte("Y", (byte) sectionY);

                // build block & collect block entities
                for (int sectionLocalY = 0; sectionLocalY < Chunk.CHUNK_SECTION_SIZE; sectionLocalY++) {
                    for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                        for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
                            int y = sectionLocalY + (sectionY * Chunk.CHUNK_SECTION_SIZE);

                            int blockIndex = x + sectionLocalY * 16 * 16 + z * 16;
                            TunedBlock block = chunk.getBlock(x, y, z);

                            // add block state
                            int blockStateId = block.stateId();
                            CompoundBinaryTag blockState = getBlockState(block);
                            int blockPaletteIndex = blockPaletteIndices.indexOf(blockStateId);
                            if (blockPaletteIndex == -1) {
                                blockPaletteIndex = blockPaletteEntries.size();
                                blockPaletteEntries.add(blockState);
                                blockPaletteIndices.add(blockStateId);
                            }
                            blockIndices[blockIndex] = blockPaletteIndex;

                            // add block entity if present
                            TunedBlockHandler handler = block.handler();
                            if (handler != null) {
                                CompoundBinaryTag.Builder blockEntityTag = CompoundBinaryTag.builder();
                                blockEntityTag.putString("id", handler.getNamespaceId().asString());
                                blockEntityTag.putInt("x", x + Chunk.CHUNK_SIZE_X * chunk.getChunkX());
                                blockEntityTag.putInt("y", y);
                                blockEntityTag.putInt("z", z + Chunk.CHUNK_SIZE_Z * chunk.getChunkZ());
                                blockEntityTag.putByte("keepPacked", (byte) 0);
                                blockEntities.add(blockEntityTag.build());
                            }
                        }
                    }
                }

                // save the block palettes
                CompoundBinaryTag.Builder blockStates = CompoundBinaryTag.builder();
                blockStates.put("palette", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, blockPaletteEntries));
                if (blockPaletteEntries.size() > 1) {
                    // if there is only one entry we do not need to write the packed indices
                    int bitsPerEntry = (int) Math.max(1, Math.ceil(Math.log(blockPaletteEntries.size()) / Math.log(2)));
                    blockStates.putLongArray("data", ArrayUtil.pack(blockIndices, bitsPerEntry));
                }
                sectionData.put("block_states", blockStates.build());

                blockPaletteEntries.clear();
                blockPaletteIndices.clear();

                sections.add(sectionData.build());
            }
        }

        chunkData.put("sections", sections.build());
        chunkData.put("block_entities", blockEntities.build());
    }

    private CompoundBinaryTag getBlockState(TunedBlock block) {
        return this.blockStateId2ObjectCacheTLS.get().computeIfAbsent(block.stateId(), _unused -> {
            CompoundBinaryTag.Builder tag = CompoundBinaryTag.builder();
            tag.putString("Name", block.name());

            if (!block.properties().isEmpty()) {

                @SuppressWarnings("DataFlowIssue")
                Map<String, String> defaultProperties = TunedBlock.fromBlockId(block.id()).properties(); // Never null
                CompoundBinaryTag.Builder propertiesTag = CompoundBinaryTag.builder();

                for (Map.Entry<String, String> entry : block.properties().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (defaultProperties.get(key).equals(value)) {
                        continue; // Skip default values
                    }

                    propertiesTag.putString(key, value);
                }
                CompoundBinaryTag properties = propertiesTag.build();
                if (properties.size() > 0) {
                    tag.put("Properties", properties);
                }
            }
            return tag.build();
        });
    }

    @Override
    public void unloadChunk(Chunk chunk) {
        int regionX = ChunkUtil.toRegionCoordinate(chunk.getChunkX());
        int regionZ = ChunkUtil.toRegionCoordinate(chunk.getChunkZ());
        IntIntImmutablePair regionKey = new IntIntImmutablePair(regionX, regionZ);

        this.perRegionLoadedChunksLock.lock();
        try {
            Set<IntIntImmutablePair> chunks = this.perRegionLoadedChunks.get(regionKey);
            if (chunks != null) { // if null, trying to unload a chunk from a region that was not created by the AnvilLoader
                // don't check return value, trying to unload a chunk not created by the AnvilLoader is valid
                chunks.remove(new IntIntImmutablePair(chunk.getChunkX(), chunk.getChunkZ()));

                if (chunks.isEmpty()) {
                    this.perRegionLoadedChunks.remove(regionKey);
                    RegionFile regionFile = this.alreadyLoaded.remove(RegionFile.getFileName(regionX, regionZ));
                    if (regionFile != null) {
                        try {
                            regionFile.close();
                        } catch (IOException e) {
                            MinecraftServer.getExceptionManager().handleException(e);
                        }
                    }
                }
            }
        } finally {
            this.perRegionLoadedChunksLock.unlock();
        }
    }

    @Override
    public boolean supportsParallelLoading() {
        return true;
    }

    @Override
    public boolean supportsParallelSaving() {
        return true;
    }
}