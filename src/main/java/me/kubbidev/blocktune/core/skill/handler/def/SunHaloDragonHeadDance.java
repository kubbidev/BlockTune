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
public class SunHaloDragonHeadDance extends SkillHandler<SimpleSkillResult> {

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
            int t1 = 0;
            int t2 = 0;

            @Override
            public boolean shouldCancel() {
                return !caster.isValid() || t1++ > 20;
            }

            @Override
            protected void tick() {
                Vector velocity = UtilityMethod.getForwardVelocity(caster, false);
                caster.setVelocity(velocity.multiply(0.8).setY(caster.getVelocity().getY()));

                for (int i = 0; i < 10; i++) {
                    double x = Math.sin(Math.toRadians(t2)) * 3;
                    double y = 1;
                    double z = 0;

                    Vector rotated = new Vector(x, y, z)
                            .rotateAroundY(Math.toRadians(-caster.getYaw()));

                    Location displayLoc = caster.getLocation().clone().add(rotated);
                    UtilityMethod.attack(meta, displayLoc,
                            damage,
                            radius,
                            knockback,
                            repulsion, true, Element.FIRE,
                            DamageType.MAGIC,
                            DamageType.SKILL
                    );

                    Particle.DUST.builder().location(displayLoc).color(Color.RED, 2.f)
                            .count(2).offset(0.25, 0.25, 0.25).spawn();

                    Particle.FLAME.builder().location(displayLoc)
                            .count(4).offset(0.5, 0.5, 0.5).extra(0.1).spawn();

                    Particle.INSTANT_EFFECT.builder().location(displayLoc)
                            .count(2).offset(0.5, 0.5, 0.5).spawn();

                    t2 += 3;
                }
                if (t1 % 7 == 1) {
                    caster.getWorld().playSound(caster, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.f);
                    caster.getWorld().playSound(caster, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.9f);
                    caster.getWorld().playSound(caster, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.8f);
                }
            }

            @Override
            protected void onStart() {
                caster.getWorld().playSound(caster, Sound.ITEM_TOTEM_USE, 0.5f, 1.f);
                caster.getWorld().playSound(caster, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.f);

                Particle.EXPLOSION.builder().location(caster.getLocation())
                        .count(2).offset(0.5, 0.5, 0.5).extra(2.0).spawn();
            }

            @Override
            protected void onEnd() {

            }
        }.runTask(meta);
    }
}
