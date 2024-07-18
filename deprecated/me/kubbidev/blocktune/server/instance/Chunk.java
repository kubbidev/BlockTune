package me.kubbidev.blocktune.server.instance;

import me.kubbidev.blocktune.server.Tickable;
import me.kubbidev.blocktune.server.instance.block.TunedBlock;
import me.kubbidev.blocktune.server.instance.block.TunedBlockHandler;
import me.kubbidev.blocktune.server.util.chunk.ChunkUtil;
import me.kubbidev.blocktune.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public abstract class Chunk implements TunedBlock.Getter, TunedBlock.Setter, Tickable {
    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Z = 16;
    public static final int CHUNK_SECTION_SIZE = 16;

    private final UUID identifier;

    protected final Instance instance;
    protected final int chunkX;
    protected final int chunkZ;
    protected final int minSection;
    protected final int maxSection;

    protected volatile boolean loaded = true;

    public Chunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        this.identifier = UUID.randomUUID();
        this.instance = instance;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        DimensionType instanceDim = instance.getCachedDimensionType();
        this.minSection = instanceDim.minY() / CHUNK_SECTION_SIZE;
        this.maxSection = (instanceDim.minY() + instanceDim.height()) / CHUNK_SECTION_SIZE;
    }

    /**
     * Sets a block at a location.
     * <p>
     * This is used when the previous block has to be destroyed/replaced, meaning that it clears the previous data and update method.
     * <p>
     * WARNING: this method is not thread-safe, the thread-safe version is {@link Instance#setBlock(int, int, int, TunedBlock)}
     * (or any similar instance methods) Otherwise, you can simply do not forget to have this chunk synchronized when this is called.
     *
     * @param x     the block X
     * @param y     the block Y
     * @param z     the block Z
     * @param block the block to place
     */
    @Override
    public void setBlock(int x, int y, int z, @NotNull TunedBlock block) {
        setBlock(x, y, z, block, null, null);
    }

    protected abstract void setBlock(int x, int y, int z, @NotNull TunedBlock block,
                                     @Nullable TunedBlockHandler.Placement placement,
                                     @Nullable TunedBlockHandler.Destroy destroy);

    public abstract @NotNull List<Section> getSections();

    public abstract @NotNull Section getSection(int section);

    public @NotNull Section getSectionAt(int blockY) {
        return getSection(ChunkUtil.getChunkCoordinate(blockY));
    }

    /**
     * Executes a chunk tick.
     * <p>
     * Should be used to update all the blocks in the chunk.
     * <p>
     * WARNING: this method doesn't necessary have to be thread-safe, proceed with caution.
     *
     * @param time the time of the update in milliseconds
     */
    @Override
    public abstract void tick(long time);

    /**
     * Gets the last time that this chunk changed.
     * <p>
     * It is necessary to see if the cached version of this chunk can be used
     * instead of re-writing and compressing everything.
     *
     * @return the last change time in milliseconds
     */
    public abstract long getLastChangeTime();

    /**
     * Creates a copy of this chunk, including blocks state id, custom block id, update data.
     * <p>
     * The chunk location (X/Z) can be modified using the given arguments.
     *
     * @param instance the chunk owner
     * @param chunkX   the chunk X of the copy
     * @param chunkZ   the chunk Z of the copy
     * @return a copy of this chunk with a potentially new instance and location
     */
    public abstract @NotNull Chunk copy(@NotNull Instance instance, int chunkX, int chunkZ);

    /**
     * Resets the chunk, this means clearing all the data making it empty.
     */
    public abstract void reset();

    /**
     * Gets the unique identifier of this chunk.
     * <p>
     * WARNING: this UUID is not persistent but randomized once the object is instantiated.
     *
     * @return the chunk identifier
     */
    public @NotNull UUID getIdentifier() {
        return this.identifier;
    }

    /**
     * Gets the instance where this chunk is stored.
     *
     * @return the linked instance
     */
    public @NotNull Instance getInstance() {
        return this.instance;
    }

    /**
     * Gets the chunk X.
     *
     * @return the chunk X
     */
    public int getChunkX() {
        return this.chunkX;
    }

    /**
     * Gets the chunk Z.
     *
     * @return the chunk Z
     */
    public int getChunkZ() {
        return this.chunkZ;
    }

    /**
     * Gets the lowest (inclusive) section Y available in this chunk.
     *
     * @return the lowest (inclusive) section Y available in this chunk
     */
    public int getMinSection() {
        return this.minSection;
    }

    /**
     * Gets the highest (exclusive) section Y available in this chunk.
     *
     * @return the highest (exclusive) section Y available in this chunk
     */
    public int getMaxSection() {
        return this.maxSection;
    }

    /**
     * Used to verify if the chunk should still be kept in memory.
     *
     * @return true if the chunk is loaded
     */
    public boolean isLoaded() {
        return this.loaded;
    }

    /**
     * Called when the chunk has been successfully loaded.
     */
    protected void onLoad() {
    }

    /**
     * Called when the chunk generator has finished generating the chunk.
     */
    public void onGenerate() {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + this.chunkX + ":" + this.chunkZ + "]";
    }

    /**
     * Sets the chunk as "unloaded".
     */
    protected void unload() {
        this.loaded = false;
    }
}
