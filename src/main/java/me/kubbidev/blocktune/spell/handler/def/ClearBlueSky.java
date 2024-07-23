package me.kubbidev.blocktune.spell.handler.def;

import me.kubbidev.blocktune.UtilityMethod;
import me.kubbidev.blocktune.spell.handler.SpellRunnable;
import me.kubbidev.spellcaster.damage.DamageType;
import me.kubbidev.spellcaster.damage.Element;
import me.kubbidev.spellcaster.spell.SpellMetadata;
import me.kubbidev.spellcaster.spell.handler.SpellHandler;
import me.kubbidev.spellcaster.spell.result.def.SimpleSpellResult;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class ClearBlueSky extends SpellHandler<SimpleSpellResult> {
    private static final double X_AXIS_ROTATION = -22.5;

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
            int t1 = 0;
            int t2 = 0;

            double theta = 0.0;

            @Override
            public boolean shouldCancel() {
                return !caster.isValid() || ((t1++ >= 40 && t2 == 0) || theta >= Math.PI * 2);
            }

            @Override
            protected void tick() {
                if (t2 == 0 && (t1 > 20 || caster.getVelocity().getY() <= 0.2)) {
                    t2 = 1;

                    if (location == null) {
                        location = caster.getLocation();
                    }
                    location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.f);
                    location.getWorld().playSound(location, "minecraft:custom.generic.sword_sweep", 0.2f, 1.f);

                    for (int layer = 0; layer < 3; layer++) {
                        double layerRadius = 2.5 + layer * 0.33;

                        for (double i = 0; i <= Math.PI * 2; i += Math.PI / 18) {
                            double x = Math.cos(i) * layerRadius;
                            double y = Math.sin(i) * layerRadius;

                            Vector rotated = new Vector(x, y, 0.0)
                                    .rotateAroundX(Math.toRadians(X_AXIS_ROTATION))
                                    .rotateAroundY(Math.toRadians(-location.getYaw()));

                            Location displayLoc = location.clone().add(rotated);

                            Particle.FLAME.builder().location(displayLoc)
                                    .count(2).offset(0.1, 0.1, 0.1).extra(0).spawn();

                            Particle.INSTANT_EFFECT.builder().location(displayLoc)
                                    .count(2).offset(0.1, 0.1, 0.1).spawn();

                            Particle.SWEEP_ATTACK.builder().location(displayLoc).spawn();
                            if (layer == 2) {
                                Particle.FLASH.builder().location(displayLoc).spawn();
                            }
                        }
                    }
                }

                if (t2 > 0 && ++t2 > 8 && (theta += Math.PI / 2) <= Math.PI * 2) {
                    Vector currentVelocity = caster.getVelocity();
                    caster.setVelocity(new Vector(currentVelocity.getX(), Math.max(currentVelocity.getY(), 0.0), currentVelocity.getZ()));

                    if (theta == Math.PI / 2) {
                        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.f);
                        location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.f);
                        caster.swingMainHand();
                    }

                    for (int layer = 0; layer < 4; layer++) {
                        double layerRadius = 6.0 + layer * 0.66;

                        for (double i = 0; i <= Math.PI / 2; i += Math.PI / 36) {
                            double x = Math.cos(i + this.theta) * layerRadius;
                            double y = Math.sin(i + this.theta) * layerRadius;

                            Vector rotated = new Vector(x, y, 0.0)
                                    .rotateAroundX(Math.toRadians(X_AXIS_ROTATION))
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

                            if (layer == 1 || layer == 2) {
                                Particle.FLAME.builder().location(displayLoc)
                                        .count(4).offset(0.2, 0.2, 0.2).extra(0.03).force(true).spawn();
                            }

                            if (layer == 0 || layer == 3) {
                                Particle.DUST.builder().location(displayLoc).color(Color.RED, 2.f)
                                        .offset(0.1, 0.1, 0.1).spawn();
                            }
                        }

                    }
                }
            }

            @Override
            protected void onStart() {
                if (caster.getVelocity().getY() >= -0.5) {
                    caster.setVelocity(caster.getVelocity().setY(1.2));
                }
            }

            @Override
            protected void onEnd() {

            }
        }.runTask(meta);
    }
}
