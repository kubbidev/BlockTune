package me.kubbidev.blocktune.core.damage;

public enum DamageType {
    /**
     * Magic damage dealt by magic weapons or abilities
     */
    MAGIC,
    /**
     * Physical damage dealt by melee attacks or skills
     */
    PHYSICAL,
    /**
     * Damage dealt by any type of weapon
     */
    WEAPON,
    /**
     * Damage dealt by skills or abilities
     */
    SKILL,
    /**
     * Projectile based weapons or skills
     */
    PROJECTILE,
    /**
     * Hitting an enemy with bare hands
     */
    UNARMED,
    /**
     * Damage over time
     */
    DOT
}