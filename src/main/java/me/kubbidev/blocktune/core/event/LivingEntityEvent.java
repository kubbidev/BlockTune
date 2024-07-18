package me.kubbidev.blocktune.core.event;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an LivingEntity-related event
 */
public abstract class LivingEntityEvent extends Event {
    protected final LivingEntity entity;

    public LivingEntityEvent(@NotNull LivingEntity entity) {
        this.entity = entity;
    }

    /**
     * Returns the LivingEntity involved in this event.
     *
     * @return LivingEntity who is involved in this event
     */
    @NotNull
    public LivingEntity getEntity() {
        return this.entity;
    }

    /**
     * Gets the EntityType of the LivingEntity involved in this event.
     *
     * @return EntityType of the LivingEntity involved in this event
     */
    @NotNull
    public EntityType getEntityType() {
        return this.entity.getType();
    }
}
