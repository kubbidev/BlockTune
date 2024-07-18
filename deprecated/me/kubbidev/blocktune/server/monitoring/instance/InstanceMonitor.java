package me.kubbidev.blocktune.server.monitoring.instance;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.server.MinecraftServer;
import me.kubbidev.blocktune.server.event.instance.InstanceTickEvent;
import me.kubbidev.blocktune.server.instance.Chunk;
import me.kubbidev.blocktune.server.instance.Instance;
import me.kubbidev.blocktune.server.instance.InstanceManager;
import me.kubbidev.blocktune.server.thread.ThreadDispatcher;
import me.kubbidev.nexuspowered.Events;
import me.kubbidev.nexuspowered.Schedulers;
import me.kubbidev.nexuspowered.util.CompletableFutures;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class InstanceMonitor {
    public static final InstanceMonitor INSTANCE = new InstanceMonitor();

    private static final long AUTOSAVE_TIMING = 6000L;

    // cache use to keep track of loaded and unloaded chunks on the server to dispatch events correctly
    // (please please please improve or find a new way to enhance this shit)
    private final Map<UUID, ChunkWatcher> monitoredInstances = new ConcurrentHashMap<>();
    // the collector responsible of collecting chunk directly from nms internals
    private final InstanceChunkCollector collector = new InstanceChunkCollector(this);

    private InstanceMonitor() {
    }

    @SuppressWarnings("resource")
    public void start(@NotNull BlockTune plugin) {
        Events.subscribe(WorldLoadEvent.class).handler(this::onWorldLoad)
                .bindWith(plugin);

        Events.subscribe(WorldUnloadEvent.class).handler(this::onWorldUnload)
                .bindWith(plugin);

        Events.subscribe(InstanceTickEvent.class).handler(this::onInstanceTick)
                .bindWith(plugin);

        // run the saving task on its own thread.
        Schedulers.async().runRepeating(task -> saveInstances(plugin, task.getTimesRan()),
                AUTOSAVE_TIMING,
                AUTOSAVE_TIMING
        ).bindWith(plugin);
    }

    private static void saveInstances(@NotNull BlockTune plugin, int savesRan) {
        long startTime = System.currentTimeMillis();
        plugin.getLogger().info("(Saving #%s) -> Starting saving process.".formatted(savesRan));

        // A set of futures, which are really just the processes we need to wait for.
        Set<CompletableFuture<Void>> futures = new HashSet<>();

        int total = 0;
        AtomicInteger processedCount = new AtomicInteger(0);

        for (Instance instance : MinecraftServer.getInstanceManager().getInstances()) {
            futures.add(instance.saveChunksToStorage().thenAcceptAsync(e -> processedCount.incrementAndGet()));
            total++;
        }

        // all of the threads have been scheduled now and are running. we just need to wait for them all to complete
        CompletableFuture<Void> overallFuture = CompletableFutures.allOf(futures);

        while (true) {
            try {
                overallFuture.get(2, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                // abnormal error - just break
                e.printStackTrace();
                break;
            } catch (TimeoutException e) {
                // still executing - send a progress report and continue waiting
                int process = processedCount.get();
                int percent = process * 100 / total;
                plugin.getLogger().info("(Saving #%s) -> %s%% complete - %s/%s operations complete."
                        .formatted(savesRan, percent, process, total));
                continue;
            }

            // process is complete
            break;
        }

        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0;

        plugin.getLogger().info("(Saving #%s) COMPLETED - took %s seconds.".formatted(savesRan, seconds));
    }

    private void onWorldLoad(@NotNull WorldLoadEvent e) {
        MinecraftServer.getInstanceManager().createInstanceContainer(e.getWorld());
    }

    private void onWorldUnload(@NotNull WorldUnloadEvent e) {
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        Instance instance = instanceManager.getInstance(e.getWorld().getUID());
        if (instance == null) {
            return;
        }
        unregisterMonitor(instance);
        instance.saveChunksToStorage().thenAccept(u -> instanceManager.unregisterInstance(instance));
    }

    private void onInstanceTick(@NotNull InstanceTickEvent e) {
        Instance instance = e.getInstance();

        ChunkWatcher chunkWatcher = getMonitorOrComputeIfAbsent(instance);
        chunkWatcher.checkForChangesInInstance();
    }

    void onChunkLoad(@NotNull Instance instance, int x, int z) {
        instance.loadOptionalChunk(x, z).thenAccept(chunk -> {
            if (chunk == null) return;

            ThreadDispatcher<Chunk> dispatcher = MinecraftServer.process().dispatcher();
            dispatcher.createPartition(chunk);
        });
    }

    void onChunkUnload(@NotNull Instance instance, int x, int z) {
        Chunk chunk = instance.getChunk(x, z);
        Objects.requireNonNull(chunk, "The chunk at " + x + ":" + z + " is unloaded");

        ThreadDispatcher<Chunk> dispatcher = MinecraftServer.process().dispatcher();
        dispatcher.deletePartition(chunk);
    }

    private void unregisterMonitor(@NotNull Instance instance) {
        this.monitoredInstances.remove(instance.getIdentifier());
    }

    @NotNull
    private InstanceMonitor.ChunkWatcher getMonitorOrComputeIfAbsent(@NotNull Instance instance) {
        return this.monitoredInstances.computeIfAbsent(instance.getIdentifier(), uuid -> {
            // compute and return the value if the instance's chunk watcher cannot be found
            ChunkWatcher monitor = new ChunkWatcher(instance);
            // and finally before returning the value that will be compute, add a listener
            // that will be notify on chunk "loading" and "unloading"
            monitor.addChunkListener(this.collector);
            return monitor;
        });
    }

    interface ChunkListener {
        void onChunkLoaded(Object nmsChunk, @NotNull Instance instance);

        void onChunkUnloaded(Object nmsChunk, @NotNull Instance instance);
    }

    private class ChunkWatcher {
        private final List<ChunkListener> listeners = new ArrayList<>();

        private final Instance instance;
        private final Set<Object> previousChunks = new HashSet<>();

        public ChunkWatcher(@NotNull Instance instance) {
            this.instance = instance;
        }

        public void addChunkListener(@NotNull ChunkListener listener) {
            this.listeners.add(listener);
        }

        public void checkForChangesInInstance() {
            Set<Object> currentLoadedChunks = new HashSet<>(InstanceMonitor.this.collector.collectChunks(this.instance));

            // check for added elements
            for (Object nmsChunk : currentLoadedChunks) {
                if (!this.previousChunks.contains(nmsChunk)) {
                    notifyChunkAdded(nmsChunk);
                }
            }

            // check for removed elements
            for (Object nmsChunk : this.previousChunks) {
                if (!currentLoadedChunks.contains(nmsChunk)) {
                    notifyChunkRemoved(nmsChunk);
                }
            }

            this.previousChunks.clear();
            this.previousChunks.addAll(currentLoadedChunks);
        }

        private void notifyChunkAdded(Object nmsChunk) {
            for (ChunkListener listener : this.listeners) {
                listener.onChunkLoaded(nmsChunk, this.instance);
            }
        }

        private void notifyChunkRemoved(Object nmsChunk) {
            for (ChunkListener listener : this.listeners) {
                listener.onChunkUnloaded(nmsChunk, this.instance);
            }
        }
    }
}
