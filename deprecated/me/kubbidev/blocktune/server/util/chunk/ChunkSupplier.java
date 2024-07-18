package me.kubbidev.blocktune.server.util.chunk;

import me.kubbidev.blocktune.server.instance.Chunk;
import me.kubbidev.blocktune.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

/**
 * Used to customize which type of {@link Chunk} an implementation should use.
 */
@FunctionalInterface
public interface ChunkSupplier {

    /**
     * Creates a {@link Chunk} object.
     *
     * @param instance the linked instance
     * @param chunkX   the chunk X
     * @param chunkZ   the chunk Z
     * @return a newly {@link Chunk} object, cannot be null
     */
    @NotNull
    Chunk createChunk(@NotNull Instance instance, int chunkX, int chunkZ);
}