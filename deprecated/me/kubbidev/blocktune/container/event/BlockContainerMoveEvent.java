package me.kubbidev.blocktune.container.event;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BlockContainerMoveEvent extends BlockContainerEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final Block blockTo;

    public BlockContainerMoveEvent(@NotNull Block block, Block blockTo, Reason reason, Cancellable parentEvent) {
        super(block, reason, parentEvent);
        this.blockTo = blockTo;
    }

    public Block getBlockTo() {
        return this.blockTo;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
