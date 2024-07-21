package me.kubbidev.blocktune.core.skill.handler.def;

import me.kubbidev.blocktune.core.UtilityMethod;
import me.kubbidev.blocktune.core.damage.DamageType;
import me.kubbidev.blocktune.core.damage.Element;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.skill.handler.SkillHandler;
import me.kubbidev.blocktune.core.skill.handler.SkillHandlerRunnable;
import me.kubbidev.blocktune.core.skill.result.def.SimpleSkillResult;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class SolarHeatHaze extends SkillHandler<SimpleSkillResult> {
    private static final double OFFSET = Math.PI / 8.0;

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
            int t = 0;
            double theta = 0;

            @Override
            public boolean shouldCancel() {
                return !caster.isValid() || (theta += Math.PI / 6.0) > Math.PI;
            }

            @Override
            protected void tick() {
                for (int layer = 0; layer < 3; layer++) {
                    double layerRadius = 3.2 + layer * 0.33;

                    for (double i = 0; i <= Math.PI / 6; i += Math.PI / 18) {
                        double y = Math.sin(i + theta + OFFSET) * layerRadius + 1.5;
                        double z = Math.cos(i + theta + OFFSET) * layerRadius;

                        Vector rotated = new Vector(0, y, -z)
                                .rotateAroundZ(Math.PI / 5.0)
                                .rotateAroundX(Math.toRadians(caster.getLocation().getPitch()))
                                .rotateAroundY(Math.toRadians(-caster.getLocation().getYaw()));

                        Location displayLoc = caster.getLocation().add(rotated);
                        UtilityMethod.attack(meta, displayLoc,
                                damage,
                                radius,
                                knockback,
                                repulsion, false, Element.FIRE,
                                DamageType.MAGIC,
                                DamageType.SKILL
                        );

                        if (theta < Math.PI / 2) {
                            Particle.SOUL_FIRE_FLAME.builder().location(displayLoc)
                                    .count(4).offset(0.2, 0.2, 0.2).extra(0.05).spawn();

                            Particle.INSTANT_EFFECT.builder().location(displayLoc)
                                    .count(4).offset(0.2, 0.2, 0.2).extra(0.02).spawn();
                        } else {
                            Particle.FLAME.builder().location(displayLoc)
                                    .count(4).offset(0.2, 0.2, 0.2).extra(0.05).spawn();
                        }
                        Particle.DUST.builder().location(displayLoc).color(Color.fromRGB(
                                        UtilityMethod.clampColorToRange(0.3 + t / 120.0), UtilityMethod.clampColorToRange(0.3),
                                        UtilityMethod.clampColorToRange(1.0 - t / 120.0)), 2.f)
                                .count(4).offset(0.25, 0.25, 0.25).spawn();
                    }
                    t += 5;
                }
            }

            @Override
            protected void onStart() {
                caster.setVelocity(new Vector(0.0, Math.min(caster.getVelocity().getY(), 0.0), 0.0));

                caster.getWorld().playSound(caster, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.0f);
                caster.getWorld().playSound(caster, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f);
                caster.getWorld().playSound(caster, Sound.ITEM_TOTEM_USE, 0.5f, 1.0f);
                caster.swingMainHand();
            }

            @Override
            protected void onEnd() {

            }
        }.runTask(meta);
    }
}
