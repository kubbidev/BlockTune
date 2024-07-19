package me.kubbidev.blocktune.core;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.damage.AttackMetadata;
import me.kubbidev.blocktune.core.damage.DamageMetadata;
import me.kubbidev.blocktune.core.damage.DamageType;
import me.kubbidev.blocktune.core.damage.Element;
import me.kubbidev.blocktune.core.entity.EntityMetadataProvider;
import me.kubbidev.blocktune.core.interaction.InteractionType;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.nexuspowered.item.ItemStackBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

//    /**
//     * Checks if the source entity can target the target entity at a specific location using the default interaction type.
//     *
//     * @param plugin   The plugin instance.
//     * @param source   The source entity attempting to target.
//     * @param location The location where the target is being checked. Can be null.
//     * @param target   The target entity.
//     * @return true if the source can target the target entity, false otherwise.
//     */
//    public static boolean canTarget(BlockTune plugin, Entity source, @Nullable Location location, Entity target) {
//        return canTarget(plugin, source, location, target, InteractionType.OFFENSE_SKILL);
//    }

    /**
     * Checks if the source entity can target the target entity using the default interaction type.
     *
     * @param plugin The plugin instance.
     * @param source The source entity attempting to target.
     * @param target The target entity.
     * @return true if the source can target the target entity, false otherwise.
     */
    public static boolean canTarget(BlockTune plugin, Entity source, Entity target) {
        return canTarget(plugin, source, target, InteractionType.OFFENSE_SKILL);
    }

    /**
     * Checks if the source entity can target the target entity using a specified interaction type.
     *
     * @param plugin The plugin instance.
     * @param source The source entity attempting to target.
     * @param target The target entity.
     * @param type   The type of interaction.
     * @return true if the source can target the target entity, false otherwise.
     */
    public static boolean canTarget(BlockTune plugin, Entity source, Entity target, InteractionType type) {
        return plugin.getEntityManager().canInteract(source, target, type);
    }

//    /**
//     * The amount by which to expand an entity's bounding box for target checks.
//     */
//    private static final double BOUNDING_BOX_EXPANSION = 0.2;
//
//    /**
//     * Checks if the source entity can target the target entity at a specific location using a specified interaction type.
//     *
//     * @param plugin   The plugin instance.
//     * @param source   The source entity attempting to target.
//     * @param location The location where the target is being checked. Can be null.
//     * @param target   The target entity.
//     * @param type     The type of interaction.
//     * @return true if the source can target the target entity, false otherwise.
//     */
//    public static boolean canTarget(BlockTune plugin, Entity source, @Nullable Location location, Entity target, InteractionType type) {
//        if (location == null || target.getBoundingBox().expand(BOUNDING_BOX_EXPANSION).contains(location.toVector())) {
//            // also use the internal plugin entity interaction manager method.
//            return plugin.getEntityManager().canInteract(source, target, type);
//        }
//        return false;
//    }

    /**
     * Retrieves all entities in the chunks surrounding the given location.
     *
     * @param loc The location used to determine the chunks to search.
     * @return A list of entities found in the surrounding chunks.
     */
    public static List<Entity> getNearbyChunkEntities(Location loc) {
        List<Entity> entities = new ArrayList<>();

        int cx = loc.getChunk().getX();
        int cz = loc.getChunk().getZ();

        for (int x = -1; x < 2; x++)
            for (int z = -1; z < 2; z++)
                entities.addAll(Arrays.asList(loc.getWorld().getChunkAt(cx + x, cz + z).getEntities()));

        return entities;
    }

    /**
     * Checks if an entity is vanished based on its metadata.
     *
     * @param entity The entity to check.
     * @return true if the entity is vanished, false otherwise.
     */
    public static boolean isVanished(Metadatable entity) {
        return entity.getMetadata("vanished").stream().anyMatch(MetadataValue::asBoolean);
    }

    /**
     * Checks if an item stack is air or null.
     *
     * @param item The item stack to check. Can be null.
     * @return true if the item stack is air or null, false otherwise.
     */
    public static boolean isAir(@Nullable ItemStack item) {
        return item == null || item.getType().isAir();
    }

    /**
     * Checks if an item stack is considered a weapon based on its durability
     * (Purely arbitrary but works decently).
     *
     * @param item The item stack to check. Can be null.
     * @return true if the item stack is a weapon, false otherwise.
     */
    public static boolean isWeapon(@Nullable ItemStack item) {
        return item != null && item.getType().getMaxDurability() > 0;
    }

    /**
     * Heals a damageable entity by the specified amount.
     *
     * @param <T>        The type of the entity to be healed, which must implement Damageable and Attributable.
     * @param entity     The entity to be healed.
     * @param healAmount The amount of health to regain. This value must be positive.
     */
    public static <T extends Damageable & Attributable> void heal(T entity, double healAmount) {
        heal(entity, healAmount, false);
    }

    /**
     * Heals a damageable entity by the specified amount.
     *
     * @param <T>            The type of the entity to be healed, which must implement Damageable and Attributable.
     * @param entity         The entity to be healed.
     * @param healAmount     The amount of health to regain. If allowNegatives is false, this value must be positive.
     * @param allowNegatives Whether negative heal amounts are allowed. If false, healAmount must be positive to heal the entity.
     */
    public static <T extends Damageable & Attributable> void heal(T entity, double healAmount, boolean allowNegatives) {
        if (!(healAmount > 0) && !allowNegatives) {
            throw new IllegalArgumentException("Heal amount must be strictly positive");
        }
        double currentHealth = entity.getHealth();
        double maxHealth = getAttributeValue(entity, Attribute.GENERIC_MAX_HEALTH);

        EntityRegainHealthEvent called = new EntityRegainHealthEvent(entity, healAmount, EntityRegainHealthEvent.RegainReason.CUSTOM);
        if (called.callEvent()) {
            entity.setHealth(Math.min(currentHealth + called.getAmount(), maxHealth));
        }
    }

    /**
     * Safely retrieves the value of an attribute for a given entity.
     *
     * @param entity    The entity whose attribute value is being retrieved.
     * @param attribute The attribute whose value is being retrieved.
     * @return The value of the attribute for the entity, or 0.0 if the attribute instance is not found.
     */
    public static double getAttributeValue(Attributable entity, Attribute attribute) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        return attributeInstance != null ? attributeInstance.getValue() : 0.0;
    }

    /**
     * Safely retrieves the base value of an attribute for a given entity.
     *
     * @param entity    The entity whose attribute base value is being retrieved.
     * @param attribute The attribute whose base value is being retrieved.
     * @return The base value of the attribute for the entity, or 0.0 if the attribute instance is not found.
     */
    public static double getAttributeBaseValue(Attributable entity, Attribute attribute) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        return attributeInstance != null ? attributeInstance.getBaseValue() : 0.0;
    }

    /**
     * Safely retrieves the default value of an attribute for a given entity.
     *
     * @param entity    The entity whose attribute default value is being retrieved.
     * @param attribute The attribute whose default value is being retrieved.
     * @return The default value of the attribute for the entity, or 0.0 if the attribute instance is not found.
     */
    public static double getAttributeDefaultValue(Attributable entity, Attribute attribute) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        return attributeInstance != null ? attributeInstance.getDefaultValue() : 0.0;
    }

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
     * Converts a camel case string (e.g., "MySimpleWord") to kebab-case (e.g., "my_simple_word").
     *
     * @param input the camel case string to be converted
     * @return the converted kebab-case string, or the input string if it is null or empty
     */
    public static String convertToKebabCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();

        for (char c : chars) {
            if (Character.isUpperCase(c)) {
                if (!result.isEmpty()) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Super useful to display enum names like DIAMOND_SWORD in chat.
     *
     * @param input String with lower cases and spaces only
     * @return Same string with capital letters at the beginning of each word.
     */
    public static String caseOnWords(String input) {
        StringBuilder builder = new StringBuilder(input);

        boolean isLastSpace = true;
        for (int i = 0; i < builder.length(); i++) {
            char ch = builder.charAt(i);
            if (isLastSpace && ch >= 'a' && ch <= 'z') {
                builder.setCharAt(i, (char) (ch + ('A' - 'a')));
                isLastSpace = false;
            } else {
                isLastSpace = (ch == ' ');
            }
        }
        return builder.toString();
    }

    /**
     * Reads an icon string and converts it into an {@link ItemStack}.
     * <p>
     * The icon string should be in the format "MATERIAL" or "MATERIAL:customModelData".
     * <p>
     * Example formats:
     * <ul>
     * <li>{@code DIAMOND}</li>
     * <li>{@code DIAMOND:123}</li>
     * </ul>
     *
     * @param icon The icon string representing the material and optional custom model data.
     * @return The created {@link ItemStack}.
     * @throws IllegalArgumentException If the material is invalid or the custom model data is not a number.
     */
    public static ItemStack readIcon(String icon) throws IllegalArgumentException {
        String[] split = icon.split(":");
        Material material = Material.valueOf(split[0].toUpperCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_"));

        ItemStackBuilder itemStack = ItemStackBuilder.of(material);
        if (split.length > 1) {
            itemStack.customModelData(Integer.parseInt(split[1]));
        }
        return itemStack.build();
    }

    /**
     * Creates an object using the provided factory {@link Supplier}.
     *
     * @param <T>     The type of object to create.
     * @param factory The factory {@link Supplier} to create the object.
     * @return The created object.
     */
    public static <T> T make(Supplier<T> factory) {
        return factory.get();
    }

    /**
     * Initializes an object using the provided {@link Consumer} initializer.
     *
     * @param <T>         The type of object to initialize.
     * @param object      The object to initialize.
     * @param initializer The {@link Consumer} initializer to apply to the object.
     * @return The initialized object.
     */
    public static <T> T make(T object, Consumer<? super T> initializer) {
        initializer.accept(object);
        return object;
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
        // perform the ray trace in the world from the origin in the specified direction
//        @Nullable RayTraceResult result = origin.getWorld().rayTraceBlocks(origin, clonedDirection, multiply,
//                FluidCollisionMode.NEVER, true);
//
//        // if no block is hit, create an approximate result at the endpoint of the ray
//        if (result == null) {
//            result = new RayTraceResult(origin.clone()
//                    .add(clonedDirection.multiply(multiply)).toVector());
//        }
//        // return the location where the ray trace ends
//        return result.getHitPosition().toLocation(origin.getWorld());
    }

    public static void attack(SkillMetadata meta, Location location, double damage, double radius, double knockback, double repulsion, boolean shouldSwing, @Nullable Element element, DamageType... types) {
        LivingEntity caster = meta.entity();

//        if (caster instanceof Player && ((Player) caster).hasDoneTrainingArcLove()) {
//            radius *= 1.1;
//        }

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

            if (canTarget(meta.plugin(), caster, victim)) {
                isBlocking = false;

                LivingEntity target = (LivingEntity) victim;
                d = damage;
                k = knockback;

                boolean isCasterCasting = EntityMetadataProvider.isCasting(caster);
                boolean isVictimCasting = EntityMetadataProvider.isCasting(target);

                if (isVictimCasting && isCasterCasting && damage > 0.0) {
                    isBlocking = true;
                    d *= 0.5;
                    k *= 0.5;
                }

//                if (redSwordEnchantment && target.isDemon() && isNotTanjiroDemon) {
//                    d *= 1.0 + (redSwordLevel * 0.125);
//                    // maybe in the future apply red sword effect instance
//                }

//                double armorValue = getAttributeValue(target, Attribute.GENERIC_ARMOR);
//                if (!(target instanceof Player) || (((Player) target).hasDoneTrainingArcSerpent() && Math.random() < 0.3)) {
//                    d *= 1.0 - Math.max((armorValue - d * 0.5) * 0.04, armorValue * 0.008);
//                }

//                if (target instanceof Player && ((Player) target).hasDoneTrainingArcStone()) {
//                    d *= 0.9;
//                }

//                if (target.hasPotionEffect(PotionEffectType.DEMON_SLAYER_MARK)) {
//                    d *= 0.9;
//                }

//                if (target instanceof Player) {
//                    switch (target.getWorld().getDifficulty()) {
//                        case HARD:
//                            d *= 1.0;
//                            break;
//                        case NORMAL:
//                            d *= 0.75;
//                            break;
//                        default:
//                            d *= 0.5;
//                            break;
//                    }
//                }

                boolean isKnockback = k != 0.0;

                AttackMetadata attackMetadata = meta.caster().attack(target, d, isKnockback, element, types);
                DamageMetadata damageMetadata = attackMetadata.getMetadata();

                if (damageMetadata.getDamage() > DamageMetadata.MINIMAL_DAMAGE) {
                    isSwinging = true;

                    if (isBlocking) {
                        intelligentHandsSwing(target);
                        location.getWorld().playSound(target, "minecraft:custom.generic.sword_guard", 0.5f, 1.0f);
                        location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getEyeLocation(),
                                5, 0.5, 0.5, 0.5, 0.5);
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
//        if (entity.hasSlayerMark()) {
//            s += 0.25;
//        }
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
