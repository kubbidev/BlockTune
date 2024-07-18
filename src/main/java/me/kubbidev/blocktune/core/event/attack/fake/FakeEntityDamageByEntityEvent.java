package me.kubbidev.blocktune.core.event.attack.fake;

import com.google.common.base.Function;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("removal")
public abstract class FakeEntityDamageByEntityEvent extends EntityDamageByEntityEvent {

    public FakeEntityDamageByEntityEvent(@NotNull Entity damager, @NotNull Entity victim, DamageCause cause, double damage) {
        super(damager, victim, cause, damage);
    }

    @SuppressWarnings("deprecation")
    public FakeEntityDamageByEntityEvent(@NotNull Entity damager, @NotNull Entity victim, DamageCause cause, @NotNull Map<DamageModifier, Double> modifiers, @NotNull Map<DamageModifier, ? extends Function<? super Double, Double>> modifierFunctions) {
        super(damager, victim, cause, modifiers, modifierFunctions);
    }
}