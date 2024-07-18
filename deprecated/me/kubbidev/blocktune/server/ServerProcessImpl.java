package me.kubbidev.blocktune.server;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.server.event.server.ServerTickMonitorEvent;
import me.kubbidev.blocktune.server.exception.ExceptionManager;
import me.kubbidev.blocktune.server.instance.Chunk;
import me.kubbidev.blocktune.server.instance.Instance;
import me.kubbidev.blocktune.server.instance.InstanceManager;
import me.kubbidev.blocktune.server.instance.block.TunedBlockManager;
import me.kubbidev.blocktune.server.monitoring.TickMonitor;
import me.kubbidev.blocktune.server.thread.ThreadDispatcher;
import me.kubbidev.nexuspowered.Events;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

final class ServerProcessImpl implements ServerProcess {
    private final BlockTune plugin;
    private final ExceptionManager exception;

    private final InstanceManager instance;
    private final TunedBlockManager block;

    private final ThreadDispatcher<Chunk> dispatcher;
    private final Ticker ticker;

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();

    public ServerProcessImpl(BlockTune plugin) throws IOException {
        this.plugin = plugin;
        this.exception = new ExceptionManager();

        // The order of initialization here is relevant.

        this.instance = new InstanceManager(plugin);
        this.block = new TunedBlockManager(plugin);

        this.dispatcher = ThreadDispatcher.singleThread();
        this.ticker = new TickerImpl();
    }

    @Override
    public @NotNull ExceptionManager exception() {
        return this.exception;
    }

    @Override
    public @NotNull InstanceManager instance() {
        return this.instance;
    }

    @Override
    public @NotNull TunedBlockManager block() {
        return this.block;
    }

    @Override
    public @NotNull ThreadDispatcher<Chunk> dispatcher() {
        return this.dispatcher;
    }

    @Override
    public @NotNull Ticker ticker() {
        return this.ticker;
    }

    @Override
    public void start() {
        if (!this.started.compareAndSet(false, true)) {
            throw new IllegalStateException("Server already started");
        }

        this.plugin.getLogger().info("Starting internal server.");
        // ...
        this.plugin.getLogger().info("Server started successfully.");
    }

    @Override
    public void stop() {
        if (!this.stopped.compareAndSet(false, true))
            return;
        this.plugin.getLogger().info("Stopping internal server.");
        // ...
        this.dispatcher.shutdown();
        this.plugin.getLogger().info("Server stopped successfully.");
    }

    @Override
    public boolean isAlive() {
        return this.started.get() && !this.stopped.get();
    }

    private final class TickerImpl implements Ticker {

        @Override
        public void tick(long nanoTime) {
            long msTime = System.currentTimeMillis();

            // server tick (chunks)
            serverTick(msTime);

            // monitoring
            {
                double tickTimeMs = (System.nanoTime() - nanoTime) / 1e6D;
                TickMonitor tickMonitor = new TickMonitor(tickTimeMs);
                Events.call(new ServerTickMonitorEvent(tickMonitor));
            }
        }

        private void serverTick(long tickStart) {
            // tick all instances
            for (Instance instance : instance().getInstances()) {
                try {
                    instance.tick(tickStart);
                } catch (Exception e) {
                    exception().handleException(e);
                }
            }
            // tick all chunks
            dispatcher().updateAndAwait(tickStart);

            // update threads
            long tickTime = System.currentTimeMillis() - tickStart;
            dispatcher().refreshThreads(tickTime);
        }
    }
}