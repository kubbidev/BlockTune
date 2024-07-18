package me.kubbidev.blocktune.core.damage;

import lombok.Getter;
import me.kubbidev.blocktune.core.entity.EntityMetadata;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
public class AttackMetadata {

    @NotNull
    private final DamageMetadata metadata;

    @NotNull
    private final LivingEntity target;

    @Nullable
    private final EntityMetadata attacker;

    /**
     * Used by {@link AttackHandler} instances to register attacks.
     * <p>
     * {@link DamageMetadata} only gives information about the attack damage and types while
     * this class also contains info about the damager.
     * <p>
     * Some plugins don't let BlockTune determine what the damager is so there might
     * be problem with damage/reduction stat application.
     *
     * @param metadata   The attack result.
     * @param target   The entity that received the damage.
     * @param attacker The entity who dealt the damage.
     */
    public AttackMetadata(@NotNull DamageMetadata metadata, @NotNull LivingEntity target, @Nullable EntityMetadata attacker) {
        Objects.requireNonNull(target, "Target cannot be null");
        Objects.requireNonNull(metadata, "Metadata cannot be null");
        this.target = target;
        this.metadata = metadata;
        this.attacker = attacker;
    }

    public boolean hasAttacker() {
        return this.attacker != null;
    }
}