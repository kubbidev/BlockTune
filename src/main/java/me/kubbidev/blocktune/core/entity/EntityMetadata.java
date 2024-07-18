package me.kubbidev.blocktune.core.entity;

import com.google.common.base.Preconditions;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.damage.AttackMetadata;
import me.kubbidev.blocktune.core.damage.DamageMetadata;
import me.kubbidev.blocktune.core.damage.DamageType;
import me.kubbidev.blocktune.core.damage.Element;
import me.kubbidev.blocktune.core.util.EquipmentSlot;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public record EntityMetadata(BlockTune plugin, LivingEntity entity, EquipmentSlot actionHand) {
    public EntityMetadata {
        Preconditions.checkArgument(actionHand.isHand(), "Equipment slot must be a hand");
    }

    /**
     * Utility method that makes an entity deal damage to another specific entity.
     * <p>
     * This creates the attackMetadata based on the data stored by the CasterMetadata,
     * and calls it using damage manager.
     *
     * @param target The target entity.
     * @param damage The damage dealt.
     * @param types  The type of damage inflicted.
     * @return The (modified) attack metadata
     */
    public AttackMetadata attack(LivingEntity target, double damage, DamageType... types) {
        return attack(target, damage, null, types);
    }

    /**
     * Utility method that makes an entity deal damage to another specific entity.
     * <p>
     * This creates the attackMetadata based on the data stored by the CasterMetadata,
     * and calls it using damage manager.
     *
     * @param target  The target entity.
     * @param damage  The damage dealt.
     * @param element The damage element applied.
     * @param types   The type of damage inflicted.
     * @return The (modified) attack metadata
     */
    public AttackMetadata attack(LivingEntity target, double damage, @Nullable Element element, DamageType... types) {
        return attack(target, damage, true, element, types);
    }

    /**
     * Utility method that makes an entity deal damage to another specific entity.
     * <p>
     * This creates the attackMetadata based on the data stored by the CasterMetadata,
     * and calls it using damage manager.
     *
     * @param target    The target entity.
     * @param damage    The damage dealt.
     * @param element   The damage element applied.
     * @param knockback If should apply knockback.
     * @param types     The type of damage inflicted.
     * @return The (modified) attack metadata
     */
    public AttackMetadata attack(LivingEntity target, double damage, boolean knockback, @Nullable Element element, DamageType... types) {
        @Nullable AttackMetadata registeredAttack = this.plugin.getDamageManager().getRegisteredAttackMetadata(target);
        if (registeredAttack != null) {
            registeredAttack.getMetadata().add(damage, element, types);
            return registeredAttack;
        }

        DamageMetadata damageMetadata = new DamageMetadata(damage, element, types);
        AttackMetadata attackMetadata = new AttackMetadata(damageMetadata, target, this);

        this.plugin.getDamageManager().registerAttack(attackMetadata, knockback, false);
        return attackMetadata;
    }
}