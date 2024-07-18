package me.kubbidev.blocktune.server.instance;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.server.Tickable;
import me.kubbidev.blocktune.server.event.instance.InstanceTickEvent;
import me.kubbidev.blocktune.server.instance.block.TunedBlock;
import me.kubbidev.blocktune.server.instance.block.TunedBlockHandler;
import me.kubbidev.blocktune.server.util.chunk.ChunkCache;
import me.kubbidev.blocktune.server.util.chunk.ChunkSupplier;
import me.kubbidev.blocktune.server.util.chunk.ChunkUtil;
import me.kubbidev.blocktune.server.world.DimensionType;
import me.kubbidev.nexuspowered.Events;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class Instance implements TunedBlock.Getter, TunedBlock.Setter, Tickable {
    private final BlockTune plugin;

    private boolean registered;
    private final DimensionType cachedDimensionType;

    // tick since the creation of the instance
    private long worldAge;

    private final ChunkCache blockRetriever = new ChunkCache(this, null, null);

    // field for tick events
    private long lastTickAge = System.currentTimeMillis();

    // the bukkit world cached instance
    private final World bukkitInstance;

    public Instance(@NotNull BlockTune plugin, @NotNull World bukkitInstance) {
        this.cachedDimensionType = DimensionType.of(bukkitInstance);
        this.plugin = plugin;
        this.bukkitInstance = bukkitInstance;
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull TunedBlock block) {
        setBlock(x, y, z, block, true);
    }

    public void setBlock(@NotNull Location blockLocation, @NotNull TunedBlock block, boolean doBlockUpdates) {
        setBlock(blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ(), block, doBlockUpdates);
    }

    public abstract void setBlock(int x, int y, int z, @NotNull TunedBlock block, boolean doBlockUpdates);

    @ApiStatus.Internal
    public boolean placeBlock(@NotNull TunedBlockHandler.Placement placement) {
        return placeBlock(placement, true);
    }

    @ApiStatus.Internal
    public abstract boolean placeBlock(@NotNull TunedBlockHandler.Placement placement, boolean doBlockUpdates);

    @ApiStatus.Internal
    public boolean breakBlock(@NotNull Player player, @NotNull Location blockLocation, @NotNull BlockFace blockFace) {
        return breakBlock(player, blockLocation, blockFace, true);
    }

    @ApiStatus.Internal
    public abstract boolean breakBlock(@NotNull Player player, @NotNull Location blockLocation, @NotNull BlockFace blockFace, boolean doBlockUpdates);

    /**
     * Forces the generation of a {@link Chunk}.
     *
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return a {@link CompletableFuture} completed once the chunk has been loaded
     */
    public abstract @NotNull CompletableFuture<@NotNull Chunk> loadChunk(int chunkX, int chunkZ);

    /**
     * Loads the chunk at the given {@link Location} with a callback.
     *
     * @param location the chunk location
     */
    public @NotNull CompletableFuture<@NotNull Chunk> loadChunk(@NotNull Location location) {
        return loadChunk(
                ChunkUtil.getChunkCoordinate(location.getX()),
                ChunkUtil.getChunkCoordinate(location.getZ())
        );
    }

    /**
     * Loads the chunk if the chunk is already loaded or if
     * {@link #hasEnabledAutoChunkLoad()} returns true.
     *
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return a {@link CompletableFuture} completed once the chunk has been processed, can be null if not loaded
     */
    public abstract @NotNull CompletableFuture<@Nullable Chunk> loadOptionalChunk(int chunkX, int chunkZ);

    /**
     * Loads a {@link Chunk} (if {@link #hasEnabledAutoChunkLoad()} returns true)
     * at the given {@link Location} with a callback.
     *
     * @param location the chunk location
     * @return a {@link CompletableFuture} completed once the chunk has been processed, null if not loaded
     */
    @SuppressWarnings("MismatchedJavadocCode")
    public @NotNull CompletableFuture<@Nullable Chunk> loadOptionalChunk(@NotNull Location location) {
        return loadOptionalChunk(
                ChunkUtil.getChunkCoordinate(location.getX()),
                ChunkUtil.getChunkCoordinate(location.getZ())
        );
    }

    /**
     * Schedules the removal of a {@link Chunk}, this method does not promise when it will be done.
     *
     * @param chunk the chunk to unload
     */
    public abstract void unloadChunk(@NotNull Chunk chunk);

    /**
     * Unloads the chunk at the given location.
     *
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     */
    public void unloadChunk(int chunkX, int chunkZ) {
        Chunk chunk = getChunk(chunkX, chunkZ);
        Objects.requireNonNull(chunk, "The chunk at " + chunkX + ":" + chunkZ + " is already unloaded");
        unloadChunk(chunk);
    }

    /**
     * Gets the loaded {@link Chunk} at a location.
     * <p>
     * WARNING: this should only return already-loaded chunk, use {@link #loadChunk(int, int)} or similar to load one instead.
     *
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return the chunk at the specified location, null if not loaded
     */
    public abstract @Nullable Chunk getChunk(int chunkX, int chunkZ);

    /**
     * @param chunkX the chunk X
     * @param chunkZ this chunk Z
     * @return true if the chunk is loaded
     */
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return getChunk(chunkX, chunkZ) != null;
    }

    /**
     * @param location coordinate of a block or other
     * @return true if the chunk is loaded
     */
    public boolean isChunkLoaded(Location location) {
        return isChunkLoaded(
                ChunkUtil.getChunkCoordinate(location.getX()),
                ChunkUtil.getChunkCoordinate(location.getZ())
        );
    }

    /**
     * Saves a {@link Chunk} to permanent storage.
     *
     * @param chunk the {@link Chunk} to save
     * @return future called when the chunk is done saving
     */
    public abstract @NotNull CompletableFuture<Void> saveChunkToStorage(@NotNull Chunk chunk);

    /**
     * Saves multiple chunks to permanent storage.
     *
     * @return future called when the chunks are done saving
     */
    public abstract @NotNull CompletableFuture<Void> saveChunksToStorage();

    public abstract void setChunkSupplier(@NotNull ChunkSupplier chunkSupplier);

    /**
     * Gets the chunk supplier of the instance.
     *
     * @return the chunk supplier of the instance
     */
    public abstract ChunkSupplier getChunkSupplier();

    /**
     * Gets all the instance's loaded chunks.
     *
     * @return an unmodifiable containing all the instance chunks
     */
    public abstract @NotNull Collection<@NotNull Chunk> getChunks();

    /**
     * When set to true, chunks will load automatically when requested.
     * Otherwise using {@link #loadChunk(int, int)} will be required to even spawn a player
     *
     * @param enable enable the auto chunk load
     */
    public abstract void enableAutoChunkLoad(boolean enable);

    /**
     * Gets if the instance should auto load chunks.
     *
     * @return true if auto chunk load is enabled, false otherwise
     */
    public abstract boolean hasEnabledAutoChunkLoad();

    /**
     * Determines whether a location in the void.
     *
     * @param location the location in the world
     * @return true if the point is inside the void
     */
    public abstract boolean isInVoid(@NotNull Location location);

    /**
     * Gets if the instance has been registered in {@link InstanceManager}.
     *
     * @return true if the instance has been registered
     */
    public boolean isRegistered() {
        return this.registered;
    }

    /**
     * Changes the registered field.
     * <p>
     * WARNING: should only be used by {@link InstanceManager}.
     *
     * @param registered true to mark the instance as registered
     */
    protected void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public @NotNull BlockTune getPlugin() {
        return this.plugin;
    }

    @ApiStatus.Internal
    public @NotNull DimensionType getCachedDimensionType() {
        return this.cachedDimensionType;
    }

    /**
     * Gets the age of this instance in tick.
     *
     * @return the age of this instance in tick
     */
    public long getWorldAge() {
        return this.worldAge;
    }

    @Override
    public @Nullable TunedBlock getBlock(int x, int y, int z, @NotNull Condition condition) {
        TunedBlock block = this.blockRetriever.getBlock(x, y, z, condition);
        if (block == null) {
            throw new NullPointerException("Unloaded chunk at " + x + "," + y + "," + z);
        }
        return block;
    }

    /**
     * Gets the {@link Chunk} at the given block location, null if not loaded.
     *
     * @param x the X location
     * @param z the Z location
     * @return the chunk at the given location, null if not loaded
     */
    public @Nullable Chunk getChunkAt(double x, double z) {
        return getChunk(ChunkUtil.getChunkCoordinate(x), ChunkUtil.getChunkCoordinate(z));
    }

    /**
     * Gets the {@link Chunk} at the given {@link Location}, null if not loaded.
     *
     * @param location the location
     * @return the chunk at the given location, null if not loaded
     */
    public @Nullable Chunk getChunkAt(@NotNull Location location) {
        return getChunk(
                ChunkUtil.getChunkCoordinate(location.getX()),
                ChunkUtil.getChunkCoordinate(location.getZ())
        );
    }

    /**
     * Gets the bukkit world instance.
     *
     * @return the bukkit instance
     */
    public @NotNull World getBukkitInstance() {
        return this.bukkitInstance;
    }

    /**
     * Gets the bukkit world instance.
     *
     * @return the bukkit instance
     */
    public @NotNull UUID getIdentifier() {
        return this.bukkitInstance.getUID();
    }

    /**
     * Performs a single tick in the instance.
     * <p>
     * Warning: this does not update chunks.
     *
     * @param time the tick time in milliseconds
     */
    @Override
    public void tick(long time) {
        // time
        {
            this.worldAge++;
        }

        // tick event
        {
            // process tick events
            Events.call(new InstanceTickEvent(this, time, this.lastTickAge));
            // set last tick age
            this.lastTickAge = time;
        }
    }
}
