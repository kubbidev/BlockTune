package me.kubbidev.blocktune.server.event.instance;

import me.kubbidev.blocktune.server.event.trait.InstanceEvent;
import me.kubbidev.blocktune.server.instance.Chunk;
import me.kubbidev.blocktune.server.instance.Instance;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a chunk in an instance is loaded.
 */
public class InstanceAsyncChunkLoadEvent extends InstanceEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final Chunk chunk;

    public InstanceAsyncChunkLoadEvent(@NotNull Instance instance, @NotNull Chunk chunk) {
        super(instance, true);
        this.chunk = chunk;
    }

    /**
     * Gets the chunk X.
     *
     * @return the chunk X
     */
    public int getChunkX() {
        return this.chunk.getChunkX();
    }

    /**
     * Gets the chunk Z.
     *
     * @return the chunk Z
     */
    public int getChunkZ() {
        return this.chunk.getChunkZ();
    }

    /**
     * Gets the chunk.
     *
     * @return the chunk.
     */
    public @NotNull Chunk getChunk() {
        return this.chunk;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}