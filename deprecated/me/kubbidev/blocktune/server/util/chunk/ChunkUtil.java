package me.kubbidev.blocktune.server.util.chunk;

import me.kubbidev.blocktune.server.instance.Chunk;
import me.kubbidev.blocktune.server.instance.Instance;
import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class ChunkUtil {
    private ChunkUtil() {
    }

    public static boolean isLoaded(@Nullable Chunk chunk) {
        return chunk != null && chunk.isLoaded();
    }

    /**
     * @param xz the instance coordinate to convert
     * @return the chunk X or Z based on the argument
     */
    public static int getChunkCoordinate(double xz) {
        return getChunkCoordinate((int) Math.floor(xz));
    }

    public static int getChunkCoordinate(int xz) {
        // Assume chunk/section size being 16 (4 bits)
        return xz >> 4;
    }

    /**
     * Gets the chunk index of chunk coordinates.
     * <p>
     * Used when you want to store a chunk somewhere without using a reference to the whole object
     * (as this can lead to memory leaks).
     *
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return a number storing the chunk X and Z
     */
    public static long getChunkIndex(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xffffffffL);
    }

    public static long getChunkIndex(@NotNull Chunk chunk) {
        return getChunkIndex(chunk.getChunkX(), chunk.getChunkZ());
    }

    /**
     * Gets the block index of a location.
     *
     * @param x the block X
     * @param y the block Y
     * @param z the block Z
     * @return an index which can be used to store and retrieve later data linked to a block location
     */
    public static int getBlockIndex(int x, int y, int z) {
        x = x % Chunk.CHUNK_SIZE_X;
        z = z % Chunk.CHUNK_SIZE_Z;

        int index = x & 0xF; // 4 bits
        if (y > 0) {
            index |= (y << 4) & 0x07FFFFF0; // 23 bits (24th bit is always 0 because y is positive)
        } else {
            index |= ((-y) << 4) & 0x7FFFFF0; // Make positive and use 23 bits
            index |= 1 << 27; // Set negative sign at 24th bit
        }
        index |= (z << 28) & 0xF0000000; // 4 bits
        return index;
    }

    /**
     * @param index    an index computed from {@link #getBlockIndex(int, int, int)}
     * @param chunkX   the chunk X
     * @param chunkZ   the chunk Z
     * @param instance the instance to build the location from
     * @return the instance location of the block located in {@code index}
     */
    public static @NotNull Location getBlockLocation(int index, int chunkX, int chunkZ, Instance instance) {
        int x = blockIndexToChunkLocationX(index) + Chunk.CHUNK_SIZE_X * chunkX;
        int y = blockIndexToChunkLocationY(index);
        int z = blockIndexToChunkLocationZ(index) + Chunk.CHUNK_SIZE_Z * chunkZ;
        return new Location(instance.getBukkitInstance(), x, y, z);
    }

    /**
     * Converts a block index to a chunk location X.
     *
     * @param index an index computed from {@link #getBlockIndex(int, int, int)}
     * @return the chunk location X (O-15) of the specified index
     */
    public static int blockIndexToChunkLocationX(int index) {
        return index & 0xF; // 0-4 bits
    }

    /**
     * Converts a block index to a chunk location Y.
     *
     * @param index an index computed from {@link #getBlockIndex(int, int, int)}
     * @return the chunk location Y of the specified index
     */
    public static int blockIndexToChunkLocationY(int index) {
        int y = (index & 0x07FFFFF0) >>> 4;
        if (((index >>> 27) & 1) == 1) {
            y = -y; // Sign bit set, invert sign
        }
        return y; // 4-28 bits
    }

    /**
     * Converts a block index to a chunk location Z.
     *
     * @param index an index computed from {@link #getBlockIndex(int, int, int)}
     * @return the chunk location Z (O-15) of the specified index
     */
    public static int blockIndexToChunkLocationZ(int index) {
        return (index >> 28) & 0xF; // 28-32 bits
    }

    /**
     * Converts a global coordinate value to a section coordinate.
     *
     * @param xyz global coordinate
     * @return section coordinate
     */
    public static int toSectionRelativeCoordinate(int xyz) {
        return xyz & 0xF;
    }

    public static int toRegionCoordinate(int chunkCoordinate) {
        return chunkCoordinate >> 5;
    }

    public static int toRegionLocal(int chunkCoordinate) {
        return chunkCoordinate & 0x1F;
    }
}