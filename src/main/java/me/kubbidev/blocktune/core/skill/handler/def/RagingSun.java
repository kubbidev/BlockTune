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
public class RagingSun extends SkillHandler<SimpleSkillResult> {

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
            int t = 0;

            @Override
            public void run() {
                if (!caster.isValid() || t++ > 5) {
                    // remove this handler from casting in the caster metadata map instance
                    EntityMetadataProvider.onCastEnd(caster, RagingSun.this);
                    cancel();
                    return;
                }

                if (t == 1) spawnCircularSlash(45.f, 67.5f);
                if (t == 6) spawnCircularSlash(-45.f, -67.5f);
            }

            private void spawnCircularSlash(double offsetAngle, double yawAngle) {
                caster.getWorld().playSound(caster, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.f);
                caster.getWorld().playSound(caster, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.f);
                caster.swingMainHand();

                // block the pitch at 0 to avoid the hit point location
                // being above or under the entity y when ray casting
                Location location = caster.getLocation();
                location.setPitch(0.f);

                // rotate the direction of the caster looking direction to
                // match the targeted yaw direction
                Vector direction = location.getDirection();
                direction.rotateAroundY(Math.toRadians(offsetAngle));

                // ray cast the offset center location of the circle we will spawn
                Location offsetLocation = UtilityMethod.fastRayTrace(location, direction, 2.0);

                for (int layer = 0; layer < 4; layer++) {
                    double layerRadius = 2.2 + layer * 0.33;

                    for (double i = 0; i <= Math.PI; i += Math.PI / 18) {
                        double x = Math.cos(i) * layerRadius;
                        double z = Math.sin(i) * layerRadius;

                        Vector rotated = new Vector(x, 1.0, z)
                                .rotateAroundY(Math.toRadians(-caster.getYaw() + yawAngle));

                        Location displayLoc = offsetLocation.clone().add(rotated);
                        UtilityMethod.attack(meta, displayLoc,
                                damage,
                                radius,
                                knockback,
                                repulsion, false, Element.FIRE,
                                DamageType.MAGIC,
                                DamageType.SKILL
                        );

                        if (layer == 0) {
                            Particle.INSTANT_EFFECT.builder().location(displayLoc)
                                    .count(8).offset(0.1, 0.1, 0.1).spawn();
                        } else {
                            Particle.FLAME.builder().location(displayLoc)
                                    .count(4).offset(0.2, 0.2, 0.2).extra(0.05).spawn();

                            Particle.SWEEP_ATTACK.builder().location(displayLoc)
                                    .offset(0.2, 0.05, 0.2).spawn();

                            Particle.INSTANT_EFFECT.builder().location(displayLoc)
                                    .offset(0.2, 0.5, 0.2).spawn();
                        }
                    }

                }
            }
        }.runTaskTimer(meta.plugin(), 0, 1);
    }
}
