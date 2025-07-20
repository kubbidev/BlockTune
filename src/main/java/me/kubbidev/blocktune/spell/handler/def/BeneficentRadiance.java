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
public class BeneficentRadiance extends SpellHandler<SimpleSpellResult> {

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
            Location velocity = null;
            double   t        = 0.0;

            @Override
            public boolean shouldCancel() {
                return !caster.isValid() || (t += Math.PI / 2.0) >= Math.PI * 8.0;
            }

            @Override
            protected void tick() {
                Location location = EntityBody.BODY.getLocation(caster);
                caster.getWorld().playSound(caster, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f);
                caster.setVelocity(this.velocity.toVector());

                Particle.FLAME.builder().location(location)
                    .count(8).offset(0.2, 0.2, 0.2).extra(0.05).spawn();

                Particle.INSTANT_EFFECT.builder().location(location)
                    .count(4).offset(0.2, 0.2, 0.2).extra(0.05).spawn();

                Particle.DUST.builder().location(location).color(Color.RED, 2.f)
                    .count(2).offset(0.1, 0.1, 0.1).spawn();

                for (double i = 0.0; i < Math.PI / 2.0; i += Math.PI / 24.0) {
                    double x = Math.cos(i + t) * 2;
                    double y = Math.sin(i + t) * 2;

                    Vector rotated = new Vector(x, y, 0)
                        .rotateAroundX(Math.toRadians(velocity.getPitch()))
                        .rotateAroundY(Math.toRadians(-velocity.getYaw()));

                    Location displayLoc = location.clone().add(rotated);
                    UtilityMethod.attack(meta, displayLoc,
                        damage,
                        radius,
                        knockback,
                        repulsion, true, Element.FIRE,
                        DamageType.MAGIC,
                        DamageType.SPELL
                    );

                    Particle.FLAME.builder().location(displayLoc)
                        .count(4).offset(0.2, 0.2, 0.2).extra(0.05).spawn();

                    Particle.INSTANT_EFFECT.builder().location(displayLoc)
                        .count(2).offset(0.2, 0.2, 0.2).extra(0.05).spawn();

                    Particle.DUST.builder().location(displayLoc).color(Color.RED, 2.f)
                        .offset(0.1, 0.1, 0.1).spawn();

                    Particle.DUST.builder().location(displayLoc).color(Color.RED, 2.f).spawn();
                }
            }

            @Override
            protected void onStart() {
                Location location = EntityBody.BODY.getLocation(caster);
                caster.getWorld().playSound(caster, Sound.ITEM_TOTEM_USE, 0.5f, 1.0f);
                caster.getWorld().playSound(caster, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 1.0f);

                if (this.velocity == null) {
                    this.velocity = UtilityMethod.getForwardVelocity(caster, true).multiply(0.8)
                        .toLocation(caster.getWorld(),
                            location.getYaw(),
                            location.getPitch()
                        );
                }
                Particle.FLAME.builder().location(location)
                    .count(20).extra(0.5).spawn();

                Particle.CLOUD.builder().location(location)
                    .count(20).extra(0.5).spawn();
            }

            @Override
            protected void onEnd() {
                if (caster.isValid()) {
                    caster.setVelocity(new Vector());
                }
            }
        }.runTask(meta);
    }
}
