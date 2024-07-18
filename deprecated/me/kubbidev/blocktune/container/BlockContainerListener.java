package me.kubbidev.blocktune.container;

import me.kubbidev.blocktune.container.event.BlockContainerEvent;
import me.kubbidev.blocktune.container.event.BlockContainerMoveEvent;
import me.kubbidev.blocktune.container.event.BlockContainerClearedEvent;
import me.kubbidev.nexuspowered.Events;
import me.kubbidev.nexuspowered.Schedulers;
import me.kubbidev.nexuspowered.serialize.BlockPosition;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.*;
import java.util.function.Supplier;

public class BlockContainerListener implements Listener {
    private static final Set<BlockPosition> DIRTY_BLOCKS = new HashSet<>();

    public static boolean isDirty(Block block) {
        return DIRTY_BLOCKS.contains(BlockPosition.of(block));
    }

    public static void setDirty(BlockPosition position) {
        DIRTY_BLOCKS.add(position);
        Schedulers.sync().execute(() -> DIRTY_BLOCKS.remove(position));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        callEventAndRemoveContainer(e, BlockContainerEvent.Reason.BLOCK_BREAK, e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!isDirty(e.getBlock())) {
            callEventAndRemoveContainer(e, BlockContainerEvent.Reason.BLOCK_PLACE, e.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (e.getTo() != e.getBlock().getType()) {
            callEventAndRemoveContainer(e, BlockContainerEvent.Reason.ENTITY_CHANGE_BLOCK, e.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        callEventAndRemoveContainer(e, BlockContainerEvent.Reason.BLOCK_EXPLOSION, e.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        callEventAndRemoveContainer(e, BlockContainerEvent.Reason.ENTITY_EXPLOSION, e.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        callEventAndRemoveContainer(e, BlockContainerEvent.Reason.BURN, e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent e) {
        onBlockPiston(e, BlockContainerEvent.Reason.PISTON_EXTEND, e.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent e) {
        onBlockPiston(e, BlockContainerEvent.Reason.PISTON_RETRACT, e.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent e) {
        if (e.getBlock().getType() == Material.FIRE || e.getNewState().getType() == e.getBlock().getType()) {
            return;
        }
        callEventAndRemoveContainer(e, BlockContainerEvent.Reason.FADE, e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent e) {
        callEventAndRemoveContainerState(e, BlockContainerEvent.Reason.STRUCTURE_GROW, e.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFertilize(BlockFertilizeEvent e) {
        callEventAndRemoveContainerState(e, BlockContainerEvent.Reason.FERTILIZE, e.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFrom(LeavesDecayEvent e) {
        callEventAndRemoveContainer(e, BlockContainerEvent.Reason.LEAVES_DECAY, e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFrom(BlockGrowEvent e) {
        callEventAndRemoveContainer(e, BlockContainerEvent.Reason.GROW, e.getBlock());
    }

    private static <M extends Map<K, V>, K, V> M reverseMap(M map, Supplier<M> factory) {
        List<K> keys = new ArrayList<>(map.keySet());
        Collections.reverse(keys);
        M reversed = factory.get();
        for (K key : keys) {
            reversed.put(key, map.get(key));
        }
        return reversed;
    }

    private void onBlockPiston(BlockPistonEvent e, BlockContainerEvent.Reason reason, List<Block> blocks) {
        LinkedHashMap<Block, BlockContainer> blockToMove = collectPistonBlockToMove(e, reason, blocks);
        LinkedHashMap<Block, BlockContainer> reversedMap = reverseMap(blockToMove, LinkedHashMap::new);

        reversedMap.forEach((block, blockContainer) -> {
            blockContainer.copyTo(block);
            blockContainer.clearContainer();
        });
    }

    private LinkedHashMap<Block, BlockContainer> collectPistonBlockToMove(BlockPistonEvent e, BlockContainerEvent.Reason reason, List<Block> blocks) {
        LinkedHashMap<Block, BlockContainer> blockToMove = new LinkedHashMap<>();
        for (Block block : blocks) {
            if (!BlockContainer.hasBlockContainer(block)) {
                continue;
            }

            BlockContainer blockContainer = new BlockContainer(block);
            if (blockContainer.isEmpty() || blockContainer.isEmpty()) {
                continue;
            }
            Block destinationBlock = block.getRelative(e.getDirection());
            BlockContainerMoveEvent called = new BlockContainerMoveEvent(block, destinationBlock, reason, e);

            if (!Events.callAndReturn(called).isCancelled()) {
                blockToMove.put(destinationBlock, blockContainer);
            }
        }
        return blockToMove;
    }

    private void callEventAndRemoveContainerState(Cancellable e, BlockContainerEvent.Reason reason, List<BlockState> blockStates) {
        blockStates.forEach(state -> callEventAndRemoveContainer(e, reason, state.getBlock()));
    }

    private void callEventAndRemoveContainer(Cancellable e, BlockContainerEvent.Reason reason, List<Block> blocks) {
        blocks.forEach(block -> callEventAndRemoveContainer(e, reason, block));
    }

    private void callEventAndRemoveContainer(Cancellable e, BlockContainerEvent.Reason reason, Block block) {
        if (callEvent(e, reason, block)) {
            new BlockContainer(block).clearContainer();
        }
    }

    private boolean callEvent(Cancellable e, BlockContainerEvent.Reason reason, Block block) {
        if (!BlockContainer.hasBlockContainer(block) || BlockContainer.isBlockProtected(block)) {
            return false;
        }

        BlockContainerClearedEvent called = new BlockContainerClearedEvent(block, reason, e);
        return !Events.callAndReturn(called).isCancelled();
    }
}
