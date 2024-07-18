package me.kubbidev.blocktune.server.event.server;

import me.kubbidev.blocktune.server.monitoring.TickMonitor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class ServerTickMonitorEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final TickMonitor tickMonitor;

    public ServerTickMonitorEvent(@NotNull TickMonitor tickMonitor) {
        this.tickMonitor = tickMonitor;
    }

    public @NotNull TickMonitor getTickMonitor() {
        return this.tickMonitor;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}