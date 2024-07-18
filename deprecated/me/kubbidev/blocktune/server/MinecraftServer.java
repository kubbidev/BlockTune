package me.kubbidev.blocktune.server;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.server.exception.ExceptionManager;
import me.kubbidev.blocktune.server.instance.InstanceManager;
import me.kubbidev.blocktune.server.instance.block.TunedBlockManager;
import me.kubbidev.blocktune.server.monitoring.instance.InstanceMonitor;
import me.kubbidev.blocktune.server.thread.TickSchedulerThread;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;

/**
 * The main server class used to start the server and retrieve all the managers.
 * <p>
 * The server needs to be initialized with {@link #init(BlockTune)} and started with {@link #start(BlockTune)}.
 * You should register all of your dimensions, biomes, commands, events, etc... in-between.
 */
public final class MinecraftServer implements MinecraftConstants {
    // Threads
    public static final String THREAD_NAME_TICK = "Bt-Tick";

    // in-Game Manager
    private static volatile ServerProcess serverProcess;

    public static MinecraftServer init(BlockTune plugin) {
        updateProcess(plugin);
        return new MinecraftServer();
    }

    @ApiStatus.Internal
    public static ServerProcess updateProcess(BlockTune plugin) {
        ServerProcess process;
        try {
            process = new ServerProcessImpl(plugin);
            serverProcess = process;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return process;
    }

    @ApiStatus.Experimental
    public static @UnknownNullability ServerProcess process() {
        return serverProcess;
    }

    public static @NotNull InstanceManager getInstanceManager() {
        return serverProcess.instance();
    }

    public static @NotNull TunedBlockManager getBlockManager() {
        return serverProcess.block();
    }

    public static @NotNull ExceptionManager getExceptionManager() {
        return serverProcess.exception();
    }

    public static boolean isStarted() {
        return serverProcess.isAlive();
    }

    public static boolean isStopping() {
        return !isStarted();
    }

    /**
     * Starts the server.
     * <p>
     * It should be called after {@link #init(BlockTune)} and probably your own initialization code.
     *
     * @throws IllegalStateException if called before {@link #init(BlockTune)} or if the server is already running
     */
    public void start(BlockTune plugin) {
        serverProcess.start();
        plugin.registerListener(new TickSchedulerThread(serverProcess));
        // register the custom chunk monitor system as well to enable the server correctly,
        // this feature is essential cause it makes chunk load and unload dynamically
        InstanceMonitor.INSTANCE.start(plugin);
    }

    /**
     * Stops this server properly (saves if needed, etc.)
     */
    public static void stopCleanly() {
        serverProcess.stop();
    }
}