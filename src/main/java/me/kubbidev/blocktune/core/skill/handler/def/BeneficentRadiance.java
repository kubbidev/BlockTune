package me.kubbidev.blocktune.core.skill.handler.def;

import me.kubbidev.blocktune.core.UtilityMethod;
import me.kubbidev.blocktune.core.damage.DamageType;
import me.kubbidev.blocktune.core.damage.Element;
import me.kubbidev.blocktune.core.entity.EntityMetadataProvider;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.skill.handler.SkillHandler;
import me.kubbidev.blocktune.core.skill.result.def.SimpleSkillResult;
import me.kubbidev.blocktune.core.util.EntityBody;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BeneficentRadiance extends SkillHandler<SimpleSkillResult> {

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
            Location velocity = null;
            double t = 0.0;

            @Override
            public void run() {
                if (!caster.isValid() || (t += Math.PI / 2.0) >= Math.PI * 8.0) {
                    if (caster.isValid()) {
                        caster.setVelocity(new Vector());
                    }
                    // remove this handler from casting in the caster metadata map instance
                    EntityMetadataProvider.onCastEnd(caster, BeneficentRadiance.this);
                    cancel();
                    return;
                }
                Location location = EntityBody.BODY.getLocation(caster);
                if (t == Math.PI / 2.0) {
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
                caster.getWorld().playSound(caster, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f);
                caster.setVelocity(this.velocity.toVector());

                // todo should we really want to run attack method here also ??
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
                            DamageType.SKILL
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
        }.runTaskTimer(meta.plugin(), 0, 1);
    }
}
