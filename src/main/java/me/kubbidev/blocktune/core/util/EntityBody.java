package me.kubbidev.blocktune.core.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public enum EntityBody {

    /**
     * By the entity's feet
     */
    FEET(0),

    /**
     * At the middle of the body
     */
    BODY(.5),

    /**
     * At the top of the entity's head
     */
    TOP(1),

    /**
     * The position of the eyes
     */
    EYES(1.6 / 1.8);

    private final double heightPercentage;

    EntityBody(double heightPercentage) {
        this.heightPercentage = heightPercentage;
    }

    public Location getLocation(Entity entity) {
        Location location = entity.getLocation();
        location.add(0, entity.getHeight() * this.heightPercentage, 0);
        return location;
    }
}