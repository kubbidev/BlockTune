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
public class BurningBonesSummerSun extends SkillHandler<SimpleSkillResult> {

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
            double t = 0.0;

            @Override
            public boolean shouldCancel() {
                return !caster.isValid() || (t += Math.PI / 2.0) >= Math.PI * 8.0;
            }

            @Override
            protected void tick() {
                Location location = caster.getLocation();
                caster.getWorld().playSound(caster, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f);
                caster.swingMainHand();

                Vector currentVelocity = caster.getVelocity();
                Vector reducedVelocity = currentVelocity.clone().multiply(1.0 / 3.0);
                caster.setVelocity(new Vector(reducedVelocity.getX(), currentVelocity.getY(), reducedVelocity.getZ()));

                for (double i = 0.0; i < Math.PI / 2.0; i += Math.PI / 24.0) {
                    double x = Math.cos(i + t) * t / 5.0;
                    double y = Math.sin(i + t) * t / 5.0;
                    double z = t / 4;

                    Vector rotated = new Vector(x, y, z)
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

                    Particle.DUST.builder().location(displayLoc).color(Color.RED, 2.f)
                            .offset(0.1, 0.1, 0.1).spawn();

                    Particle.FLAME.builder().location(displayLoc)
                            .count(4).offset(0.2, 0.2, 0.2).extra(0.1).spawn();

                    Particle.SWEEP_ATTACK.builder().location(displayLoc)
                            .extra(0.01).spawn();

                    Particle.INSTANT_EFFECT.builder().location(displayLoc)
                            .count(2).offset(0.5, 0.5, 0.5).spawn();
                }
            }

            @Override
            protected void onStart() {
                caster.getWorld().playSound(caster, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.0f);
            }

            @Override
            protected void onEnd() {

            }
        }.runTask(meta);
    }
}
