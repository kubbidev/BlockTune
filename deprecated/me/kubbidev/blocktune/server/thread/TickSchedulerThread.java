package me.kubbidev.blocktune.server.thread;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import me.kubbidev.blocktune.server.ServerProcess;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class TickSchedulerThread implements Listener {
    private final ServerProcess serverProcess;

    public TickSchedulerThread(ServerProcess serverProcess) {
        this.serverProcess = serverProcess;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerTickEnd(ServerTickEndEvent ignored) {
        if (this.serverProcess.isAlive()) {
            long tickStart = System.nanoTime();
            try {
                serverProcess.ticker().tick(tickStart);
            } catch (Exception e) {
                serverProcess.exception().handleException(e);
            }
        }
    }
}