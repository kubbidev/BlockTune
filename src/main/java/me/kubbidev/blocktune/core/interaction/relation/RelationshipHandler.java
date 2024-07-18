package me.kubbidev.blocktune.core.interaction.relation;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public interface RelationshipHandler {

    @NotNull
    Relationship getRelationship(@NotNull LivingEntity source, @NotNull LivingEntity target);
}