package me.kubbidev.blocktune.core.util;

import com.google.common.base.Preconditions;
import me.kubbidev.blocktune.core.entity.modfier.EntityModifier;
import me.kubbidev.blocktune.core.entity.modfier.ModifierSource;

/**
 * Used to make a difference between stat modifiers granted by off hand and main hand items.
 */
@SuppressWarnings("EnhancedSwitchMigration")
public enum EquipmentSlot {

    /**
     * When placed in any armor slot.
     * <p>
     * No distinction between helmet, chest plate, leggings and boots unlike vanilla
     * Minecraft since you can't place a chest plate item inside of the feet slot for instance.
     */
    ARMOR,

    /**
     * When placed in an accessory slot.
     */
    ACCESSORY,

    /**
     * When placed in main hand.
     */
    MAIN_HAND,

    /**
     * When placed in off hand.
     */
    OFF_HAND,

    /**
     * Fictive equipment slot which overrides all
     * rules and apply the item stats whatsoever.
     */
    OTHER;

    public org.bukkit.inventory.EquipmentSlot toBukkit() {
        switch (this) {
            case MAIN_HAND:
                return org.bukkit.inventory.EquipmentSlot.HAND;
            case OFF_HAND:
                return org.bukkit.inventory.EquipmentSlot.OFF_HAND;
            default:
                throw new RuntimeException("Not a hand slot");
        }
    }

    public static EquipmentSlot fromBukkit(org.bukkit.inventory.EquipmentSlot slot) {
        switch (slot) {
            // Hand items
            case HAND:
                return MAIN_HAND;
            case OFF_HAND:
                return OFF_HAND;

            // Others
            case FEET:
            case HEAD:
            case LEGS:
            case CHEST:
            case BODY:
                return ARMOR;
            default:
                return OTHER;
        }
    }

    private EquipmentSlot getOppositeHand() {
        Preconditions.checkArgument(this == MAIN_HAND || this == OFF_HAND, "Not a hand equipment slot");
        return this == MAIN_HAND ? OFF_HAND : MAIN_HAND;
    }

    /**
     * Basic modifier application rule.
     *
     * @param modifier The entity modifier
     * @return If a modifier should be taken into account given the action hand
     */
    public boolean isCompatible(EntityModifier modifier) {
        return isCompatible(modifier.getSource(), modifier.getSlot());
    }

    /**
     * Every action has a {@link org.bukkit.inventory.EquipmentSlot Hand} associated to it, called the action hand.
     * <p>
     * It corresponds to the hand the entity is using to perform an action.
     * By default, BlockTune uses the {@link org.bukkit.inventory.EquipmentSlot#HAND} if none is specified.
     * The action hand is the enum value calling this method.
     * <p>
     * Modifiers from both hands are registered in modifier maps YET filtered out when
     * calculating stat values/filtering out abilities/...
     *
     * <br>Modifiers from the other hand are ignored IF AND ONLY IF the other hand item is a weapon.
     * <br>As long as the item placement is valid, non-weapon items all apply their modifiers.
     * <p>
     * Filtering out the right entity modifiers is referred as "isolating modifiers"
     *
     * @param modifierSource The source of modifier
     * @param equipmentSlot  The equipment slot of the modifier
     * @return True if a modifier with the given equipment slot and modifier source should
     * be taken into account given by the action hand
     */
    public boolean isCompatible(ModifierSource modifierSource, EquipmentSlot equipmentSlot) {
        Preconditions.checkArgument(isHand(), "Instance called must be a hand equipment slot");

        if (equipmentSlot == OTHER) {
            return true;
        }

        switch (modifierSource) {

            // Simple rules
            case VOID:
                return false;
            case OTHER:
                return true;

            // Ignore modifiers from opposite hand if it's a weapon
            case RANGED_WEAPON:
            case MELEE_WEAPON:
                return equipmentSlot == this;

            // Hand items
            case OFFHAND_ITEM:
                return equipmentSlot == OFF_HAND;
            case MAINHAND_ITEM:
                return equipmentSlot == MAIN_HAND;
            case HAND_ITEM:
                return equipmentSlot.isHand();

            // Accessories & armor
            case ARMOR:
                return equipmentSlot == ARMOR;
            case ACCESSORY:
                return equipmentSlot == ACCESSORY;

            default:
                throw new IllegalArgumentException();
        }
    }

    public boolean isHand() {
        return this == MAIN_HAND || this == OFF_HAND;
    }
}