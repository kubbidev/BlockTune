package me.kubbidev.blocktune.core.entity.modfier;

import me.kubbidev.blocktune.core.util.EquipmentSlot;

/**
 * Main problem solved by the {@link ModifierSource} is being able to
 * calculate specific statistics while ignoring other modifiers.
 * <p>
 * When calculating the entity's when using a main
 * hand weapon, we must completely ignore attack damage given by off-hand modifiers.
 *
 * @see EquipmentSlot#isCompatible(EntityModifier)
 */
public enum ModifierSource {

    /**
     * Modifier given by a melee weapon.
     * <p>
     * These modifiers should only be taken into account when the
     * entity wears the item in the main hand.
     */
    MELEE_WEAPON,

    /**
     * Modifier given by a ranged weapon.
     * <p>
     * These modifiers should only be taken into account when the
     * entity wears the item in the main hand.
     */
    RANGED_WEAPON,

    /**
     * Modifier given by an offhand item.
     * <p>
     * These modifiers should only be taken into account when the
     * entity wears the item in the offhand.
     */
    OFFHAND_ITEM,

    /**
     * Modifier given by a mainhand item.
     * <p>
     * These modifiers should only be taken into account when the
     * entity wears the item in the mainhand.
     */
    MAINHAND_ITEM,

    /**
     * Modifier given by a hand item.
     * <p>
     * These modifiers should only be taken into account when the
     * entity holds the item in one of their hands.
     */
    HAND_ITEM,

    /**
     * Modifier given by an armor item.
     * <p>
     * Modifiers are applied if worn in an armor slot.
     */
    ARMOR,

    /**
     * Modifier given by an accessory.
     * <p>
     * Modifiers are applied if worn in an accessory slot.
     */
    ACCESSORY,

    /**
     * Modifier given by anything else (Modifiers always apply).
     * <p>
     * Has a lower priority compared to {@link EquipmentSlot#OTHER}
     */
    OTHER,

    /**
     * Modifiers never apply whatsoever.
     * <p>
     * Has a lower priority compared to {@link EquipmentSlot#OTHER}
     */
    VOID;

    /**
     * @return True if the source of the modifier is a weapon, otherwise false.
     */
    public boolean isWeapon() {
        return this == MELEE_WEAPON || this == RANGED_WEAPON;
    }

    /**
     * @return True if the source of the modifier is an item that must be handheld (accessories & weapons).
     */
    public boolean isHandheld() {
        return isEquipment() && this != ARMOR && this != ACCESSORY;
    }

    /**
     * @return True if the source of the modifier is a piece of equipment (either handheld or armor).
     */
    public boolean isEquipment() {
        return this != VOID && this != OTHER;
    }
}