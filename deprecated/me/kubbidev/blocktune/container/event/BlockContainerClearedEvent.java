package me.kubbidev.blocktune.container.event;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BlockContainerClearedEvent extends BlockContainerEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public BlockContainerClearedEvent(@NotNull Block block, Reason reason, Cancellable parentEvent) {
        super(block, reason, parentEvent);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
