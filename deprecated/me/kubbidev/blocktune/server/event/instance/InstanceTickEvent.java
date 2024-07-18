package me.kubbidev.blocktune.server.event.instance;

import me.kubbidev.blocktune.server.event.trait.InstanceEvent;
import me.kubbidev.blocktune.server.instance.Instance;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when an instance processes a tick.
 */
public class InstanceTickEvent extends InstanceEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final int duration;

    public InstanceTickEvent(@NotNull Instance instance, long time, long lastTickAge) {
        super(instance);
        this.duration = (int) (time - lastTickAge);
    }

    /**
     * Gets the duration of the tick in ms.
     *
     * @return the duration
     */
    public int getDuration() {
        return this.duration;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}