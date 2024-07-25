package me.kubbidev.blocktune.entity;

import com.google.common.base.Preconditions;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.spell.SpellMetadataProvider;
import me.kubbidev.blocktune.spell.Ability;
import me.kubbidev.spellcaster.SpellCasterProvider;
import me.kubbidev.spellcaster.spell.Spell;
import me.kubbidev.spellcaster.spell.SpellMetadata;
import me.kubbidev.spellcaster.spell.handler.SpellHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

@ApiStatus.Experimental
@ApiStatus.Internal
public class TanjiroEntity extends SmartEntity {
    private final Random random = new Random();
    // default values
    private int attackSpeed = 60;
    private int attackLimit = 50;
    private int breathSpeed = 40;

    public TanjiroEntity(BlockTune plugin, Location location) {
        super(plugin, location);

        setCustomName(Component.literal("Tanjiro"));
        setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(47, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(48, new FloatGoal(this));
        this.goalSelector.addGoal(49, new MoveBackToVillageGoal(this, 0.6, false));
        this.goalSelector.addGoal(50, new OpenDoorGoal(this, true));

        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers(getClass()));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, Monster.class, false, false));
    }

    @Override
    public @NotNull AttributeMap createAdditionalAttributes() {
        return new AttributeMap()
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.ARMOR, 10.0);
    }

    public int getAttackSpeed() {
        return this.attackSpeed;
    }

    /**
     * Sets the entity attack speed in ticks.
     *
     * @param attackSpeed speed in ticks.
     */
    public void setAttackSpeed(int attackSpeed) {
        Preconditions.checkArgument(attackSpeed > 0, "attack speed must be strictly positive");
        this.attackSpeed = attackSpeed;
        this.attackLimit = (int) (attackSpeed * (5.0 / 6.0));
        this.breathSpeed = (int) (attackSpeed * (2.0 / 3.0));
    }

    @SuppressWarnings("resource")
    @Override
    public void baseTick() {
        super.baseTick();
        double x = getX();
        double y = getY();
        double z = getZ();
        ServerLevel instance = (ServerLevel) level();
        if (!isAlive()) {
            return;
        }

        if (this.m == 0) {
            @Nullable LivingEntity target = getTarget();
            if (target != null) {
                this.x++;
                if (this.x > this.breathSpeed && Math.random() < 0.5) {
                    instance.sendParticles(ParticleTypes.CLOUD, x, y + 1.6, z, 0, 0, 0, 0, 0);
                }
                if (this.x > this.attackSpeed) {
                    this.x = 0;

                    int s;
                    while (true) {
                        s = (int) (Math.ceil(this.random.nextDouble() * 12.0) + 17.0);
                        if (target.distanceToSqr(this) > (12 * 12)) {
                            if (s == 23 || s == 25 || s == 26) {
                                break;
                            }
                        } else {
                            if (target.distanceToSqr(this) <= (6 * 6)) {
                                break;
                            }
                            if (s != 19 && s != 24 && s != 27 && s != 28 && s != 29) {
                                break;
                            }
                        }
                    }
                    this.m = s;
                }
            } else {
                if (this.x > this.attackLimit) {
                    this.x = this.attackLimit;
                }
            }
        } else {
            if (!SpellMetadataProvider.isCasting(getBukkitLivingEntity())) {
                Ability ability = selectAbility();
                if (ability == null) {
                    // todo remove this in future
                    this.plugin.getLogger().info("Selected a null ability: " + this.m);
                    this.m = 0;
                } else {
                    Spell spell = getSpell(ability);
                    spell.cast(getBukkitLivingEntity());
                }
            }
        }

        tickDirection(this);
    }

    private @NotNull Spell getSpell(Ability ability) {
        return new Spell(SpellCasterProvider.get()) {

            @Override
            public boolean getResult(SpellMetadata meta) {
                return true;
            }

            @Override
            public void whenCast(SpellMetadata meta) {

            }

            @Override
            public SpellHandler<?> getHandler() {
                return ability.getHandler();
            }

            @Override
            public double getParameter(String path) {
                return ability.getParameters().getOrDefault(path, 0d);
            }
        };
    }

    private @Nullable Ability selectAbility() {
        Ability ability = null;
        if (this.m == 18) {
            ability = Ability.DANCE;
        } else if (this.m == 19) {
            ability = Ability.CLEAR_BLUE_SKY;
        } else if (this.m == 20) {
            ability = Ability.RAGING_SUN;
        } else if (this.m == 21) {
            ability = Ability.BURNING_BONES_SUMMER_SUN;
        } else if (this.m == 22) {
            ability = Ability.SUNFLOWER_THRUST;
        } else if (this.m == 23) {
            ability = Ability.SUN_HALO_DRAGON_HEAD_DANCE;
        } else if (this.m == 24) {
            ability = Ability.SETTING_SUN_TRANSFORMATION;
        } else if (this.m == 25) {
            ability = Ability.BENEFICENT_RADIANCE;
        } else if (this.m == 26) {
            ability = Ability.FIRE_WHEEL;
        } else if (this.m == 27) {
        } else if (this.m == 28) {
        } else if (this.m == 29) {
            ability = Ability.SOLAR_HEAT_HAZE;
        }
        return ability;
    }
}
