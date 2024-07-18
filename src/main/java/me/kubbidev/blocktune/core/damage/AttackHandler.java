package me.kubbidev.blocktune.core.damage;

import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

public interface AttackHandler {

    /**
     * @param event Damage event corresponding to the attack.
     *              <p>
     *              Some plugins don't store the damager so the only way to retrieve it is through
     *              the damage event.
     * @return Information about the attack (the potential player damage source, damage types,
     * and attack damage value).
     */
    @Nullable
    AttackMetadata getAttack(EntityDamageEvent event);
}