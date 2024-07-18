package me.kubbidev.blocktune.server.instance.palette;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.kubbidev.blocktune.server.MinecraftServer;
import me.kubbidev.blocktune.server.util.MathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

/**
 * Palette able to take any value anywhere. May consume more memory than required.
 */
final class FlexiblePalette implements SpecializedPalette, Cloneable {
    private static final ThreadLocal<int[]> WRITE_CACHE = ThreadLocal.withInitial(() -> new int[4096]);

    // specific to this palette type
    private final AdaptivePalette adaptivePalette;
    private byte bitsPerEntry;
    private int count;

    private long[] values;
    // palette index = value
    IntArrayList paletteToValueList;
    // value = palette index
    private Int2IntOpenHashMap valueToPaletteMap;

    FlexiblePalette(AdaptivePalette adaptivePalette, byte bitsPerEntry) {
        this.adaptivePalette = adaptivePalette;

        this.bitsPerEntry = bitsPerEntry;

        this.paletteToValueList = new IntArrayList(1);
        this.paletteToValueList.add(0);
        this.valueToPaletteMap = new Int2IntOpenHashMap(1);
        this.valueToPaletteMap.put(0, 0);
        this.valueToPaletteMap.defaultReturnValue(-1);

        int valuesPerLong = 64 / bitsPerEntry;
        this.values = new long[(maxSize() + valuesPerLong - 1) / valuesPerLong];
    }

    FlexiblePalette(AdaptivePalette adaptivePalette) {
        this(adaptivePalette, adaptivePalette.defaultBitsPerEntry);
    }

    @Override
    public int get(int x, int y, int z) {
        int bitsPerEntry = this.bitsPerEntry;
        int sectionIndex = getSectionIndex(dimension(), x, y, z);
        int valuesPerLong = 64 / bitsPerEntry;
        int index = sectionIndex / valuesPerLong;
        int bitIndex = (sectionIndex - index * valuesPerLong) * bitsPerEntry;
        int value = (int) (this.values[index] >> bitIndex) & ((1 << bitsPerEntry) - 1);
        // Change to palette value and return
        return hasPalette() ? this.paletteToValueList.getInt(value) : value;
    }

    @Override
    public void getAll(@NotNull EntryConsumer consumer) {
        retrieveAll(consumer, true);
    }

    @Override
    public void getAllPresent(@NotNull EntryConsumer consumer) {
        retrieveAll(consumer, false);
    }

    @Override
    public void set(int x, int y, int z, int value) {
        value = getPaletteIndex(value);
        int bitsPerEntry = this.bitsPerEntry;
        long[] values = this.values;
        // change to palette value
        int valuesPerLong = 64 / bitsPerEntry;
        int sectionIndex = getSectionIndex(dimension(), x, y, z);
        int index = sectionIndex / valuesPerLong;
        int bitIndex = (sectionIndex - index * valuesPerLong) * bitsPerEntry;

        long block = values[index];
        long clear = (1L << bitsPerEntry) - 1L;
        long oldBlock = block >> bitIndex & clear;
        values[index] = block & ~(clear << bitIndex) | ((long) value << bitIndex);
        // check if block count needs to be updated
        boolean currentAir = oldBlock == 0;
        if (currentAir != (value == 0)) {
            this.count += currentAir ? 1 : -1;
        }
    }

    @Override
    public void fill(int value) {
        if (value == 0) {
            Arrays.fill(this.values, 0);
            this.count = 0;
            return;
        }
        value = getPaletteIndex(value);
        int bitsPerEntry = this.bitsPerEntry;
        int valuesPerLong = 64 / bitsPerEntry;
        long[] values = this.values;
        long block = 0;
        for (int i = 0; i < valuesPerLong; i++) {
            block |= (long) value << i * bitsPerEntry;
        }
        Arrays.fill(values, block);
        this.count = maxSize();
    }

    @Override
    public void setAll(@NotNull EntrySupplier supplier) {
        int[] cache = WRITE_CACHE.get();
        final int dimension = dimension();
        // Fill cache with values
        int fillValue = -1;
        int count = 0;
        int index = 0;
        for (int y = 0; y < dimension; y++) {
            for (int z = 0; z < dimension; z++) {
                for (int x = 0; x < dimension; x++) {
                    int value = supplier.get(x, y, z);
                    // Support for fill fast exit if the supplier returns a constant value
                    if (fillValue != -2) {
                        if (fillValue == -1) {
                            fillValue = value;
                        } else if (fillValue != value) {
                            fillValue = -2;
                        }
                    }
                    // Set value in cache
                    if (value != 0) {
                        value = getPaletteIndex(value);
                        count++;
                    }
                    cache[index++] = value;
                }
            }
        }
        assert index == maxSize();
        // Update palette content
        if (fillValue < 0) {
            updateAll(cache);
            this.count = count;
        } else {
            fill(fillValue);
        }
    }

    @Override
    public void replace(int x, int y, int z, @NotNull IntUnaryOperator operator) {
        int oldValue = get(x, y, z);
        int newValue = operator.applyAsInt(oldValue);
        if (oldValue != newValue) {
            set(x, y, z, newValue);
        }
    }

    @Override
    public void replaceAll(@NotNull EntryFunction function) {
        int[] cache = WRITE_CACHE.get();
        AtomicInteger arrayIndex = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();
        getAll((x, y, z, value) -> {
            int newValue = function.apply(x, y, z, value);
            int index = arrayIndex.getPlain();
            arrayIndex.setPlain(index + 1);
            cache[index] = newValue != value ? getPaletteIndex(newValue) : value;
            if (newValue != 0) {
                count.setPlain(count.getPlain() + 1);
            }
        });
        assert arrayIndex.getPlain() == maxSize();
        // update palette content
        updateAll(cache);
        this.count = count.getPlain();
    }

    @Override
    public int count() {
        return this.count;
    }

    @Override
    public int bitsPerEntry() {
        return this.bitsPerEntry;
    }

    @Override
    public int maxBitsPerEntry() {
        return this.adaptivePalette.maxBitsPerEntry();
    }

    @Override
    public int dimension() {
        return this.adaptivePalette.dimension();
    }

    @Override
    public @NotNull SpecializedPalette clone() {
        try {
            FlexiblePalette palette = (FlexiblePalette) super.clone();
            palette.values = this.values != null ? this.values.clone() : null;
            palette.paletteToValueList = this.paletteToValueList.clone();
            palette.valueToPaletteMap = this.valueToPaletteMap.clone();
            palette.count = this.count;
            return palette;
        } catch (CloneNotSupportedException e) {
            MinecraftServer.getExceptionManager().handleException(e);
            throw new IllegalStateException("Weird thing happened");
        }
    }

    private void retrieveAll(@NotNull EntryConsumer consumer, boolean consumeEmpty) {
        if (!consumeEmpty && this.count == 0) {
            return;
        }
        long[] values = this.values;
        int dimension = this.dimension();
        int bitsPerEntry = this.bitsPerEntry;
        int magicMask = (1 << bitsPerEntry) - 1;
        int valuesPerLong = 64 / bitsPerEntry;
        int size = maxSize();
        int dimensionMinus = dimension - 1;
        int[] ids = hasPalette() ? this.paletteToValueList.elements() : null;
        int dimensionBitCount = MathUtil.bitsToRepresent(dimensionMinus);
        int shiftedDimensionBitCount = dimensionBitCount << 1;
        for (int i = 0; i < values.length; i++) {
            long value = values[i];
            int startIndex = i * valuesPerLong;
            int endIndex = Math.min(startIndex + valuesPerLong, size);
            for (int index = startIndex; index < endIndex; index++) {
                int bitIndex = (index - startIndex) * bitsPerEntry;
                int paletteIndex = (int) (value >> bitIndex & magicMask);
                if (consumeEmpty || paletteIndex != 0) {
                    int y = index >> shiftedDimensionBitCount;
                    int z = index >> dimensionBitCount & dimensionMinus;
                    int x = index & dimensionMinus;
                    int result = ids != null && paletteIndex < ids.length ? ids[paletteIndex] : paletteIndex;
                    consumer.accept(x, y, z, result);
                }
            }
        }
    }

    private void updateAll(int[] paletteValues) {
        int size = maxSize();
        assert paletteValues.length >= size;
        int bitsPerEntry = this.bitsPerEntry;
        int valuesPerLong = 64 / bitsPerEntry;
        long clear = (1L << bitsPerEntry) - 1L;
        long[] values = this.values;
        for (int i = 0; i < values.length; i++) {
            long block = values[i];
            int startIndex = i * valuesPerLong;
            int endIndex = Math.min(startIndex + valuesPerLong, size);
            for (int index = startIndex; index < endIndex; index++) {
                int bitIndex = (index - startIndex) * bitsPerEntry;
                block = block & ~(clear << bitIndex) | ((long) paletteValues[index] << bitIndex);
            }
            values[i] = block;
        }
    }

    void resize(byte newBitsPerEntry) {
        newBitsPerEntry = newBitsPerEntry > maxBitsPerEntry() ? 15 : newBitsPerEntry;
        FlexiblePalette palette = new FlexiblePalette(this.adaptivePalette, newBitsPerEntry);
        palette.paletteToValueList = this.paletteToValueList;
        palette.valueToPaletteMap = this.valueToPaletteMap;
        getAll(palette::set);
        this.bitsPerEntry = palette.bitsPerEntry;
        this.values = palette.values;
        assert this.count == palette.count;
    }

    private int getPaletteIndex(int value) {
        if (!hasPalette()) {
            return value;
        }
        int lastPaletteIndex = this.paletteToValueList.size();
        byte bpe = this.bitsPerEntry;
        if (lastPaletteIndex >= maxPaletteSize(bpe)) {
            // Palette is full, must resize
            resize((byte) (bpe + 1));
            return getPaletteIndex(value);
        }
        int lookup = this.valueToPaletteMap.putIfAbsent(value, lastPaletteIndex);
        if (lookup != -1) {
            return lookup;
        }
        this.paletteToValueList.add(value);
        assert lastPaletteIndex < maxPaletteSize(bpe);
        return lastPaletteIndex;
    }

    boolean hasPalette() {
        return this.bitsPerEntry <= maxBitsPerEntry();
    }

    static int getSectionIndex(int dimension, int x, int y, int z) {
        int dimensionMask = dimension - 1;
        int dimensionBitCount = MathUtil.bitsToRepresent(dimensionMask);
        return (y & dimensionMask) << (dimensionBitCount << 1) |
                (z & dimensionMask) << dimensionBitCount |
                (x & dimensionMask);
    }

    static int maxPaletteSize(int bitsPerEntry) {
        return 1 << bitsPerEntry;
    }
}