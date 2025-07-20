package me.kubbidev.blocktune.spell.handler.def;

import me.kubbidev.blocktune.UtilityMethod;
import me.kubbidev.blocktune.spell.handler.SpellRunnable;
import me.kubbidev.spellcaster.damage.DamageType;
import me.kubbidev.spellcaster.element.Element;
import me.kubbidev.spellcaster.spell.SpellMetadata;
import me.kubbidev.spellcaster.spell.handler.SpellHandler;
import me.kubbidev.spellcaster.spell.result.def.SimpleSpellResult;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class FireWheel extends SpellHandler<SimpleSpellResult> {

    @Override
    public SimpleSpellResult getResult(SpellMetadata meta) {
        return new SimpleSpellResult();
    }

    @Override
    public void whenCast(SimpleSpellResult result, SpellMetadata meta) {
        LivingEntity caster = meta.entity();

        double damage = meta.parameter("damage");
        double radius = meta.parameter("radius");

        double knockback = meta.parameter("knockback");
        double repulsion = meta.parameter("repulsion");
        new SpellRunnable() {
            Location location = null;

            double t = 0.0;

            @Override
            public boolean shouldCancel() {
                return !caster.isValid() || ((t += Math.PI / 2.0) > Math.PI * 10.0);
            }

            @Override
            protected void tick() {
                if (t == Math.PI * 4) {

                    caster.swingMainHand();
                    if (location == null) {
                        location = caster.getLocation();
                    }
                    location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.f);
                    location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.f);
                }

                if ((t >= Math.PI * 4) && (t <= Math.PI * 6)) {
                    for (int layer = 0; layer < 4; layer++) {
                        double layerRadius = 3.5 + layer * 0.33;

                        for (double i = 0; i < Math.PI / 2; i += Math.PI / 24) {
                            double y = Math.sin(i + t) * layerRadius + 0.6;
                            double z = Math.cos(i + t) * layerRadius;

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
                                DamageType.SPELL
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

                Particle.FLAME.builder().location(caster.getLocation())
                    .count(4).offset(0.25, 0.25, 0.25).extra(0.0).spawn();
            }

            @Override
            protected void onStart() {
                Vector velocity = UtilityMethod.getForwardVelocity(caster, true);
                caster.setVelocity(velocity.setY(Math.max(caster.getVelocity().getY(), 0.6)));

                Location location = caster.getLocation();
                location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.f);

                Particle.FLAME.builder().location(location)
                    .count(20).extra(0.5).spawn();

                Particle.CLOUD.builder().location(location)
                    .count(20).extra(0.5).spawn();
            }

            @Override
            protected void onEnd() {

            }
        }.runTask(meta);
    }
}
