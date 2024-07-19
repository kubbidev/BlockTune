package me.kubbidev.blocktune.core.skill.handler.def;

import me.kubbidev.blocktune.core.UtilityMethod;
import me.kubbidev.blocktune.core.damage.DamageType;
import me.kubbidev.blocktune.core.damage.Element;
import me.kubbidev.blocktune.core.entity.EntityMetadataProvider;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.skill.handler.SkillHandler;
import me.kubbidev.blocktune.core.skill.result.def.SimpleSkillResult;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class Dance extends SkillHandler<SimpleSkillResult> {
    private static final double OFFSET = 3.0 * Math.PI / 20.0;

    @Override
    public SimpleSkillResult getResult(SkillMetadata meta) {
        return new SimpleSkillResult();
    }

    @Override
    public void whenCast(SimpleSkillResult result, SkillMetadata meta) {
        LivingEntity caster = meta.entity();
        // attach this handler as casting in the entity metadata map instance
        EntityMetadataProvider.onCastStart(caster, this);

        double damage = meta.parameter("damage");
        double radius = meta.parameter("radius");

        double knockback = meta.parameter("knockback");
        double repulsion = meta.parameter("repulsion");
        new BukkitRunnable() {
            Location location = null;

            double t = 0.0;

            @Override
            public void run() {
                if (!caster.isValid() || (t += Math.PI / 4.0) > (5.0 * Math.PI / 4.0)) {
                    // remove this handler from casting in the caster metadata map instance
                    EntityMetadataProvider.onCastEnd(caster, Dance.this);
                    cancel();
                    return;
                }
                if (location == null) {
                    location = caster.getLocation();

                    location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.f);
                    location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.f);
                    caster.swingMainHand();
                }

                for (int layer = 0; layer < 4; layer++) {
                    double layerRadius = 3.0 + layer * 0.33;

                    for (double i = 0; i <= Math.PI / 4; i += Math.PI / 18) {
                        double y = Math.sin(i + t + OFFSET) * layerRadius + 0.6;
                        double z = Math.cos(i + t + OFFSET) * layerRadius;

                        Vector rotated = new Vector(0, y, -z)
                                .rotateAroundX(Math.toRadians(location.getPitch()))
                                .rotateAroundY(Math.toRadians(-location.getYaw()));

                        Location displayLoc = location.clone().add(rotated);
                        UtilityMethod.attack(meta, displayLoc,
                                damage,
                                radius,
                                knockback,
                                repulsion, false, Element.FIRE,
                                DamageType.MAGIC,
                                DamageType.SKILL
                        );

                        if (layer > 1) {
                            Particle.FLAME.builder().location(displayLoc)
                                    .count(4).offset(0.1, 0.1, 0.1).extra(0.02).spawn();
                        } else {
                            Particle.FLAME.builder().location(displayLoc)
                                    .count(2).offset(0.1, 0.1, 0.1).extra(0.01).spawn();
                        }
                        Particle.INSTANT_EFFECT.builder().location(displayLoc)
                                .count(2).offset(0.05, 0.05, 0.05).spawn();
                    }
                }

            }
        }.runTaskTimer(meta.plugin(), 0, 1);
    }
}
