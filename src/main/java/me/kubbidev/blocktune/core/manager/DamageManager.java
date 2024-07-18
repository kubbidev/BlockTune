package me.kubbidev.blocktune.core.manager;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.entity.EntityMetadata;
import me.kubbidev.blocktune.core.damage.*;
import me.kubbidev.blocktune.core.event.attack.AttackUnregisteredEvent;
import me.kubbidev.blocktune.core.util.EquipmentSlot;
import me.kubbidev.blocktune.core.UtilityMethod;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

/**
 * Central piece of the damage system.
 */
public final class DamageManager implements Listener {
    /**
     * Attribute modifier used to cancel the knockback the entity receive when being damaged.
     */
    private static final AttributeModifier NO_KNOCKBACK = new AttributeModifier(new NamespacedKey("blocktune", "no_knockback"),
            100, AttributeModifier.Operation.ADD_NUMBER);

    /**
     * The singleton plugin instance.
     */
    private final BlockTune plugin;

    /**
     * External attack handlers.
     */
    private final List<AttackHandler> handlers = new ArrayList<>();

    /**
     * There is an issue with metadata not being garbage-collected on mobs.
     * It looks like persistent data containers do also suffer from that issue.
     * <p>
     * Switched back to using a weak hash map to save the current attack
     * metadata for a mob.
     * <p>
     * Weak hash maps are great for garbage collection.
     */
    private final Map<UUID, AttackMetadata> attackMetadataMap = new WeakHashMap<>();

    public DamageManager(@NotNull BlockTune plugin) {
        this.plugin = plugin;
    }

    /**
     * This method is used to unregister custom {@link AttackMetadata} after everything
     * was calculated, hence MONITOR priority.
     * <p>
     * As a safe practice, it does NOT ignore cancelled damage events.
     * <p>
     * It does however ignore fake events as they are sometimes called for checking interaction rules after the metadata
     * has been set, but before effects are being applied which can screw things up.
     * <p>
     * This method is ABSOLUTELY NECESSARY.
     * <p>
     * While BlockTune does clean up the entity metadata as soon as damage is dealt, vanilla
     * attacks and extra plugins just don't.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void unregisterCustomAttacks(EntityDamageEvent e) {
        if (this.plugin.getFakeEventManager().isFake(e)) {
            return;
        }

        if (e.getEntity() instanceof LivingEntity) {
            @Nullable AttackMetadata attack = unmarkAsMetadata(e.getEntity());

            if (attack != null && !e.isCancelled() && e.getFinalDamage() > 0) {
                AttackUnregisteredEvent called = new AttackUnregisteredEvent(e, attack);
                called.callEvent();
            }
        }
    }

    /**
     * {@link AttackHandler}s are used to keep track of details of every
     * attack so that it can apply damage based stats like PvE damage, Magic
     * Damage...
     *
     * @param handler The damage handler being registered.
     */
    public void registerHandler(AttackHandler handler) {
        Objects.requireNonNull(handler, "Damage handler cannot be null");
        this.handlers.add(handler);
    }

    public List<AttackHandler> getHandlers() {
        return ImmutableList.copyOf(this.handlers);
    }

    /**
     * Forces a player to damage an entity with knockback
     *
     * @param metadata The class containing all info about the current attack
     */
    public void registerAttack(AttackMetadata metadata) {
        registerAttack(metadata, true, false);
    }

    /**
     * Forces a player to damage an entity with (no) knockback
     *
     * @param metadata  The class containing all info about the current attack
     * @param knockback If the attack should deal knockback
     */
    public void registerAttack(AttackMetadata metadata, boolean knockback) {
        registerAttack(metadata, knockback, false);
    }


    /**
     * Deals damage to an entity. Does not do anything if the
     * damage is negative or null.
     *
     * @param attack         The class containing all info about the current attack
     * @param knockback      If the attack should deal knockback
     * @param ignoreImmunity The attack will not produce immunity frames.
     */
    public void registerAttack(AttackMetadata attack, boolean knockback, boolean ignoreImmunity) {
        markAsMetadata(attack);

        LivingEntity attacker = attack.getAttacker() == null ? null : attack.getAttacker().entity();
        LivingEntity target = attack.getTarget();
        try {
            applyDamage(attack.getMetadata().getDamage(), target, attacker, knockback, ignoreImmunity);
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "Caught an exception (1) while damaging entity '" + target.getUniqueId() + "':", e);
        } finally {
            unmarkAsMetadata(target);
        }
    }

    private void applyDamage(double damage, LivingEntity target, @Nullable LivingEntity damager, boolean knockback, boolean ignoreImmunity) {

        // should knockback be applied
        if (!knockback) {
            AttributeInstance instance = target.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
            try {
                if (instance != null) {
                    instance.addModifier(NO_KNOCKBACK);
                }
                applyDamage(damage, target, damager, true, ignoreImmunity);
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Caught an exception (2) while damaging entity '" + target.getUniqueId() + "':", e);
            } finally {
                if (instance != null) {
                    instance.removeModifier(NO_KNOCKBACK);
                }
            }
            // should damage immunity be taken into account
        } else if (ignoreImmunity) {
            int noDamageTicks = target.getNoDamageTicks();
            try {
                target.setNoDamageTicks(0);
                applyDamage(damage, target, damager, true, false);
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Caught an exception (3) while damaging entity '" + target.getUniqueId() + "':", e);
            } finally {
                target.setNoDamageTicks(noDamageTicks);
            }
            // just damage entity
        } else {
            Preconditions.checkArgument(damage > 0, "Damage must be strictly positive");
            if (damager == null) {
                target.damage(damage);
            } else {
                target.damage(damage, damager);
            }
        }
    }

    /**
     * Very important method.
     * <p>
     * Looks for a RegisteredAttack that would have been registered by other plugins.
     * <p>
     * If it can't find any plugin that has registered an attack, it checks if it is simply
     * not just a vanilla attack: {@link ProjectileAttackMetadata} or {@link MeleeAttackMetadata}.
     * <p>
     * If so it registers this new attack meta to make sure the same attackMeta is provided later on.
     *
     * @param e The attack event.
     * @return <code>null</code> if BlockTune cannot find the attack source, some attack meta otherwise.
     */
    public AttackMetadata findAttack(EntityDamageEvent e) {
        Preconditions.checkArgument(e.getEntity() instanceof LivingEntity, "Target entity is not living");

        LivingEntity entity = (LivingEntity) e.getEntity();

        // attack registry
        @Nullable AttackMetadata attackFound = getRegisteredAttackMetadata(entity);
        if (attackFound != null) return attackFound;

        // attack registries from other plugins
        for (AttackHandler handler : this.handlers) {
            attackFound = handler.getAttack(e);

            if (attackFound != null) {
                markAsMetadata(attackFound);
                return attackFound;
            }
        }

        // attack with a damager
        if (e instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) e).getDamager();

            if (damager instanceof LivingEntity) {
                EntityMetadata attacker = new EntityMetadata(this.plugin, (LivingEntity) damager, EquipmentSlot.MAIN_HAND);

                DamageMetadata damage = new DamageMetadata(e.getDamage(), getVanillaDamageTypes((EntityDamageByEntityEvent) e, EquipmentSlot.MAIN_HAND));
                AttackMetadata attack = new MeleeAttackMetadata(damage, entity, attacker);

                markAsMetadata(attack);
                return attack;
            } else if (damager instanceof Projectile projectile) {
                // try to trace back the shooter source
                @Nullable ProjectileSource source = projectile.getShooter();
                if (source != null && !source.equals(entity) && source instanceof LivingEntity) {
                    EntityMetadata attacker = new EntityMetadata(this.plugin, (LivingEntity) source, EquipmentSlot.MAIN_HAND);

                    DamageMetadata damage = new DamageMetadata(e.getDamage(), DamageType.WEAPON, DamageType.PHYSICAL, DamageType.PROJECTILE);
                    AttackMetadata attack = new ProjectileAttackMetadata(damage, entity, attacker, projectile);

                    markAsMetadata(attack);
                    return attack;
                }
            }
        }

        // attack with no damager
        DamageMetadata damage = new DamageMetadata(e.getDamage(), getVanillaDamageTypes(e));
        AttackMetadata attack = new AttackMetadata(damage, entity, null);

        markAsMetadata(attack);
        return attack;
    }

    /**
     * Registers the {@link AttackMetadata} inside the entity metadata.
     * <p>
     * This does NOT apply any damage to the target entity.
     *
     * @param attack The attack metadata being registered.
     * @return The {@link AttackMetadata} already present on the entity, if it's not
     * the case it's throws an internal error.
     */
    public @Nullable AttackMetadata markAsMetadata(AttackMetadata attack) {
        @Nullable AttackMetadata found = this.attackMetadataMap.put(attack.getTarget().getUniqueId(), attack);
        if (found != null) {
            this.plugin.getLogger().warning("Please report this issue to the developer: persistent attack metadata was found.");
        }
        return found;
    }

    /**
     * @param target Target of current attack.
     * @return The {@link AttackMetadata} that was found, if any.
     */
    public @Nullable AttackMetadata unmarkAsMetadata(Entity target) {
        return this.attackMetadataMap.remove(target.getUniqueId());
    }

    /**
     * @param e The attack event.
     * @return The damage types of a vanilla attack
     */
    public DamageType[] getVanillaDamageTypes(EntityDamageEvent e) {
        return getVanillaDamageTypes(e.getCause());
    }

    /**
     * @param cause The cause of the attack.
     * @return The damage types of a vanilla attack
     */
    @SuppressWarnings("EnhancedSwitchMigration")
    public DamageType[] getVanillaDamageTypes(EntityDamageEvent.DamageCause cause) {
        switch (cause) {
            case MAGIC:
            case DRAGON_BREATH:
                return new DamageType[]{DamageType.MAGIC};
            case POISON:
            case WITHER:
                return new DamageType[]{DamageType.MAGIC, DamageType.DOT};
            case FIRE_TICK:
            case MELTING:
                return new DamageType[]{DamageType.PHYSICAL, DamageType.DOT};
            case STARVATION:
            case DRYOUT:
            case FREEZE:
                return new DamageType[]{DamageType.DOT};
            case FIRE:
            case LAVA:
            case HOT_FLOOR:
            case SONIC_BOOM:
            case LIGHTNING:
            case FALL:
            case THORNS:
            case CONTACT:
            case ENTITY_EXPLOSION:
            case ENTITY_SWEEP_ATTACK:
            case FALLING_BLOCK:
            case FLY_INTO_WALL:
            case BLOCK_EXPLOSION:
            case ENTITY_ATTACK:
            case SUFFOCATION:
            case CRAMMING:
            case DROWNING:
                return new DamageType[]{DamageType.PHYSICAL};
            case PROJECTILE:
                return new DamageType[]{DamageType.PHYSICAL, DamageType.PROJECTILE};
            default:
                return new DamageType[0];
        }
    }

    /**
     * @param e    The attack event.
     * @param hand The hand used to perform the attack.
     * @return The damage types of a vanilla melee entity attack
     */
    public DamageType[] getVanillaDamageTypes(EntityDamageByEntityEvent e, EquipmentSlot hand) {
        Preconditions.checkArgument(e.getDamager() instanceof LivingEntity, "Not an entity attack");
        return getVanillaDamageTypes((LivingEntity) e.getDamager(), e.getCause(), hand);
    }

    /**
     * @param damager The entity attacking.
     * @param cause   The cause of the attack.
     * @param hand    The hand used to perform the attack.
     * @return The damage types of a vanilla melee entity attack
     */
    public DamageType[] getVanillaDamageTypes(LivingEntity damager, EntityDamageEvent.DamageCause cause, EquipmentSlot hand) {

        // not an entity attack
        if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK && cause != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)
            return new DamageType[]{DamageType.PHYSICAL};

        // physical attack with bare fists.
        @Nullable ItemStack handItem = null;
        @Nullable org.bukkit.inventory.EntityEquipment equipment = damager.getEquipment();

        if (equipment != null) {
            handItem = switch (hand) {
                case MAIN_HAND -> equipment.getItemInMainHand();
                case OFF_HAND -> equipment.getItemInOffHand();
                default -> throw new IllegalArgumentException("Must provide a hand slot");
            };
        }

        if (UtilityMethod.isAir(handItem))
            return new DamageType[]{DamageType.UNARMED, DamageType.PHYSICAL};

        // weapon attack
        if (UtilityMethod.isWeapon(handItem))
            return new DamageType[]{DamageType.WEAPON, DamageType.PHYSICAL};

        // hitting with a random item
        return new DamageType[]{DamageType.PHYSICAL};
    }

    public @Nullable AttackMetadata getRegisteredAttackMetadata(Entity entity) {
        return this.attackMetadataMap.get(entity.getUniqueId());
    }
}