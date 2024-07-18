package me.kubbidev.blocktune.core.event.attack;

import lombok.Getter;
import me.kubbidev.blocktune.core.damage.AttackMetadata;
import me.kubbidev.blocktune.core.damage.DamageMetadata;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.jetbrains.annotations.NotNull;

/**
 * This is a wrapper for bukkit damage events used to provide more information on the current attack:
 * <br>- the entity attacking (maybe null)
 * <br>- full info on the damage
 */
public class AttackEvent extends EntityEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final EntityDamageEvent event;

    @Getter
    private final AttackMetadata attack;

    /**
     * Called whenever an entity deals damage to another entity.
     *
     * @param event  The corresponding damage event.
     * @param attack The generated attack result which can be edited.
     */
    public AttackEvent(EntityDamageEvent event, AttackMetadata attack) {
        super(event.getEntity());

        this.event = event;
        this.attack = attack;
    }

    public EntityDamageEvent toBukkit() {
        return this.event;
    }

    public DamageMetadata getMetadata() {
        return this.attack.getMetadata();
    }

    @Override
    public boolean isCancelled() {
        return this.event.isCancelled();
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.event.setCancelled(cancel);
    }

    @Override
    public @NotNull LivingEntity getEntity() {
        return this.attack.getTarget();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}