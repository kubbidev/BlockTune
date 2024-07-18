package me.kubbidev.blocktune.server.instance.palette;

import org.jetbrains.annotations.NotNull;

import java.util.function.IntUnaryOperator;

/**
 * Palette that switches between its backend based on the use case.
 */
final class AdaptivePalette implements Palette, Cloneable {
    final byte dimension;
    final byte defaultBitsPerEntry;
    final byte maxBitsPerEntry;
    SpecializedPalette palette;

    AdaptivePalette(byte dimension, byte maxBitsPerEntry, byte bitsPerEntry) {
        validateDimension(dimension);
        this.dimension = dimension;
        this.maxBitsPerEntry = maxBitsPerEntry;
        this.defaultBitsPerEntry = bitsPerEntry;
        this.palette = new FilledPalette(dimension, 0);
    }

    @Override
    public int get(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0) {
            throw new IllegalArgumentException("Coordinates must be positive");
        }
        return this.palette.get(x, y, z);
    }

    @Override
    public void getAll(@NotNull EntryConsumer consumer) {
        this.palette.getAll(consumer);
    }

    @Override
    public void getAllPresent(@NotNull EntryConsumer consumer) {
        this.palette.getAllPresent(consumer);
    }

    @Override
    public void set(int x, int y, int z, int value) {
        if (x < 0 || y < 0 || z < 0) {
            throw new IllegalArgumentException("Coordinates must be positive");
        }
        flexiblePalette().set(x, y, z, value);
    }

    @Override
    public void fill(int value) {
        this.palette = new FilledPalette(this.dimension, value);
    }

    @Override
    public void setAll(@NotNull EntrySupplier supplier) {
        SpecializedPalette newPalette = new FlexiblePalette(this);
        newPalette.setAll(supplier);
        this.palette = newPalette;
    }

    @Override
    public void replace(int x, int y, int z, @NotNull IntUnaryOperator operator) {
        if (x < 0 || y < 0 || z < 0) {
            throw new IllegalArgumentException("Coordinates must be positive");
        }
        flexiblePalette().replace(x, y, z, operator);
    }

    @Override
    public void replaceAll(@NotNull EntryFunction function) {
        flexiblePalette().replaceAll(function);
    }

    @Override
    public int count() {
        return this.palette.count();
    }

    @Override
    public int bitsPerEntry() {
        return this.palette.bitsPerEntry();
    }

    @Override
    public int maxBitsPerEntry() {
        return this.maxBitsPerEntry;
    }

    @Override
    public int dimension() {
        return this.dimension;
    }

    @Override
    public @NotNull Palette clone() {
        try {
            AdaptivePalette adaptivePalette = (AdaptivePalette) super.clone();
            adaptivePalette.palette = this.palette.clone();
            return adaptivePalette;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }


    Palette flexiblePalette() {
        SpecializedPalette currentPalette = this.palette;
        if (currentPalette instanceof FilledPalette filledPalette) {
            currentPalette = new FlexiblePalette(this);
            currentPalette.fill(filledPalette.value());
            this.palette = currentPalette;
        }
        return currentPalette;
    }

    private static void validateDimension(int dimension) {
        if (dimension <= 1 || (dimension & dimension - 1) != 0)
            throw new IllegalArgumentException("Dimension must be a positive power of 2");
    }
}