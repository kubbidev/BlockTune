package me.kubbidev.blocktune.core.listener.indicator;

import com.google.common.util.concurrent.AtomicDouble;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.event.indicator.IndicatorDisplayEvent;
import me.kubbidev.blocktune.core.hologram.Hologram;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class GameIndicator implements Listener {
    /**
     * Global random number generator used throughout the class.
     */
    protected static final Random random = new Random();

    protected final BlockTune plugin;

    public GameIndicator(BlockTune plugin) {
        this.plugin = plugin;
    }

    /**
     * Displays a message using a hologram around an entity
     *
     * @param entity    The entity used to find the hologram initial position
     * @param message   The message to display
     * @param direction The average direction of the hologram indicator
     */
    public void displayIndicator(Entity entity, Component message, Vector direction, IndicatorDisplayEvent.IndicatorType type) {
        IndicatorDisplayEvent called = new IndicatorDisplayEvent(entity, message, type);
        if (!called.callEvent()) return;

        Location location = entity.getLocation().add(
                (random.nextDouble() - 0.5) * 1.2, Y_OFFSET + entity.getHeight() * ENTITY_HEIGHT_PERCENTAGE,
                (random.nextDouble() - 0.5) * 1.2);

        displayIndicator(location, called.getText(), direction);
    }

    private void displayIndicator(Location location, Component message, Vector direction) {
        Hologram hologram = Hologram.create(location, Collections.singletonList(message));
        hologram.spawn();

        AtomicDouble velocity = new AtomicDouble(6 * INITIAL_UPWARD_VELOCITY);
        AtomicInteger counter = new AtomicInteger(0);
        new BukkitRunnable() {

            @Override
            public void run() {
                if (counter.get() == 0) {
                    direction.multiply(2 * RADIAL_VELOCITY);
                }

                if (counter.getAndIncrement() >= HOLOGRAM_LIFE_SPAN) {
                    hologram.despawn();
                    cancel();
                    return;
                }
                velocity.addAndGet(-GRAVITY * ACCELERATION);
                location.add(
                        direction.getX() * ACCELERATION, velocity.get() * ACCELERATION,
                        direction.getZ() * ACCELERATION
                );
                hologram.updateLocation(location);
            }
        }.runTaskTimer(this.plugin, 0, 3);
    }

    // TODO add this into the configuration file
    /**
     * The radial velocity used for hologram movement.
     */
    private static final double RADIAL_VELOCITY = 1;

    /**
     * The gravitational constant, representing the acceleration due to gravity in m/s^2.
     */
    private static final double GRAVITY = 10;

    /**
     * The acceleration factor applied to the hologram movement.
     */
    private static final double ACCELERATION = 0.15;

    /**
     * The initial upward velocity for the hologram.
     */
    private static final double INITIAL_UPWARD_VELOCITY = 1;

    /**
     * The percentage of the entity's height used to calculate the Y offset for the hologram.
     */
    private static final double ENTITY_HEIGHT_PERCENTAGE = 0.75;

    /**
     * The Y offset added to the entity's height to determine the initial hologram position.
     */
    private static final double Y_OFFSET = 0.1;

    /**
     * Hologram life span in ticks.
     */
    private static final int HOLOGRAM_LIFE_SPAN = 7;
}