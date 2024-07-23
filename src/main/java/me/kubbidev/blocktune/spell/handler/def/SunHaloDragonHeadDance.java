package me.kubbidev.blocktune.spell.handler.def;

import me.kubbidev.blocktune.UtilityMethod;
import me.kubbidev.blocktune.spell.handler.SpellRunnable;
import me.kubbidev.spellcaster.damage.DamageType;
import me.kubbidev.spellcaster.damage.Element;
import me.kubbidev.spellcaster.spell.SpellMetadata;
import me.kubbidev.spellcaster.spell.handler.SpellHandler;
import me.kubbidev.spellcaster.spell.result.def.SimpleSpellResult;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class SunHaloDragonHeadDance extends SpellHandler<SimpleSpellResult> {

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
                            DamageType.SPELL
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
