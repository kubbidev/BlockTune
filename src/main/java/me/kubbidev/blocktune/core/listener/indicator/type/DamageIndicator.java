package me.kubbidev.blocktune.core.listener.indicator.type;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.damage.DamageMetadata;
import me.kubbidev.blocktune.core.damage.DamagePacket;
import me.kubbidev.blocktune.core.damage.DamageType;
import me.kubbidev.blocktune.core.damage.Element;
import me.kubbidev.blocktune.core.event.attack.AttackUnregisteredEvent;
import me.kubbidev.blocktune.core.event.indicator.IndicatorDisplayEvent;
import me.kubbidev.blocktune.core.listener.indicator.GameIndicator;
import me.kubbidev.blocktune.core.UtilityMethod;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamageIndicator extends GameIndicator {

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private static final Component SKILL_ICON = Component.text("\u2605", NamedTextColor.GOLD);
    private static final Component WEAPON_ICON = Component.text("\uD83D\uDDE1", NamedTextColor.RED);

    public DamageIndicator(BlockTune plugin) {
        super(plugin);
    }

    @EventHandler
    public void displayIndicators(AttackUnregisteredEvent e) {
        if (e.getMetadata().getDamage() <= DamageMetadata.MINIMAL_DAMAGE) {
            return;
        }

        // no indicator around vanished entities
        if (UtilityMethod.isVanished(e.getEntity())) {
            return;
        }

        DamageMetadata metadata = e.getMetadata();
        List<Component> holograms = new ArrayList<>();

        Map<IndicatorType, Double> mappedDamage = new HashMap<>();
        for (DamagePacket packet : metadata.getPackets()) {

            IndicatorType type = new IndicatorType(metadata, packet);
            mappedDamage.put(type, mappedDamage.getOrDefault(type, 0.0) + packet.getFinalValue());
        }

        double modifier = (e.toBukkit().getFinalDamage() - metadata.getDamage()) / Math.max(1, mappedDamage.size());
        for (Map.Entry<IndicatorType, Double> entry : mappedDamage.entrySet()) {
            holograms.add(entry.getKey().getIndicator(entry.getValue() + modifier));
        }

        for (Component hologram : holograms) {
            displayIndicator(e.getEntity(), hologram, getDirection(e.toBukkit()), IndicatorDisplayEvent.IndicatorType.DAMAGE);
        }
    }

    private Vector getDirection(EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent) {
            Vector direction = getDirectionToEntity(e.getEntity(), ((EntityDamageByEntityEvent) e).getDamager());

            if (direction.lengthSquared() > 0) {
                double angle = Math.atan2(direction.getZ(), direction.getX()) + Math.PI / 2 * (random.nextDouble() - 0.5);
                return new Vector(
                        Math.cos(angle), 0,
                        Math.sin(angle)
                );
            }
        }
        double angle = random.nextDouble() * Math.PI * 2;
        return new Vector(
                Math.cos(angle), 0,
                Math.sin(angle)
        );
    }

    private Vector getDirectionToEntity(Entity e1, Entity e2) {
        return e1.getLocation().toVector().subtract(e2.getLocation().toVector()).setY(0);
    }

    private class IndicatorType {
        private final boolean physical;
        private final boolean crit;

        /**
         * The element present inside the damage packet or null.
         */
        @Nullable
        private final Element element;

        /**
         * Constructs an IndicatorType instance.
         *
         * @param metadata The damage metadata.
         * @param packet   The damage packet.
         */
        IndicatorType(DamageMetadata metadata, DamagePacket packet) {
            this.physical = packet.hasType(DamageType.PHYSICAL);
            this.element = packet.getElement();

            this.crit = (this.physical ? metadata.isWeaponCrit() : metadata.isSkillCrit())
                    || (this.element != null && metadata.isElementCrit(this.element));
        }

        private Component getIcon() {
            TextComponent.Builder builder = Component.text().append(this.physical
                    ? WEAPON_ICON.decoration(TextDecoration.BOLD, this.crit)
                    : SKILL_ICON.decoration(TextDecoration.BOLD, this.crit));

            if (this.element != null) {
                builder.append(this.element.getIcon());
            }
            return builder.build();
        }

        private Component getIndicator(double damage) {
            String formattedDamage = DamageIndicator.super.plugin.getConfiguration().getDecimalFormat().format(damage);
            return Component.text()
                    .append(getIcon())
                    .append(Component.space())
                    .append(Component.text(formattedDamage, NamedTextColor.WHITE))
                    .build();
        }
    }
}