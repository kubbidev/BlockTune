package me.kubbidev.blocktune;

import me.kubbidev.blocktune.spell.SpellMetadataProvider;
import me.kubbidev.spellcaster.SpellCasterProvider;
import me.kubbidev.spellcaster.damage.AttackMetadata;
import me.kubbidev.spellcaster.damage.DamageMetadata;
import me.kubbidev.spellcaster.damage.DamageType;
import me.kubbidev.spellcaster.damage.Element;
import me.kubbidev.spellcaster.spell.SpellMetadata;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.*;

import static me.kubbidev.spellcaster.InternalMethod.*;

/**
 * A utility class providing various static methods.
 */
public final class UtilityMethod {
    private UtilityMethod() {
    }

    /**
     * The random number generator used by the utility methods.
     */
    private static final Random random = new Random();

    /**
     * Gets the amplifier level of a specific {@link PotionEffect} applied to a living entity.
     *
     * @param entity The entity from which to retrieve the potion effect.
     * @param type   The type of potion effect to check for.
     * @return The amplifier level of the potion effect plus one, or zero if the effect is not present.
     */
    public static int getPotionAmplifier(LivingEntity entity, PotionEffectType type) {
        PotionEffect potionEffect = entity.getPotionEffect(type);
        return potionEffect != null ? potionEffect.getAmplifier() + 1 : 0;
    }

    /**
     * Gets the remaining duration of a specific {@link PotionEffect} applied to a living entity.
     *
     * @param entity The entity from which to retrieve the potion effect.
     * @param type   The type of potion effect to check for.
     * @return The remaining duration of the potion effect in ticks, or zero if the effect is not present.
     */
    public static int getPotionDuration(LivingEntity entity, PotionEffectType type) {
        PotionEffect potionEffect = entity.getPotionEffect(type);
        return potionEffect != null ? potionEffect.getDuration() : 0;
    }

    /**
     * Clamps a normalized color value (0.0 to 1.0) to the integer range of 0 to 255.
     *
     * @param color the normalized color value to be clamped, should be between 0.0 and 1.0
     * @return the clamped color value as an integer between 0 and 255
     */
    public static int clampColorToRange(@Range(from = 0, to = 255) double color) {
        return Math.min(255, Math.max((int) (color * 255), 0));
    }

    /**
     * Performs a fast ray trace from the given origin in the specified direction.
     *
     * @param origin    The starting location of the ray trace.
     * @param direction The direction vector for the ray trace.
     * @param multiply  The distance the ray will travel.
     * @return The location where the ray trace ends, either at a hit block or the endpoint of the ray.
     */
    public static Location fastRayTrace(Location origin, Vector direction, double multiply) {
        return origin.clone().add(direction.multiply(multiply));
    }

    public static void attack(SpellMetadata meta, Location location, double damage, double radius, double knockback, double repulsion, boolean shouldSwing, @Nullable Element element, DamageType... types) {
        LivingEntity caster = meta.entity();
        double d;
        double k;
        boolean isSwinging = false;
        boolean isBlocking;

        // scale damage on caster strength effect amplifier
        damage *= 1.0 + ((double) getPotionAmplifier(caster, PotionEffectType.STRENGTH) / 3);

        for (Entity victim : getNearbyChunkEntities(location).stream()
                .filter(e -> !e.equals(caster))
                .filter(e -> e.getLocation().distanceSquared(location) <= (radius * radius))
                .toList()) {

            if (canTarget(SpellCasterProvider.get(), caster, victim)) {
                isBlocking = false;

                LivingEntity target = (LivingEntity) victim;
                d = damage;
                k = knockback;

                // return entity loop here if it cannot be damaged yet
                if (target.getNoDamageTicks() > (target.getMaximumNoDamageTicks() / 2.0)) {
                    continue;
                }

                boolean isCasterCasting = SpellMetadataProvider.isCasting(caster);
                boolean isVictimCasting = SpellMetadataProvider.isCasting(target);

                if (isVictimCasting && isCasterCasting && damage > 0.0) {
                    isBlocking = true;
                    d *= 0.5;
                    k *= 0.5;
                }

                boolean isKnockback = k != 0.0;

                AttackMetadata attackMetadata = meta.caster().attack(target, d, isKnockback, element, types);
                DamageMetadata damageMetadata = attackMetadata.getMetadata();

                if (damageMetadata.getDamage() > DamageMetadata.MINIMAL_DAMAGE) {
                    isSwinging = true;

                    if (isBlocking) {
                        intelligentHandsSwing(target);
                        location.getWorld().playSound(target, "minecraft:custom.generic.sword_guard", 0.2f, 1.0f);

                        Location displayLoc = target.getEyeLocation();
                        displayLoc.getWorld().spawnParticle(Particle.FLASH, displayLoc, 0);
                        displayLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, displayLoc, 64,
                                0.5,
                                0.5,
                                0.5, 0.5);
                    }
                    if (isKnockback) {
                        double[] velocity = getRepulsionVelocity(caster, target);
                        double x = velocity[0] * k;
                        double y = velocity[1] * k + 0.1;
                        double z = velocity[2] * k;
                        target.setVelocity(new Vector(x, Math.min(y, 2.0), z));
                    }
                }
            }
            if (victim instanceof Projectile) {
                isSwinging = true;

                if (repulsion == 1.0) {
                    victim.remove();
                }
                if (repulsion == 2.0) {
                    double[] velocity = getRepulsionVelocity(caster, victim);
                    double x = velocity[0] / 2.0;
                    double y = velocity[1] / 2.0;
                    double z = velocity[2] / 2.0;
                    victim.setVelocity(new Vector(x, y, z));
                }
            }
        }
        if (isSwinging && shouldSwing) {
            intelligentHandsSwing(caster);
        }
    }

    private static void intelligentHandsSwing(LivingEntity entity) {
        entity.swingMainHand();
        if (entity.getEquipment() != null && isWeapon(entity.getEquipment().getItemInOffHand())) {
            entity.swingOffHand();
        }
    }

    private static double[] getRepulsionVelocity(Entity entity, Entity target) {
        double x = target.getX() - entity.getX();
        double y = target.getY() - entity.getY();
        double z = target.getZ() - entity.getZ();
        // calculate the distance between the entity and the target
        double disManhattan = Math.abs(x) + Math.abs(y) + Math.abs(z);
        if (disManhattan == 0.0) {
            x = target.getVelocity().getX();
            y = target.getVelocity().getY();
            z = target.getVelocity().getZ();
        } else {
            x = x / disManhattan * 3.0;
            y = y / disManhattan * 3.0;
            z = z / disManhattan * 3.0;
        }
        // return the resulting knockback velocity vector
        return new double[]{x, y, z};
    }

    /**
     * Calculates the forward velocity for a given entity.
     * For {@link Mob}, the forward velocity is directed towards their target.
     * For other living entities, it is based on their looking direction.
     *
     * @param entity   The entity for which to calculate the forward velocity.
     * @param usePitch Whether or not the pitch direction for an entity should
     *                 be take in consideration when ray casting.
     * @return A Vector representing the forward velocity.
     */
    public static Vector getForwardVelocity(LivingEntity entity, boolean usePitch) {
        double x = 0;
        double y = 0;
        double z = 0;

        // if the entity is a mob, calculate the velocity towards its target
        if (entity instanceof Mob) {
            @Nullable LivingEntity target = ((Mob) entity).getTarget();
            if (target != null) {
                x = target.getX() - entity.getX();
                y = target.getY() - entity.getY();
                z = target.getZ() - entity.getZ();
            }
        } else {
            Location eyeLocation = entity.getEyeLocation();
            // block the pitch at 0 to avoid the hit point location
            // being above or under the entity y when ray casting
            if (!usePitch) {
                eyeLocation.setPitch(0.f);
            }

            // cache the caster eye location direction for performance
            Vector direction = eyeLocation.getDirection();

            // for other entities, use ray tracing to calculate the direction
            Location loc1 = fastRayTrace(eyeLocation, direction, 10.0);
            Location loc2 = fastRayTrace(eyeLocation, direction, 0.0);
            x = loc1.getX() - loc2.getX();
            y = loc1.getY() - loc2.getY();
            z = loc1.getZ() - loc2.getZ();
        }
        double disManhattan = Math.abs(x) + Math.abs(y) + Math.abs(z);
        if (disManhattan == 0.0) {
            x = 0;
            y = 0;
            z = 0;
        } else {
            x = x / disManhattan * 3.0;
            y = y / disManhattan * 3.0;
            z = z / disManhattan * 3.0;
        }

        if (!isEntityNearGround(entity)) {
            y = Math.min(y, 0.0);
        }

        // adjust the velocity based on the entity's strength potion effect
        double s = 0.75 + (double) Math.min(getPotionAmplifier(entity, PotionEffectType.STRENGTH), 9) / 40;
        x *= s;
        y *= s;
        z *= s;
        return new Vector(x, y, z);
    }

    public static boolean isEntityNearGround(LivingEntity entity) {
        Location loc = entity.getLocation();
        // if the entity is airborne and not near solid blocks,
        // ensure it does not gain upward velocity
        return entity.isOnGround() ||
                loc.getWorld().getBlockAt(loc.getBlockX() + 1, loc.getBlockY(), loc.getBlockZ()).isSolid() ||
                loc.getWorld().getBlockAt(loc.getBlockX() - 1, loc.getBlockY(), loc.getBlockZ()).isSolid() ||
                loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ() + 1).isSolid() ||
                loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ() - 1).isSolid();
    }
}
