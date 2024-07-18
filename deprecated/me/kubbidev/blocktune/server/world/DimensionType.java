package me.kubbidev.blocktune.server.world;

import org.bukkit.World;

public record DimensionType(int minY, int maxY) {
    public static DimensionType of(World world) {
        return new DimensionType(
                world.getMinHeight(),
                world.getMaxHeight()
        );
    }

    public int height() {
        return this.maxY - this.minY;
    }

    @Override
    public String toString() {
        return this.minY + ":" + this.maxY;
    }
}
