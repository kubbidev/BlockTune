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
public class Dance extends SpellHandler<SimpleSpellResult> {

    private static final double OFFSET = 3.0 * Math.PI / 20.0;

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
                return !caster.isValid() || (t += Math.PI / 4.0) > (5.0 * Math.PI / 4.0);
            }

            @Override
            protected void tick() {
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

            @Override
            protected void onStart() {
                location = caster.getLocation();
                location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.f);
                location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.f);
                caster.swingMainHand();
            }

            @Override
            protected void onEnd() {

            }
        }.runTask(meta);
    }
}
