package me.kubbidev.blocktune.core.event.attack;

import lombok.Getter;
import me.kubbidev.blocktune.core.damage.AttackMetadata;
import me.kubbidev.blocktune.core.event.LivingEntityEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EntityKillEntityEvent extends LivingEntityEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final EntityDamageEvent event;

    @Getter
    private final LivingEntity target;

    @Getter
    private final AttackMetadata attack;

    public EntityKillEntityEvent(@NotNull EntityDamageEvent event, @NotNull AttackMetadata attack, @NotNull LivingEntity target) {
        super(Objects.requireNonNull(attack.getAttacker(), "attacker").entity());
        this.event = event;
        this.attack = attack;
        this.target = target;
    }

    public EntityDamageEvent toBukkit() {
        return this.event;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}