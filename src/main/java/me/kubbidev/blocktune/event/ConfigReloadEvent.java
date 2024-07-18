package me.kubbidev.blocktune.event;

import lombok.Getter;
import me.kubbidev.blocktune.BlockTune;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when the configuration is reloaded
 */
@Getter
public class ConfigReloadEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final BlockTune plugin;

    public ConfigReloadEvent(BlockTune plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
