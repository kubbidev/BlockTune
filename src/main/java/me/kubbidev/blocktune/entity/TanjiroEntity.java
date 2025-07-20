package me.kubbidev.blocktune.entity;

import com.google.common.base.Preconditions;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.spell.SpellMetadataProvider;
import me.kubbidev.blocktune.spell.Ability;
import me.kubbidev.spellcaster.spell.Spell;
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
public final class TanjiroEntity extends SmartEntity {

    private final Random random      = new Random();
    // default values
    private       int    attackSpeed = 60;
    private       int    attackLimit = 50;
    private       int    breathSpeed = 40;

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
        if (!isAlive()) {
            return;
        }
        double x = getX();
        double y = getY();
        double z = getZ();
        ServerLevel instance = (ServerLevel) level();
        tickDirection(this);

        if (this.m == 0) {
            @Nullable LivingEntity target = getTarget();
            if (target == null) {
                if (this.x > this.attackLimit) {
                    this.x = this.attackLimit;
                }
            } else {
                this.x++;
                if (this.x > this.breathSpeed && Math.random() < 0.5) {
                    instance.sendParticles(ParticleTypes.CLOUD, x, y + 1.6, z, 0, 0, 0, 0, 0);
                }
                if (this.x > this.attackSpeed) {
                    this.x = 0;

                    int s;
                    while (true) {
                        s = this.random.nextInt(12);

                        var distance = target.distanceToSqr(this);
                        if (distance > (12 * 12)) {
                            // high distance (only high range spell)
                            if (s == 6 || s == 8 || s == 9) {
                                break;
                            }
                        } else {
                            // close distance (accept anything)
                            if (distance <= (6 * 6)) {
                                break;
                            }
                            // mid distance (anything except close range spell)
                            if (s != 1 && s != 4 && s != 5 && s != 10 && s != 11) {
                                break;
                            }
                        }
                    }
                    this.m = s;
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
                    Spell spell = new SmartEntitySpell(ability);
                    spell.cast(getBukkitLivingEntity());
                }
            }
        }
    }

    private @Nullable Ability selectAbility() {
        return switch (this.m) {
            case 0 -> Ability.DANCE;
            case 1 -> Ability.CLEAR_BLUE_SKY;
            case 2 -> Ability.RAGING_SUN;
            case 3 -> Ability.BURNING_BONES_SUMMER_SUN;
            case 4 -> Ability.SETTING_SUN_TRANSFORMATION;
            case 5 -> Ability.SOLAR_HEAT_HAZE;
            case 6 -> Ability.BENEFICENT_RADIANCE;
            case 7 -> Ability.SUNFLOWER_THRUST;
            case 8 -> Ability.SUN_HALO_DRAGON_HEAD_DANCE;
            case 9 -> Ability.FIRE_WHEEL;
            case 10, 11 -> null;
            default -> throw new IllegalStateException("Unexpected value: " + this.m);
        };
    }
}
