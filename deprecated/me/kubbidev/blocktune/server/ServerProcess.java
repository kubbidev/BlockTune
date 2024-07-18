package me.kubbidev.blocktune.server;

import me.kubbidev.blocktune.server.exception.ExceptionManager;
import me.kubbidev.blocktune.server.instance.InstanceManager;
import me.kubbidev.blocktune.server.instance.Chunk;
import me.kubbidev.blocktune.server.instance.block.TunedBlockHandler;
import me.kubbidev.blocktune.server.instance.block.TunedBlockManager;
import me.kubbidev.blocktune.server.registry.Registries;
import me.kubbidev.blocktune.server.thread.ThreadDispatcher;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface ServerProcess extends Registries {

    /**
     * Handles registered instances.
     */
    @NotNull
    InstanceManager instance();

    /**
     * Handles {@link TunedBlockHandler block handlers}
     * and {@link me.kubbidev.blocktune.server.instance.block.rule.TunedBlockPlacementRule placement rules}.
     */
    @NotNull
    TunedBlockManager block();

    /**
     * Handles all thrown exceptions from the server.
     */
    @NotNull
    ExceptionManager exception();

    /**
     * Dispatcher for tickable game objects.
     */
    @NotNull
    ThreadDispatcher<Chunk> dispatcher();

    /**
     * Handles the server ticks.
     */
    @NotNull
    Ticker ticker();

    void start();

    void stop();

    boolean isAlive();

    @ApiStatus.NonExtendable
    interface Ticker {
        void tick(long nanoTime);
    }
}
