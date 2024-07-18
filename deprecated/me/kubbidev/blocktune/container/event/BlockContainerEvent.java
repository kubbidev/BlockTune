package me.kubbidev.blocktune.container.event;

import me.kubbidev.blocktune.container.BlockContainer;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.NotNull;

public abstract class BlockContainerEvent extends BlockEvent implements Cancellable {

    private final BlockContainer container;
    private final Reason reason;

    private final Cancellable sourceEvent;

    public BlockContainerEvent(@NotNull Block block, Reason reason, Cancellable sourceEvent) {
        super(block);
        this.container = new BlockContainer(block);
        this.reason = reason;
        this.sourceEvent = sourceEvent;
    }

    public BlockContainer getContainer() {
        return this.container;
    }

    public Reason getReason() {
        return this.reason;
    }

    public Cancellable getSourceEvent() {
        return this.sourceEvent;
    }

    @Override
    public boolean isCancelled() {
        return this.sourceEvent.isCancelled();
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.sourceEvent.setCancelled(cancel);
    }

    public enum Reason {
        BLOCK_BREAK,
        BLOCK_EXPLOSION,
        BLOCK_PLACE,
        BURN,
        ENTITY_CHANGE_BLOCK,
        ENTITY_EXPLOSION,
        FADE,
        FERTILIZE,
        GROW,
        LEAVES_DECAY,
        PISTON_EXTEND,
        PISTON_RETRACT,
        STRUCTURE_GROW
    }
}
