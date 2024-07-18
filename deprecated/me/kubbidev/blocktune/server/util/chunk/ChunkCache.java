package me.kubbidev.blocktune.server.util.chunk;

import me.kubbidev.blocktune.server.instance.Chunk;
import me.kubbidev.blocktune.server.instance.block.TunedBlock;
import me.kubbidev.blocktune.server.instance.Instance;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import static me.kubbidev.blocktune.server.util.chunk.ChunkUtil.getChunkCoordinate;

@ApiStatus.Internal
public final class ChunkCache implements TunedBlock.Getter {
    private final Instance instance;
    private Chunk chunk;

    private final TunedBlock defaultBlock;

    public ChunkCache(Instance instance, Chunk chunk, TunedBlock defaultBlock) {
        this.instance = instance;
        this.chunk = chunk;
        this.defaultBlock = defaultBlock;
    }

    public ChunkCache(Instance instance, Chunk chunk) {
        this(instance, chunk, TunedBlock.AIR);
    }

    @Override
    public @UnknownNullability TunedBlock getBlock(int x, int y, int z, @NotNull Condition condition) {
        Chunk chunk = this.chunk;
        int chunkX = getChunkCoordinate(x);
        int chunkZ = getChunkCoordinate(z);
        if (chunk == null || !chunk.isLoaded() ||
                chunk.getChunkX() != chunkX || chunk.getChunkZ() != chunkZ) {
            this.chunk = chunk = this.instance.getChunk(chunkX, chunkZ);
        }
        if (chunk != null) {
            synchronized (chunk) {
                return chunk.getBlock(x, y, z, condition);
            }
        } else return this.defaultBlock;
    }
}