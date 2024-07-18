package me.kubbidev.blocktune.core.interaction;

import me.kubbidev.blocktune.core.interaction.relation.Relationship;
import org.jetbrains.annotations.NotNull;

public interface InteractionRules {

    /**
     * Gets if in general, support skills should also take {@link org.bukkit.entity.Mob}
     * and {@link org.bukkit.entity.Creature} in count when applied.
     *
     * @return true if supported, otherwise false
     */
    boolean isSupportSkillsOnMobs();

    /**
     * Gets whether the specified {@link InteractionType} is enabled taking account different parameters.
     *
     * @param interaction  The type of entity interaction
     * @param relationship The relationship between the entities
     * @param pvp          If the PvP is enabled at a specific location
     * @return true this specific interaction is enabled, otherwise false
     */
    boolean isEnabled(@NotNull InteractionType interaction, @NotNull Relationship relationship, boolean pvp);
}