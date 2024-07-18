package me.kubbidev.blocktune.server.instance;

import com.google.common.base.Preconditions;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.server.MinecraftServer;
import me.kubbidev.blocktune.server.event.instance.InstanceAsyncChunkLoadEvent;
import me.kubbidev.blocktune.server.event.instance.InstanceAsyncChunkUnloadEvent;
import me.kubbidev.blocktune.server.instance.anvil.AnvilLoader;
import me.kubbidev.blocktune.server.instance.block.TunedBlock;
import me.kubbidev.blocktune.server.instance.block.TunedBlockHandler;
import me.kubbidev.blocktune.server.instance.block.rule.TunedBlockPlacementRule;
import me.kubbidev.blocktune.server.thread.ThreadDispatcher;
import me.kubbidev.blocktune.server.util.async.AsyncUtil;
import me.kubbidev.blocktune.server.util.chunk.ChunkCache;
import me.kubbidev.blocktune.server.util.chunk.ChunkSupplier;
import me.kubbidev.blocktune.server.util.collection.Long2ObjectSyncMap;
import me.kubbidev.blocktune.server.world.DimensionType;
import me.kubbidev.nexuspowered.Events;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static me.kubbidev.blocktune.server.util.chunk.ChunkUtil.*;

/**
 * InstanceContainer is an instance that contains chunks.
 */
public class InstanceContainer extends Instance {
    private static final AnvilLoader DEFAULT_LOADER = new AnvilLoader("world");

    private static final BlockFace[] BLOCK_UPDATE_FACES = new BlockFace[]{
            BlockFace.WEST,
            BlockFace.EAST,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.DOWN,
            BlockFace.UP
    };

    // (chunk index -> chunk) map, contains all the chunks in the instance
    // used as a monitor when access is required
    private final Long2ObjectSyncMap<Chunk> chunks = Long2ObjectSyncMap.hashmap();
    private final Map<Long, CompletableFuture<Chunk>> loadingChunks = new ConcurrentHashMap<>();

    private final Lock changingBlockLock = new ReentrantLock();
    private final Map<Location, TunedBlock> currentlyChangingBlocks = new HashMap<>();

    // the chunk loader, used when trying to load/save a chunk from another source
    private IChunkLoader chunkLoader;

    // used to automatically enable the chunk loading or not
    private boolean autoChunkLoad = true;

    // used to supply a new chunk object at a location when requested
    private ChunkSupplier chunkSupplier;

    // time at which the last block change happened (#setBlock)
    private long lastBlockChangeTime;

    public InstanceContainer(@NotNull BlockTune plugin, @NotNull World bukkitInstance) {
        this(plugin, bukkitInstance, null);
    }

    public InstanceContainer(@NotNull BlockTune plugin, @NotNull World bukkitInstance, @Nullable IChunkLoader loader) {
        super(plugin, bukkitInstance);
        setChunkSupplier(DynamicChunk::new);
        setChunkLoader(Objects.requireNonNullElse(loader, DEFAULT_LOADER));
        this.chunkLoader.loadInstance(this);
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull TunedBlock block, boolean doBlockUpdates) {
        Chunk chunk = getChunkAt(x, z);
        if (chunk == null) {
            Preconditions.checkArgument(hasEnabledAutoChunkLoad(),
                    "Tried to set a block to an unloaded chunk");
            chunk = loadChunk(getChunkCoordinate(x), getChunkCoordinate(z)).join();
        }
        if (isLoaded(chunk)) {
            UNSAFE_setBlock(chunk, x, y, z, block, null, null, doBlockUpdates, 0);
        }
    }

    /**
     * Sets a block at the specified location.
     * <p>
     * Unsafe because the method is not synchronized and it does not verify if the chunk is loaded or not.
     *
     * @param chunk the {@link Chunk} which should be loaded
     * @param x     the block X
     * @param y     the block Y
     * @param z     the block Z
     * @param block the block to place
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private synchronized void UNSAFE_setBlock(@NotNull Chunk chunk, int x, int y, int z, @NotNull TunedBlock block,
                                              @Nullable TunedBlockHandler.Placement placement,
                                              @Nullable TunedBlockHandler.Destroy destroy,
                                              boolean doBlockUpdates, int updateDistance) {
        DimensionType dim = getCachedDimensionType();
        if (y >= dim.maxY() || y < dim.minY()) {
            getPlugin().getLogger().warning("tried to set a block outside the world bounds, should be within ["
                    + dim.minY() + ", " + dim.maxY() + "): " + y);
            return;
        }

        synchronized (chunk) {
            // refresh the last block change time
            this.lastBlockChangeTime = System.currentTimeMillis();
            Location blockLocation = new Location(getBukkitInstance(), x, y, z);
            if (isAlreadyChanged(blockLocation, block)) { // do NOT change the block again.
                // Avoids StackOverflowExceptions when onDestroy tries to destroy the block itself
                // This can happen with nether portals which break the entire frame when a portal block is broken
                return;
            }
            this.currentlyChangingBlocks.put(blockLocation, block);

            // change id based on neighbors
            TunedBlockPlacementRule blockPlacementRule = MinecraftServer.getBlockManager().getBlockPlacementRule(block);
            if (placement != null && blockPlacementRule != null && doBlockUpdates) {
                TunedBlockPlacementRule.PlacementState rulePlacement;
                if (placement instanceof TunedBlockHandler.PlayerPlacement pp) {
                    rulePlacement = new TunedBlockPlacementRule.PlacementState(
                            this, block, pp.getBlockFace(),
                            blockLocation,
                            new Vector(
                                    pp.getCursorX(),
                                    pp.getCursorY(),
                                    pp.getCursorZ()
                            ),
                            pp.getPlayer().getLocation(),
                            pp.getPlayer().getEquipment().getItem(pp.getHand()),
                            pp.getPlayer().isSneaking()
                    );
                } else {
                    rulePlacement = new TunedBlockPlacementRule.PlacementState(
                            this, block, null, blockLocation,
                            null, null, null,
                            false
                    );
                }

                block = blockPlacementRule.blockPlace(rulePlacement);
                if (block == null) {
                    block = TunedBlock.AIR;
                }
            }

            // set the block
            chunk.setBlock(x, y, z, block, placement, destroy);

            // refresh neighbors since a new block has been placed
            if (doBlockUpdates) {
                executeNeighboursBlockPlacementRule(blockLocation, updateDistance);
            }
        }
    }

    @Override
    public boolean placeBlock(TunedBlockHandler.@NotNull Placement placement, boolean doBlockUpdates) {
        Location blockLocation = placement.getBlockLocation();
        Chunk chunk = getChunkAt(blockLocation);
        if (!isLoaded(chunk)) {
            return false;
        }

        int x = blockLocation.getBlockX();
        int y = blockLocation.getBlockY();
        int z = blockLocation.getBlockZ();
        UNSAFE_setBlock(chunk, x, y, z, placement.getBlock(), placement, null, doBlockUpdates, 0);
        return true;
    }

    @Override
    public boolean breakBlock(@NotNull Player player, @NotNull Location blockLocation, @NotNull BlockFace blockFace, boolean doBlockUpdates) {
        Chunk chunk = getChunkAt(blockLocation);
        Objects.requireNonNull(chunk, "You cannot break blocks in a null chunk!");
        if (!isLoaded(chunk)) {
            return false;
        }

        TunedBlock block = getBlock(blockLocation);
        int x = blockLocation.getBlockX();
        int y = blockLocation.getBlockY();
        int z = blockLocation.getBlockZ();
        UNSAFE_setBlock(chunk, x, y, z, TunedBlock.AIR, null,
                new TunedBlockHandler.PlayerDestroy(block, this, blockLocation, player), doBlockUpdates, 0);
        return true;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Chunk> loadChunk(int chunkX, int chunkZ) {
        return loadOrRetrieve(chunkX, chunkZ, () -> retrieveChunk(chunkX, chunkZ));
    }

    @Override
    public @NotNull CompletableFuture<@Nullable Chunk> loadOptionalChunk(int chunkX, int chunkZ) {
        return loadOrRetrieve(chunkX, chunkZ, () -> hasEnabledAutoChunkLoad() ? retrieveChunk(chunkX, chunkZ) : AsyncUtil.empty());
    }

    @Override
    public synchronized void unloadChunk(@NotNull Chunk chunk) {
        if (!isLoaded(chunk)) {
            return;
        }
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        InstanceAsyncChunkUnloadEvent event = new InstanceAsyncChunkUnloadEvent(this, chunk);
        Events.callAsync(event);
        // Clear cache
        this.chunks.remove(getChunkIndex(chunkX, chunkZ));
        chunk.unload();
        this.chunkLoader.unloadChunk(chunk);
        ThreadDispatcher<Chunk> dispatcher = MinecraftServer.process().dispatcher();
        dispatcher.deletePartition(chunk);
    }

    @Override
    public @Nullable Chunk getChunk(int chunkX, int chunkZ) {
        return this.chunks.get(getChunkIndex(chunkX, chunkZ));
    }

    @Override
    public @NotNull CompletableFuture<Void> saveChunkToStorage(@NotNull Chunk chunk) {
        return this.chunkLoader.saveChunk(chunk);
    }

    @Override
    public @NotNull CompletableFuture<Void> saveChunksToStorage() {
        return this.chunkLoader.saveChunks(getChunks());
    }

    protected @NotNull CompletableFuture<@NotNull Chunk> retrieveChunk(int chunkX, int chunkZ) {
        CompletableFuture<Chunk> completableFuture = new CompletableFuture<>();
        long index = getChunkIndex(chunkX, chunkZ);
        CompletableFuture<Chunk> prev = this.loadingChunks.putIfAbsent(index, completableFuture);
        if (prev != null) {
            return prev;
        }
        IChunkLoader loader = this.chunkLoader;
        Runnable retriever = () -> loader.loadChunk(this, chunkX, chunkZ)
                .thenCompose(chunk -> {
                    if (chunk != null) {
                        // chunk has been loaded from storage
                        return CompletableFuture.completedFuture(chunk);
                    } else {
                        // loader couldn't load the chunk, generate it
                        return createChunk(chunkX, chunkZ).whenComplete((c, a) -> c.onGenerate());
                    }
                })
                // cache the retrieved chunk
                .thenAccept(chunk -> {
                    // TODO run in the instance thread?
                    cacheChunk(chunk);
                    chunk.onLoad();

                    InstanceAsyncChunkLoadEvent event = new InstanceAsyncChunkLoadEvent(this, chunk);
                    Events.callAsync(event);

                    CompletableFuture<Chunk> future = this.loadingChunks.remove(index);
                    assert future == completableFuture : "Invalid future: " + future;
                    completableFuture.complete(chunk);
                })
                .exceptionally(throwable -> {
                    MinecraftServer.getExceptionManager().handleException(throwable);
                    return null;
                });
        if (loader.supportsParallelLoading()) {
            CompletableFuture.runAsync(retriever);
        } else {
            retriever.run();
        }
        return completableFuture;
    }

    protected @NotNull CompletableFuture<@NotNull Chunk> createChunk(int chunkX, int chunkZ) {
        Chunk chunk = this.chunkSupplier.createChunk(this, chunkX, chunkZ);
        Objects.requireNonNull(chunk, "Chunks supplied by a ChunkSupplier cannot be null.");
        // no chunk generator
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void enableAutoChunkLoad(boolean enable) {
        this.autoChunkLoad = enable;
    }

    @Override
    public boolean hasEnabledAutoChunkLoad() {
        return this.autoChunkLoad;
    }

    @Override
    public boolean isInVoid(@NotNull Location location) {
        // TODO: more customizable
        return location.getY() < getCachedDimensionType().minY() - 64;
    }

    /**
     * Changes which type of {@link Chunk} implementation to use once one needs to be loaded.
     * <p>
     * Uses {@link DynamicChunk} by default.
     *
     * @param chunkSupplier the new {@link ChunkSupplier} of this instance, chunks need to be non-null
     * @throws NullPointerException if {@code chunkSupplier} is null
     */
    @Override
    public void setChunkSupplier(@NotNull ChunkSupplier chunkSupplier) {
        this.chunkSupplier = chunkSupplier;
    }

    /**
     * Gets the current {@link ChunkSupplier}.
     * <p>
     * You shouldn't use it to generate a new chunk, but as a way to view which one is currently in use.
     *
     * @return the current {@link ChunkSupplier}
     */
    @Override
    public ChunkSupplier getChunkSupplier() {
        return this.chunkSupplier;
    }

    /**
     * Copies all the chunks of this instance and create a new instance container with all of them.
     * <p>
     * Chunks are copied with {@link Chunk#copy(Instance, int, int)},
     * {@link UUID} is randomized and {@link DimensionType} is passed over.
     *
     * @return an {@link InstanceContainer} with the exact same chunks as 'this'
     */
    public synchronized InstanceContainer copy() {
        InstanceContainer copiedInstance = new InstanceContainer(getPlugin(), getBukkitInstance());
        copiedInstance.lastBlockChangeTime = this.lastBlockChangeTime;
        for (Chunk chunk : this.chunks.values()) {
            int chunkX = chunk.getChunkX();
            int chunkZ = chunk.getChunkZ();
            Chunk copiedChunk = chunk.copy(copiedInstance, chunkX, chunkZ);
            copiedInstance.cacheChunk(copiedChunk);
        }
        return copiedInstance;
    }

    /**
     * Gets the last time at which a block changed.
     *
     * @return the time at which the last block changed in milliseconds, 0 if never
     */
    public long getLastBlockChangeTime() {
        return this.lastBlockChangeTime;
    }

    /**
     * Signals the instance that a block changed.
     * <p>
     * Useful if you change blocks values directly using a {@link Chunk} object.
     */
    public void refreshLastBlockChangeTime() {
        this.lastBlockChangeTime = System.currentTimeMillis();
    }

    /**
     * Gets all the instance chunks.
     *
     * @return the chunks of this instance
     */
    @Override
    public @NotNull Collection<@NotNull Chunk> getChunks() {
        return this.chunks.values();
    }

    /**
     * Gets the {@link IChunkLoader} of this instance.
     *
     * @return the {@link IChunkLoader} of this instance
     */
    public @NotNull IChunkLoader getChunkLoader() {
        return this.chunkLoader;
    }

    /**
     * Changes the {@link IChunkLoader} of this instance (to change how chunks are retrieved when not already loaded).
     *
     * <p>{@link IChunkLoader#noop()} can be used to do nothing.</p>
     *
     * @param chunkLoader the new {@link IChunkLoader}
     */
    public void setChunkLoader(@NotNull IChunkLoader chunkLoader) {
        this.chunkLoader = Objects.requireNonNull(chunkLoader, "Chunk loader cannot be null");
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        // clear block change map
        Lock wrlock = this.changingBlockLock;
        wrlock.lock();
        this.currentlyChangingBlocks.clear();
        wrlock.unlock();
    }

    /**
     * Has this block already changed since last update?
     * Prevents StackOverflow with blocks trying to modify their location in onDestroy or onPlace.
     *
     * @param blockLocation the block location
     * @param block         the block
     * @return true if the block changed since the last update
     */
    private boolean isAlreadyChanged(@NotNull Location blockLocation, @Nullable TunedBlock block) {
        TunedBlock changedBlock = this.currentlyChangingBlocks.get(blockLocation);
        return Objects.equals(changedBlock, block);
    }

    /**
     * Executed when a block is modified, this is used to modify the states of neighbours blocks.
     * <p>
     * For example, this can be used for redstone wires which need an understanding of its neighborhoods to take the right shape.
     *
     * @param blockLocation the location of the modified block
     */
    private void executeNeighboursBlockPlacementRule(@NotNull Location blockLocation, int updateDistance) {
        ChunkCache cache = new ChunkCache(this, null, null);
        for (BlockFace updateFace : BLOCK_UPDATE_FACES) {
            int neighborX = blockLocation.getBlockX() + updateFace.getModX();
            int neighborY = blockLocation.getBlockY() + updateFace.getModY();
            int neighborZ = blockLocation.getBlockZ() + updateFace.getModZ();
            if (neighborY < getCachedDimensionType().minY() || neighborY > getCachedDimensionType().height()) {
                continue;
            }
            TunedBlock neighborBlock = cache.getBlock(neighborX, neighborY, neighborZ, Condition.NONE);
            if (neighborBlock == null)
                continue;
            TunedBlockPlacementRule neighborBlockPlacementRule = MinecraftServer.getBlockManager().getBlockPlacementRule(neighborBlock);
            if (neighborBlockPlacementRule == null || updateDistance >= neighborBlockPlacementRule.maxUpdateDistance()) {
                continue;
            }

            Location neighborLocation = new Location(getBukkitInstance(), neighborX, neighborY, neighborZ);
            TunedBlock newNeighborBlock = neighborBlockPlacementRule.blockUpdate(new TunedBlockPlacementRule.UpdateState(
                    this,
                    neighborLocation,
                    neighborBlock,
                    updateFace.getOppositeFace()
            ));
            if (neighborBlock != newNeighborBlock) {
                Chunk chunk = getChunkAt(neighborLocation);
                if (!isLoaded(chunk)) {
                    continue;
                }
                UNSAFE_setBlock(chunk,
                        neighborLocation.getBlockX(),
                        neighborLocation.getBlockY(),
                        neighborLocation.getBlockZ(), newNeighborBlock,
                        null, null, true, updateDistance + 1
                );
            }
        }
    }

    private CompletableFuture<Chunk> loadOrRetrieve(int chunkX, int chunkZ, Supplier<CompletableFuture<Chunk>> supplier) {
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null) {
            // Chunk already loaded
            return CompletableFuture.completedFuture(chunk);
        }
        return supplier.get();
    }

    private void cacheChunk(@NotNull Chunk chunk) {
        this.chunks.put(getChunkIndex(chunk), chunk);
//        ThreadDispatcher<Chunk> dispatcher = MinecraftServer.process().dispatcher();
//        dispatcher.createPartition(chunk);
    }
}
