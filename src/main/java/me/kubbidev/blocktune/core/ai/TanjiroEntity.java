package me.kubbidev.blocktune.core.ai;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.entity.EntityMetadataProvider;
import me.kubbidev.blocktune.core.skill.Ability;
import me.kubbidev.blocktune.core.skill.Skill;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.skill.handler.SkillHandler;
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

@ApiStatus.Experimental
@ApiStatus.Internal
public class TanjiroEntity extends SmartEntity {
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
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.ARMOR, 10.0);
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
                if (this.x > 40 && Math.random() < 0.5) {
                    instance.sendParticles(ParticleTypes.CLOUD, x, y + 1.6, z, 0, 0, 0, 0, 0);
                }
                if (this.x > 60) {
                    this.x = 0;

                    int s;
                    while (true) {
                        s = (int) (Math.ceil(Math.random() * 12.0) + 17.0);
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
                if (this.x > 50) {
                    this.x = 50;
                }
            }
        } else {
            if (!EntityMetadataProvider.isCasting(getBukkitLivingEntity())) {
                Ability ability = selectAbility();
                if (ability == null) {
                    // todo remove this in future
                    this.plugin.getLogger().info("Selected a null ability: " + this.m);
                    this.m = 0;
                } else {
                    Skill skill = getSkill(ability);
                    skill.cast(getBukkitLivingEntity());
                }
            }
        }
    }

    private @NotNull Skill getSkill(Ability ability) {
        return new Skill(this.plugin) {

            @Override
            public boolean getResult(SkillMetadata meta) {
                return true;
            }

            @Override
            public void whenCast(SkillMetadata meta) {

            }

            @Override
            public SkillHandler<?> getHandler() {
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
        } else if (this.m == 27) {
        } else if (this.m == 28) {
        } else if (this.m == 29) {
            ability = Ability.SOLAR_HEAT_HAZE;
        }
        return ability;
    }
}
