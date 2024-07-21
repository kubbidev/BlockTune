package me.kubbidev.blocktune.core.ai;

import com.google.common.collect.ImmutableMap;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.event.skill.EndSkillCastEvent;
import me.kubbidev.nexuspowered.Events;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.monster.Zombie;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
@ApiStatus.Internal
public abstract class SmartEntity extends Zombie {
    static {
        // todo rework this shit
        Events.subscribe(EndSkillCastEvent.class)
                .handler(e -> {
                    LivingEntity entity = ((CraftLivingEntity) e.getEntity()).getHandle();
                    if (entity instanceof SmartEntity) {
                        ((SmartEntity) entity).m = 0;
                    }
                });
    }

    protected final BlockTune plugin;

    public int m = 0;
    public int x = 0;

    public SmartEntity(BlockTune plugin, Location location) {
        super(EntityType.ZOMBIE, ((CraftWorld) location.getWorld()).getHandle());
        this.plugin = plugin;
        setPos(
                location.getX(),
                location.getY(),
                location.getZ()
        );
        setShouldBurnInDay(false);
        setPersistenceRequired();

        ImmutableMap<Holder<Attribute>, Double> attributes = createAdditionalAttributes().build();
        attributes.forEach((attributeHolder, d) -> {
            AttributeInstance a = getAttribute(attributeHolder);
            if (a != null) {
                a.setBaseValue(d);
            }
        });

        setHealth(getMaxHealth());
    }

    @NotNull
    public abstract AttributeMap createAdditionalAttributes();

    @SuppressWarnings("resource")
    public void spawn() {
        level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    @Override
    protected abstract void registerGoals();

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.GENERIC_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_DEATH;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (!isAlive()) {
            return;
        }
        if (!hasEffect(MobEffects.MOVEMENT_SPEED)) {
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 0, false, false));
        }

        @Nullable LivingEntity target = getTarget();
        if (target != null && target.distanceToSqr(this) > (18 * 18) && this.m == 0) {
            int level = 0;

            MobEffectInstance instance = getEffect(MobEffects.DAMAGE_BOOST);
            if (instance != null) {
                level = (int) (instance.getAmplifier() / 3.0);
            }

            addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, 10, level, false, false
            ));
            addEffect(new MobEffectInstance(
                    MobEffects.JUMP, 20, level, false, false
            ));
        }
    }

    public static class AttributeMap {
        private final ImmutableMap.Builder<Holder<Attribute>, Double> builder = ImmutableMap.builder();
        private boolean instanceFrozen;

        private AttributeMap create(Holder<Attribute> attribute, double baseValue) {
            if (this.instanceFrozen) {
                throw new UnsupportedOperationException("Tried to change value for default attribute instance: " + attribute.getRegisteredName());
            }
            this.builder.put(attribute, baseValue);
            return this;
        }

        public AttributeMap add(Holder<Attribute> attribute) {
            return create(attribute, attribute.value().getDefaultValue());
        }

        public AttributeMap add(Holder<Attribute> attribute, double baseValue) {
            return create(attribute, baseValue);
        }

        private ImmutableMap<Holder<Attribute>, Double> build() {
            this.instanceFrozen = true;
            return this.builder.buildKeepingLast();
        }
    }
}
