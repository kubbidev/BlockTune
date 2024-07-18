package me.kubbidev.blocktune.core.interaction;

import org.bukkit.entity.LivingEntity;

public interface InteractionRestriction {

    /**
     * Called whenever an entity tries to damage OR buff another entity.
     * <p>
     * This should be used by:
     * <br>- plugins which implement friendly fire player sets like parties, guilds, nations, factions....
     * <br>- plugins which implement custom invulnerable entities like NPCs, sentinels....
     *
     * @param source The entity targeting another entity
     * @param target The entity Entity being targeted
     * @param type   The type of interaction, whether it's positive (buff, heal) or negative (offense skill, attack)
     * @return True if the interaction between the two entity is possible, otherwise false (should be cancelled!)
     */
    boolean canTarget(LivingEntity source, LivingEntity target, InteractionType type);
}