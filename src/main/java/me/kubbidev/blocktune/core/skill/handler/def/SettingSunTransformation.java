package me.kubbidev.blocktune.core.skill.handler.def;

import me.kubbidev.blocktune.core.UtilityMethod;
import me.kubbidev.blocktune.core.damage.DamageType;
import me.kubbidev.blocktune.core.damage.Element;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.skill.handler.SkillHandler;
import me.kubbidev.blocktune.core.skill.handler.SkillHandlerRunnable;
import me.kubbidev.blocktune.core.skill.result.def.SimpleSkillResult;
import me.kubbidev.blocktune.core.util.EntityBody;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class SettingSunTransformation extends SkillHandler<SimpleSkillResult> {

    @Override
    public SimpleSkillResult getResult(SkillMetadata meta) {
        return new SimpleSkillResult();
    }

    @Override
    public void whenCast(SimpleSkillResult result, SkillMetadata meta) {
        LivingEntity caster = meta.entity();

        double damage = meta.parameter("damage");
        double radius = meta.parameter("radius");

        double knockback = meta.parameter("knockback");
        double repulsion = meta.parameter("repulsion");
        new SkillHandlerRunnable() {
            Location location = null;

            double t = 0.0;

            @Override
            public boolean shouldCancel() {
                return !caster.isValid() || (t += Math.PI / 2) > 5 * Math.PI / 2;
            }

            @Override
            protected void tick() {
                if (t > 3 * Math.PI / 2) {
                    if (caster.getVelocity().getY() > 0.2) {
                        caster.setVelocity(caster.getVelocity().setY(0.0));
                    }
                    if (t == Math.PI * 2) {
                        if (location == null) {
                            location = EntityBody.BODY.getLocation(caster);
                        }
                        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.0f);
                        location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f);
                        caster.swingMainHand();
                    }

                    for (int layer = 0; layer < 4; layer++) {
                        double layerRadius = 3.0 + layer * 0.66;

                        for (double i = 0; i <= Math.PI / 2; i += Math.PI / 24) {
                            double x = Math.cos(i + t) * layerRadius;
                            double y = Math.sin(i + t) * layerRadius;

                            Vector rotated = new Vector(x, y, 0.0)
                                    .rotateAroundX(Math.toRadians(location.getPitch() + 90.f))
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

                            if (layer == 2) {
                                Particle.DUST.builder().location(displayLoc).color(Color.RED, 2.f)
                                        .count(2).offset(0.1, 0.1, 0.1).spawn();
                            } else {
                                Particle.FLAME.builder().location(displayLoc)
                                        .count(8).offset(0.1, 0.1, 0.1).extra(0.02).spawn();

                                Particle.INSTANT_EFFECT.builder().location(displayLoc)
                                        .count(2).offset(0.2, 0.2, 0.2).spawn();
                            }

                        }
                    }
                }
            }

            @Override
            protected void onStart() {
                if (caster.getVelocity().getY() >= -0.5) {
                    caster.setVelocity(caster.getVelocity().setY(1.0));
                }

                Particle.CLOUD.builder().location(caster.getLocation())
                        .count(15).extra(0.2).spawn();
            }

            @Override
            protected void onEnd() {

            }
        }.runTask(meta);
    }
}
