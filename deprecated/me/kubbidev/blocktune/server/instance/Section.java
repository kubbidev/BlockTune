package me.kubbidev.blocktune.server.instance;

import me.kubbidev.blocktune.server.instance.palette.Palette;
import org.jetbrains.annotations.NotNull;

public final class Section implements Cloneable {
    private final Palette blockPalette;

    private Section(Palette blockPalette) {
        this.blockPalette = blockPalette;
    }

    public Section() {
        this(Palette.blocks());
    }

    public Palette blockPalette() {
        return this.blockPalette;
    }

    public void clear() {
        this.blockPalette.fill(0);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public @NotNull Section clone() {
        return new Section(this.blockPalette.clone());
    }
}