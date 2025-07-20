package me.kubbidev.blocktune.spell.handler.def;

import me.kubbidev.blocktune.UtilityMethod;
import me.kubbidev.blocktune.spell.handler.SpellRunnable;
import me.kubbidev.spellcaster.damage.DamageType;
import me.kubbidev.spellcaster.element.Element;
import me.kubbidev.spellcaster.spell.SpellMetadata;
import me.kubbidev.spellcaster.spell.handler.SpellHandler;
import me.kubbidev.spellcaster.spell.result.def.SimpleSpellResult;
import me.kubbidev.spellcaster.util.EntityBody;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class SunflowerThrust extends SpellHandler<SimpleSpellResult> {

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

        Vector towardDirection = caster.getEyeLocation().getDirection().multiply(2.5);
        new SpellRunnable() {
            Location location = null;
            double   t        = 0.0;

            @Override
            public boolean shouldCancel() {
                return !caster.isValid() || (t += Math.PI / 2) > Math.PI * 4;
            }

            @Override
            protected void tick() {
                Vector currentVelocity = caster.getVelocity();
                Vector reducedVelocity = currentVelocity.clone().multiply(0.2);
                caster.setVelocity(new Vector(reducedVelocity.getX(), currentVelocity.getY(), reducedVelocity.getZ()));

                if (t > Math.PI * 2 && t <= Math.PI * 4) {
                    if (t == ((5 * Math.PI) / 2)) {
                        caster.swingMainHand();
                        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.f);
                        location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.f);
                    }

                    for (double i = 0.0; i < Math.PI / 2; i += Math.PI / 18.0) {
                        double x = Math.cos(i + t) * 3.0;
                        double y = Math.sin(i + t) * 3.0;

                        Vector rotated = new Vector(x, y, 0)
                            .rotateAroundX(Math.toRadians(location.getPitch()))
                            .rotateAroundY(Math.toRadians(-location.getYaw()));

                        Location displayLoc = location.clone().add(towardDirection).subtract(rotated);
                        Particle.DUST.builder().location(displayLoc).color(Color.RED, 2.f)
                            .count(2).offset(0.2, 0.2, 0.2).spawn();

                        Particle.FLAME.builder().location(displayLoc)
                            .count(10).offset(0.2, 0.2, 0.2).extra(0.1).spawn();

                        Particle.INSTANT_EFFECT.builder().location(displayLoc)
                            .count(4).offset(0.2, 0.2, 0.2).spawn();
                    }
                }

                if (t == ((7 * Math.PI) / 2)) {
                    Location defaultLocation = location.clone().add(towardDirection);
                    // the vector use every time to increment and finally trace a line in space
                    Vector increment = location.clone().getDirection().normalize();

                    for (int i = 0; i < 7; i++) {
                        defaultLocation.add(increment);
                        UtilityMethod.attack(meta, defaultLocation,
                            damage,
                            radius,
                            knockback,
                            repulsion, false, Element.FIRE,
                            DamageType.MAGIC,
                            DamageType.SPELL
                        );

                        Particle.FLAME.builder().location(defaultLocation)
                            .count(20).offset(0.2, 0.2, 0.2).extra(0.0).spawn();

                        Particle.INSTANT_EFFECT.builder().location(defaultLocation)
                            .count(5).offset(0.2, 0.2, 0.2).spawn();
                    }
                }
            }

            @Override
            protected void onStart() {
                location = EntityBody.BODY.getLocation(caster);
                location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.f);

                Location displayLoc = location.clone().add(towardDirection);
                Particle.FLAME.builder().location(displayLoc)
                    .count(20).offset(0.2, 0.2, 0.2).extra(0.0).spawn();

                Particle.INSTANT_EFFECT.builder().location(displayLoc)
                    .count(5).offset(0.2, 0.2, 0.2).spawn();
            }

            @Override
            protected void onEnd() {

            }
        }.runTask(meta);
    }
}
