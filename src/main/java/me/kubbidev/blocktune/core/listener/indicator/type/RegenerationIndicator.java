package me.kubbidev.blocktune.core.listener.indicator.type;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.event.indicator.IndicatorDisplayEvent;
import me.kubbidev.blocktune.core.listener.indicator.GameIndicator;
import me.kubbidev.blocktune.core.UtilityMethod;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.util.Vector;

public class RegenerationIndicator extends GameIndicator {
    public RegenerationIndicator(BlockTune plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof LivingEntity entity) || e.getAmount() <= 0) {
            return;
        }

        // no indicator around vanished entities
        if (UtilityMethod.isVanished(entity)) {
            return;
        }

        AttributeInstance instance = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (instance == null || entity.getHealth() >= instance.getValue()) {
            return;
        }
        Component message = Component.text()
                .append(Component.text('+'))
                .append(Component.text(this.plugin.getConfiguration().getDecimalFormat().format(e.getAmount())))
                .color(NamedTextColor.GREEN)
                .build();

        displayIndicator(entity, message, getIndicatorDirection(entity), IndicatorDisplayEvent.IndicatorType.REGENERATION);
    }

    private Vector getIndicatorDirection(Entity entity) {
        if (entity instanceof LivingEntity) {
            double angle = Math.toRadians(((LivingEntity) entity).getEyeLocation().getYaw()) + Math.PI * (1 + (random.nextDouble() - 0.5) / 2);
            return new Vector(
                    Math.cos(angle), 0,
                    Math.sin(angle)
            );
        }
        double angle = random.nextDouble() * Math.PI * 2;
        return new Vector(
                Math.cos(angle), 0,
                Math.sin(angle)
        );
    }
}