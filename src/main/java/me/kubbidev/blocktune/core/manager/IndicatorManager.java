package me.kubbidev.blocktune.core.manager;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.config.ConfigKeys;
import me.kubbidev.blocktune.core.listener.indicator.type.DamageIndicator;
import me.kubbidev.blocktune.core.listener.indicator.type.RegenerationIndicator;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public final class IndicatorManager {
    private final List<Listener> indicatorsListeners = new ArrayList<>();

    /**
     * Register all indicators listeners and add them to the list.
     */
    public void load(BlockTune plugin) {
        if (plugin.getConfiguration().get(ConfigKeys.INDICATOR_DAMAGE_ENABLED)) {
            try {
                Listener listener = new DamageIndicator(plugin);
                plugin.registerListener(listener);

                this.indicatorsListeners.add(listener);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Could not load damage indicators: " + e.getMessage());
            }
        }
        if (plugin.getConfiguration().get(ConfigKeys.INDICATOR_REGENERATION_ENABLED)) {
            try {
                Listener listener = new RegenerationIndicator(plugin);
                plugin.registerListener(listener);

                this.indicatorsListeners.add(listener);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Could not load regeneration indicators: " + e.getMessage());
            }
        }
    }

    /**
     * Unregister all listeners, remove them from the list and call the
     * {@link IndicatorManager#load(BlockTune)} method.
     */
    public void reload(BlockTune plugin) {
        // unregister listeners
        this.indicatorsListeners.forEach(HandlerList::unregisterAll);
        this.indicatorsListeners.clear();

        // register listeners
        load(plugin);
    }
}