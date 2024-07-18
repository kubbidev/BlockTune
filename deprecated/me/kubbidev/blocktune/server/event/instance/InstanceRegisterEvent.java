package me.kubbidev.blocktune.server.event.instance;

import me.kubbidev.blocktune.server.event.trait.InstanceEvent;
import me.kubbidev.blocktune.server.instance.Instance;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when an instance is registered
 */
public class InstanceRegisterEvent extends InstanceEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public InstanceRegisterEvent(@NotNull Instance instance) {
        super(instance);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}