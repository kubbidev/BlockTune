package me.kubbidev.blocktune.server.instance.anvil;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import me.kubbidev.blocktune.server.util.chunk.ChunkUtil;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements a thread-safe reader and writer for Minecraft region files.
 *
 * @see <a href="https://minecraft.wiki/w/Region_file_format">Region file format</a>
 */
final class RegionFile implements AutoCloseable {

    private static final int MAX_ENTRY_COUNT = 1024;
    private static final int SECTOR_SIZE = 4096;
    private static final int SECTOR_1MB = 1024 * 1024 / SECTOR_SIZE;
    private static final int HEADER_LENGTH = MAX_ENTRY_COUNT * 2 * 4; // 2 4-byte fields per entry
    private static final int CHUNK_HEADER_LENGTH = 4 + 1; // length + Compression type (todo non constant to support custom compression)

    private static final int COMPRESSION_GZIP = 1;
    private static final int COMPRESSION_ZLIB = 2;
    private static final int COMPRESSION_NONE = 3;

    private static final BinaryTagIO.Reader TAG_READER = BinaryTagIO.unlimitedReader();
    private static final BinaryTagIO.Writer TAG_WRITER = BinaryTagIO.writer();

    public static @NotNull String getFileName(int regionX, int regionZ) {
        return "r." + regionX + "." + regionZ + ".mca";
    }

    private final ReentrantLock lock = new ReentrantLock();
    private final RandomAccessFile file;

    private final int[] locations = new int[MAX_ENTRY_COUNT];
    private final int[] timestamps = new int[MAX_ENTRY_COUNT];
    private final BooleanList freeSectors = new BooleanArrayList(2);

    public RegionFile(@NotNull Path path) throws IOException {
        this.file = new RandomAccessFile(path.toFile(), "rw");

        readHeader();
    }

    public boolean hasChunkData(int chunkX, int chunkZ) {
        this.lock.lock();
        try {
            return this.locations[getChunkIndex(chunkX, chunkZ)] != 0;
        } finally {
            this.lock.unlock();
        }
    }

    public @Nullable CompoundBinaryTag readChunkData(int chunkX, int chunkZ) throws IOException {
        this.lock.lock();
        try {
            if (!hasChunkData(chunkX, chunkZ)) {
                return null;
            }

            int location = this.locations[getChunkIndex(chunkX, chunkZ)];
            this.file.seek((long) (location >> 8) * SECTOR_SIZE); // move to start of first sector
            int length = this.file.readInt();
            int compressionType = this.file.readByte();
            BinaryTagIO.Compression compression = switch (compressionType) {
                case COMPRESSION_GZIP -> BinaryTagIO.Compression.GZIP;
                case COMPRESSION_ZLIB -> BinaryTagIO.Compression.ZLIB;
                case COMPRESSION_NONE -> BinaryTagIO.Compression.NONE;
                default -> throw new IOException("Unsupported compression type: " + compressionType);
            };

            // read the raw content
            byte[] data = new byte[length - 1];
            this.file.read(data);

            // parse it as a compound tag
            return TAG_READER.read(new ByteArrayInputStream(data), compression);
        } finally {
            this.lock.unlock();
        }
    }

    public void writeChunkData(int chunkX, int chunkZ, @NotNull CompoundBinaryTag data) throws IOException {
        // write the data (compressed)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TAG_WRITER.writeNamed(Map.entry("", data), out, BinaryTagIO.Compression.ZLIB);
        byte[] dataBytes = out.toByteArray();
        int chunkLength = CHUNK_HEADER_LENGTH + dataBytes.length;

        int sectorCount = (int) Math.ceil(chunkLength / (double) SECTOR_SIZE);
        Preconditions.checkArgument(sectorCount < SECTOR_1MB, "Chunk data is too large to fit in a region file");

        this.lock.lock();
        try {
            // we don't attempt to reuse the current allocation, just write it to a new location and free the old one.
            int chunkIndex = getChunkIndex(chunkX, chunkZ);
            int oldLocation = this.locations[chunkIndex];

            // Find a new location
            int firstSector = findFreeSectors(sectorCount);
            if (firstSector == -1) {
                firstSector = allocSectors(sectorCount);
            }
            int newLocation = (firstSector << 8) | sectorCount;

            // mark the sectors as used & free the old sectors
            markLocation(oldLocation, true);
            markLocation(newLocation, false);

            // write the chunk data
            this.file.seek((long) firstSector * SECTOR_SIZE);
            this.file.writeInt(chunkLength);
            this.file.writeByte(COMPRESSION_ZLIB);
            this.file.write(dataBytes);

            // update the header and write it
            this.locations[chunkIndex] = newLocation;
            this.timestamps[chunkIndex] = (int) (System.currentTimeMillis() / 1000);
            writeHeader();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        this.file.close();
    }

    private int getChunkIndex(int chunkX, int chunkZ) {
        return (ChunkUtil.toRegionLocal(chunkZ) << 5) | ChunkUtil.toRegionLocal(chunkX);
    }

    private void readHeader() throws IOException {
        this.file.seek(0);
        if (this.file.length() < HEADER_LENGTH) {
            // new file, fill in data
            this.file.write(new byte[HEADER_LENGTH]);
        }

        //todo: addPadding()

        long totalSectors = this.file.length() / SECTOR_SIZE;
        for (int i = 0; i < totalSectors; i++) {
            this.freeSectors.add(true);
        }
        this.freeSectors.set(0, false); // first sector is locations
        this.freeSectors.set(1, false); // second sector is timestamps

        // read locations
        this.file.seek(0);
        for (int i = 0; i < MAX_ENTRY_COUNT; i++) {
            int location = this.locations[i] = this.file.readInt();
            if (location != 0) {
                markLocation(location, false);
            }
        }

        // read timestamps
        for (int i = 0; i < MAX_ENTRY_COUNT; i++) {
            this.timestamps[i] = this.file.readInt();
        }
    }

    private void writeHeader() throws IOException {
        this.file.seek(0);
        for (int location : this.locations) {
            this.file.writeInt(location);
        }
        for (int timestamp : this.timestamps) {
            this.file.writeInt(timestamp);
        }
    }

    private int findFreeSectors(int length) {
        for (int start = 0; start < this.freeSectors.size() - length; start++) {
            boolean found = true;
            for (int i = 0; i < length; i++) {
                if (!this.freeSectors.getBoolean(start++)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return start - length;
            }
        }
        return -1;
    }

    private int allocSectors(int count) throws IOException {
        long eof = this.file.length();
        this.file.seek(eof);

        byte[] emptySector = new byte[SECTOR_SIZE];
        for (int i = 0; i < count; i++) {
            this.freeSectors.add(true);
            this.file.write(emptySector);
        }

        return (int) (eof / SECTOR_SIZE);
    }

    private void markLocation(int location, boolean free) {
        int sectorCount = location & 0xFF;
        int sectorStart = location >> 8;
        Preconditions.checkArgument(sectorStart + sectorCount <= this.freeSectors.size(), "Invalid sector count");
        for (int i = sectorStart; i < sectorStart + sectorCount; i++) {
            this.freeSectors.set(i, free);
        }
    }
}