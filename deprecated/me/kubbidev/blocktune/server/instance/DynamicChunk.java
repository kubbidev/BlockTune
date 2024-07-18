package me.kubbidev.blocktune.server.instance;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.kubbidev.blocktune.server.instance.block.TunedBlock;
import me.kubbidev.blocktune.server.instance.block.TunedBlockHandler;
import me.kubbidev.blocktune.server.util.chunk.ChunkUtil;
import me.kubbidev.blocktune.server.world.DimensionType;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static me.kubbidev.blocktune.server.util.chunk.ChunkUtil.toSectionRelativeCoordinate;

public class DynamicChunk extends Chunk {
    protected List<Section> sections;

    // Key = ChunkUtil#getBlockIndex
    protected final Int2ObjectOpenHashMap<TunedBlock> entries = new Int2ObjectOpenHashMap<>(0);
    protected final Int2ObjectOpenHashMap<TunedBlock> tickableMap = new Int2ObjectOpenHashMap<>(0);

    private long lastChange;

    public DynamicChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        super(instance, chunkX, chunkZ);
        Section[] sectionsTemp = new Section[this.maxSection - this.minSection];
        Arrays.setAll(sectionsTemp, value -> new Section());
        this.sections = List.of(sectionsTemp);
    }

    @Override
    protected void setBlock(int x, int y, int z, @NotNull TunedBlock block,
                            TunedBlockHandler.@Nullable Placement placement,
                            TunedBlockHandler.@Nullable Destroy destroy) {

        DimensionType instanceDim = this.instance.getCachedDimensionType();
        if (y >= instanceDim.maxY() || y < instanceDim.minY()) {
            this.instance.getPlugin().getLogger().warning("tried to set a block outside the world bounds, should be within ["
                            + instanceDim.minY() + ", " + instanceDim.maxY() + "): " + y);
            return;
        }
        assertLock();

        this.lastChange = System.currentTimeMillis();

        Section section = getSectionAt(y);
        int sectionRelativeX = toSectionRelativeCoordinate(x);
        int sectionRelativeY = toSectionRelativeCoordinate(y);
        int sectionRelativeZ = toSectionRelativeCoordinate(z);

        section.blockPalette().set(
                sectionRelativeX,
                sectionRelativeY,
                sectionRelativeZ,
                block.stateId()
        );

        int index = ChunkUtil.getBlockIndex(x, y, z);
        // handler
        TunedBlockHandler handler = block.handler();
        TunedBlock lastCachedBlock;
        if (handler != null) {
            lastCachedBlock = this.entries.put(index, block);
        } else {
            lastCachedBlock = this.entries.remove(index);
        }
        // block tick
        if (handler != null && handler.isTickable()) {
            this.tickableMap.put(index, block);
        } else {
            this.tickableMap.remove(index);
        }

        // update block handlers
        Location blockLocation = new Location(this.instance.getBukkitInstance(), x, y, z);
        if (lastCachedBlock != null && lastCachedBlock.handler() != null) {
            // previous destroy
            lastCachedBlock.handler().onDestroy(Objects.requireNonNullElseGet(destroy,
                    () -> new TunedBlockHandler.Destroy(lastCachedBlock, this.instance, blockLocation)));
        }
        if (handler != null) {
            // new placement
            final TunedBlock finalBlock = block;
            handler.onPlace(Objects.requireNonNullElseGet(placement,
                    () -> new TunedBlockHandler.Placement(finalBlock, this.instance, blockLocation)));
        }
    }

    @Override
    public @NotNull List<Section> getSections() {
        return this.sections;
    }

    @Override
    public @NotNull Section getSection(int section) {
        return this.sections.get(section - this.minSection);
    }

    @Override
    public void tick(long time) {
        if (this.tickableMap.isEmpty()) return;
        this.tickableMap.int2ObjectEntrySet().fastForEach(entry -> {
            int index = entry.getIntKey();
            TunedBlock block = entry.getValue();
            TunedBlockHandler handler = block.handler();
            if (handler == null) {
                return;
            }
            Location blockLocation = ChunkUtil.getBlockLocation(index, this.chunkX, this.chunkZ, this.instance);
            handler.tick(new TunedBlockHandler.Tick(block, this.instance, blockLocation));
        });
    }

    @Override
    public @Nullable TunedBlock getBlock(int x, int y, int z, @NotNull Condition condition) {
        assertLock();
        if (y < this.minSection * CHUNK_SECTION_SIZE || y >= this.maxSection * CHUNK_SECTION_SIZE)
            return TunedBlock.AIR; // out of bounds

        // verify if the block object is present
        if (condition != Condition.TYPE) {
            TunedBlock entry = !this.entries.isEmpty() ? this.entries.get(ChunkUtil.getBlockIndex(x, y, z)) : null;
            if (entry != null || condition == Condition.CACHED) {
                return entry;
            }
        }
        // retrieve the block from state id
        Section section = getSectionAt(y);
        int blockStateId = section.blockPalette().get(
                toSectionRelativeCoordinate(x),
                toSectionRelativeCoordinate(y),
                toSectionRelativeCoordinate(z)
        );
        return Objects.requireNonNullElse(TunedBlock.fromStateId((short) blockStateId), TunedBlock.AIR);
    }

    @Override
    public long getLastChangeTime() {
        return this.lastChange;
    }

    @Override
    public @NotNull Chunk copy(@NotNull Instance instance, int chunkX, int chunkZ) {
        DynamicChunk dynamicChunk = new DynamicChunk(instance, chunkX, chunkZ);
        dynamicChunk.sections = this.sections.stream().map(Section::clone).toList();
        dynamicChunk.entries.putAll(this.entries);
        return dynamicChunk;
    }

    @Override
    public void reset() {
        for (Section section : this.sections) {
            section.clear();
        }
        this.entries.clear();
    }

    private void assertLock() {
        assert Thread.holdsLock(this) : "Chunk must be locked before access";
    }
}
