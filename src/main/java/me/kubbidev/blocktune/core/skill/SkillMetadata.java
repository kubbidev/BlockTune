package me.kubbidev.blocktune.core.skill;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.damage.AttackMetadata;
import me.kubbidev.blocktune.core.entity.EntityMetadata;
import me.kubbidev.blocktune.core.entity.EntityMetadataProvider;
import me.kubbidev.blocktune.core.util.EntityBody;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record SkillMetadata(Skill cast, EntityMetadata caster, Location source,
                            @Nullable Entity targetEntity,
                            @Nullable Location targetLocation, @Nullable AttackMetadata attackSource) {

    @NotNull
    public BlockTune plugin() {
        return this.caster.plugin();
    }

    @NotNull
    public LivingEntity entity() {
        return this.caster.entity();
    }

    @Override
    @NotNull
    public Location source() {
        return this.source.clone();
    }

    public boolean hasAttackSource() {
        return this.attackSource != null;
    }

    /**
     * @return The attack which triggered the skill.
     */
    @Override
    public AttackMetadata attackSource() {
        return Objects.requireNonNull(this.attackSource, "Skill was not triggered by any attack");
    }

    /**
     * Retrieves a specific skill parameter value.
     * <p>
     * This applies to the original skill being cast.
     *
     * @param parameter Skill parameter name
     * @return Skill parameter final value, taking into account skill mods
     */
    public double parameter(String parameter) {
        return EntityMetadataProvider.retrieveModifier(entity()).calculateValue(this.cast, parameter);
    }

    @Override
    @NotNull
    public Entity targetEntity() {
        return Objects.requireNonNull(this.targetEntity, "Skill has no target entity");
    }

    @Nullable
    public Entity targetEntityOrNull() {
        return this.targetEntity;
    }

    public boolean hasTargetEntity() {
        return this.targetEntity != null;
    }

    @Override
    @NotNull
    public Location targetLocation() {
        return Objects.requireNonNull(this.targetLocation, "Skill has no target location").clone();
    }

    @Nullable
    public Location targetLocationOrNull() {
        return this.targetLocation == null ? null : this.targetLocation.clone();
    }

    public boolean hasTargetLocation() {
        return this.targetLocation != null;
    }

    /**
     * Analog of {@link #skillEntity(boolean)}.
     * <p>
     * Used when a skill requires a location when no target is provided.
     *
     * @param sourceLocation If the source location should be prioritized.
     * @return Target location (and if it exists) OR location of target entity (and if it exists), source location otherwise
     */
    public Location skillLocation(boolean sourceLocation) {
        return sourceLocation ? this.source.clone() : this.targetLocation != null ? targetLocation() : this.targetEntity != null ? EntityBody.BODY.getLocation(this.targetEntity) : this.source.clone();
    }

    /**
     * Analog of {@link #skillLocation(boolean)}.
     * <p>
     * Used when a skill requires an entity when no target is provided.
     *
     * @param caster If the skill caster should be prioritized.
     * @return Target entity if prioritized (and if it exists), skill caster otherwise
     */
    public Entity skillEntity(boolean caster) {
        return caster || this.targetEntity == null ? this.caster.entity() : this.targetEntity;
    }

    /**
     * Keeps the same skill caster.
     * <p>
     * Used when casting sub-skills with different targets.
     * <p>
     * This has the effect of keeping every skill data, put aside targets.
     * <p>
     * Data that is kept on cloning:
     * <br>- skill being cast
     * <br>- skill caster
     * <br>- attack source
     * <p>
     * Data being replaced on cloning:
     * <br>- source location
     * <br>- target entity
     * <br>- target location
     *
     * @return New skill metadata for other sub-skills
     */
    public SkillMetadata clone(Location source, @Nullable Entity targetEntity, @Nullable Location targetLocation) {
        return new SkillMetadata(this.cast, this.caster, source, targetEntity, targetLocation, this.attackSource);
    }

    public SkillMetadata clone(Location targetLocation) {
        return clone(this.source, this.targetEntity, targetLocation);
    }
}