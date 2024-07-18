package me.kubbidev.blocktune.core.stat;

import me.kubbidev.blocktune.core.entity.modfier.ModifierType;
import me.kubbidev.blocktune.core.util.EquipmentSlot;
import me.kubbidev.nexuspowered.terminable.Terminable;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @param <T> The modifiers instance stored
 * @see InstanceModifier
 */
public abstract class ModifiedInstance<T extends InstanceModifier> {
    protected final Map<UUID, T> modifiers = new ConcurrentHashMap<>();

    /**
     * @param base The base value without modifiers.
     * @return The final modified value taking, into account the default value
     * as well as all of the modifiers.
     * <p>
     * relative-based modifiers are applied afterwards, onto the sum of the base value + flat modifiers.
     */
    public double getTotal(double base) {
        return getFilteredTotal(base, EquipmentSlot.MAIN_HAND::isCompatible, mod -> mod);
    }

    /**
     * @param base   The base value without modifiers.
     * @param filter Filters modifiers taken into account for the final value computation.
     * @return The final modified value taking, into account the default value
     * as well as all of the modifiers.
     * <p>
     * relative-based modifiers are applied afterwards, onto the sum of the base value + flat modifiers.
     */
    public double getFilteredTotal(double base, Predicate<T> filter) {
        return getFilteredTotal(base, filter, mod -> mod);
    }

    /**
     * @param base         The base value without modifiers.
     * @param modification A modification to any modifier before taking it into account
     *                     in the final calculation.
     *                     <p>
     *                     This can be used for instance to reduce debuffs, by checking if
     *                     a stat modifier has a negative value and returning a modifier with a reduced absolute value.
     * @return The final modified value taking, into account the default value
     * as well as all of the modifiers.
     * <p>
     * relative-based modifiers are applied afterwards, onto the sum of the base value + flat modifiers.
     */
    public double getTotal(double base, Function<T, T> modification) {
        return getFilteredTotal(base, EquipmentSlot.MAIN_HAND::isCompatible, modification);
    }

    /**
     * @param base         The base value without modifiers.
     * @param filter       Filters modifiers taken into account for the final value computation.
     * @param modification A modification to any modifier before taking it into account
     *                     in the final calculation.
     *                     <p>
     *                     This can be used for instance to reduce debuffs, by checking if
     *                     a stat modifier has a negative value and returning a modifier with a reduced absolute value.
     * @return The final modified value taking, into account the default value
     * as well as all of the modifiers.
     * <p>
     * relative-based modifiers are applied afterwards, onto the sum of the base value + flat modifiers.
     */
    public double getFilteredTotal(double base, Predicate<T> filter, Function<T, T> modification) {
        for (T mod : this.modifiers.values()) {
            if (mod.getType() == ModifierType.FLAT && filter.test(mod))
                base += modification.apply(mod).getValue();
        }

        for (T mod : this.modifiers.values()) {
            if (mod.getType() == ModifierType.RELATIVE && filter.test(mod))
                base *= 1 + modification.apply(mod).getValue() / 100.0;
        }
        return base;
    }

    /**
     * @param uniqueId The uuid of the desired modifier.
     * @return The modifier with given uuid, or <code>null</code> if not found
     */
    public @Nullable T getModifier(UUID uniqueId) {
        return this.modifiers.get(uniqueId);
    }

    public void registerModifier(T modifier) {
        this.modifiers.put(modifier.getUniqueId(), modifier);
    }

    public void removeModifier(UUID uniqueId) {
        this.modifiers.remove(uniqueId);
    }

    public boolean isEmpty() {
        return this.modifiers.isEmpty();
    }

    /**
     * Iterates through registered modifiers and unregisters them if a
     * certain condition based on their string key is met.
     *
     * @param condition Condition on the modifier key
     */
    public void removeIf(Predicate<String> condition) {
        for (Iterator<T> iterator = modifiers.values().iterator(); iterator.hasNext(); ) {
            T modifier = iterator.next();

            if (condition.test(modifier.getKey())) {
                if (modifier instanceof Terminable) ((Terminable) modifier).closeAndReportException();
                iterator.remove();
            }
        }
    }

    /**
     * @return All registered modifiers
     */
    public Collection<T> getModifiers() {
        return this.modifiers.values();
    }

    public Set<UUID> getIds() {
        return this.modifiers.keySet();
    }

    /**
     * @param uniqueId The uuid of the external modifier source or plugin.
     * @return True if a modifier is registered with this key
     */
    public boolean contains(UUID uniqueId) {
        return this.modifiers.containsKey(uniqueId);
    }
}