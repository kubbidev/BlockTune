package me.kubbidev.blocktune.core.skill.handler.def;

import me.kubbidev.blocktune.core.UtilityMethod;
import me.kubbidev.blocktune.core.damage.DamageType;
import me.kubbidev.blocktune.core.damage.Element;
import me.kubbidev.blocktune.core.entity.EntityMetadataProvider;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.skill.handler.SkillHandler;
import me.kubbidev.blocktune.core.skill.result.def.SimpleSkillResult;
import me.kubbidev.blocktune.core.util.EntityBody;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
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
        // attach this handler as casting in the entity metadata map instance
        EntityMetadataProvider.onCastStart(caster, this);

        if (caster.getVelocity().getY() >= -0.5) {
            caster.setVelocity(caster.getVelocity().setY(1.0));
        }
        double damage = meta.parameter("damage");
        double radius = meta.parameter("radius");

        double knockback = meta.parameter("knockback");
        double repulsion = meta.parameter("repulsion");
        new BukkitRunnable() {
            Location location = null;

            double theta = 0.0;

            @Override
            public void run() {
                if (!caster.isValid() || (theta += Math.PI / 2) > 5 * Math.PI / 2) {
                    // remove this handler from casting in the caster metadata map instance
                    EntityMetadataProvider.onCastEnd(caster, SettingSunTransformation.this);
                    cancel();
                    return;
                }

                if (theta == Math.PI) {
                    Particle.CLOUD.builder().location(caster.getLocation())
                            .count(15).extra(0.2).spawn();
                }

                if (theta > 3 * Math.PI / 2) {
                    if (caster.getVelocity().getY() > 0.2) {
                        caster.setVelocity(caster.getVelocity().setY(0.0));
                    }
                    if (theta == Math.PI * 2) {
                        if (location == null) {
                            location = EntityBody.BODY.getLocation(caster);
                        }
                        caster.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.0f);
                        caster.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f);
                        caster.swingMainHand();
                    }

                    for (int layer = 0; layer < 4; layer++) {
                        double layerRadius = 2.5 + layer * 0.66;

                        for (double i = 0; i <= Math.PI / 2; i += Math.PI / 24) {
                            double x = Math.cos(i + theta) * layerRadius;
                            double y = Math.sin(i + theta) * layerRadius;

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

                            Particle.FLAME.builder().location(displayLoc)
                                    .count(4).offset(0.1, 0.1, 0.1).extra(0.02).spawn();

                            Particle.INSTANT_EFFECT.builder().location(displayLoc)
                                    .offset(0.2, 0.2, 0.2).spawn();

                        }
                    }
                }
            }
        }.runTaskTimer(meta.plugin(), 0, 1);
    }
}
