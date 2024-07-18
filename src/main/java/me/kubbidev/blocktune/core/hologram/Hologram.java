package me.kubbidev.blocktune.core.hologram;

import me.kubbidev.blocktune.core.hologram.factory.BukkitHologramFactory;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * A simple base hologram.
 */
public interface Hologram {

    /**
     * Spawns the hologram
     */
    void spawn();

    /**
     * Despawns the hologram
     */
    void despawn();

    /**
     * Check if the hologram is currently spawned
     *
     * @return true if spawned and active, or false otherwise
     */
    boolean isSpawned();

    /**
     * Gets the ArmorStands that hold the lines for this hologram
     *
     * @return the ArmorStands holding the lines
     */
    Collection<ArmorStand> getArmorStands();

    /**
     * Gets the ArmorStand holding the specified line
     *
     * @param line the line
     * @return the ArmorStand holding this line
     */
    @Nullable
    ArmorStand getArmorStand(int line);

    /**
     * Updates the location of the hologram or spawn it if not present
     *
     * @param location the new location
     */
    void updateLocation(Location location);

    /**
     * Updates the lines displayed by this hologram
     *
     * <p>This method does not refresh the actual hologram display. {@link #spawn()} must be called for these changes
     * to apply.</p>
     *
     * @param lines the new lines
     */
    void updateLines(List<Component> lines);

    /**
     * Creates a new hologram instance.
     *
     * @param location The location where the hologram will be spawned.
     * @param lines    The lines of text to be displayed by the hologram.
     * @return A new hologram instance.
     */
    static Hologram create(Location location, List<Component> lines) {
        return BukkitHologramFactory.INSTANCE.newHologram(location, lines);
    }
}