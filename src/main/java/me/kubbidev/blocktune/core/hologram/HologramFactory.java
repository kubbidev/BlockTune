package me.kubbidev.blocktune.core.hologram;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;

import java.util.List;

/**
 * A object which can create {@link Hologram}s.
 */
public interface HologramFactory {

    /**
     * Creates a new hologram.
     *
     * @param location the location of the hologram
     * @param lines    the initial lines to display
     * @return the new hologram
     */
    Hologram newHologram(Location location, List<Component> lines);
}