package me.kubbidev.blocktune.server.instance.block.rule;

import me.kubbidev.blocktune.server.instance.block.TunedBlock;
import me.kubbidev.blocktune.server.instance.Instance;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TunedBlockPlacementRule {
    public static final int DEFAULT_UPDATE_RANGE = 10;

    protected final TunedBlock block;

    protected TunedBlockPlacementRule(@NotNull TunedBlock block) {
        this.block = block;
    }

    /**
     * Called when the block state id can be updated (for instance if a neighbour block changed).
     * This is first called on a newly placed block, and then this is called for all neighbors of the block
     *
     * @param updateState The current parameters to the block update
     * @return the updated block
     */
    public @NotNull TunedBlock blockUpdate(@NotNull UpdateState updateState) {
        return updateState.currentBlock();
    }

    /**
     * Called when the block is placed.
     * It is recommended that you only set up basic properties on the block for this placement, such as determining facing, etc
     *
     * @param placementState The current parameters to the block placement
     * @return the block to place, {@code null} to cancel
     */
    public abstract @Nullable TunedBlock blockPlace(@NotNull PlacementState placementState);

    public boolean isSelfReplaceable(@NotNull Replacement replacement) {
        return false;
    }

    public @NotNull TunedBlock getBlock() {
        return this.block;
    }

    /**
     * The max distance where a block update can be triggered. It is not based on block, so if the value is 3 and a completely
     * different block updates 3 blocks away it could still trigger an update.
     */
    public int maxUpdateDistance() {
        return DEFAULT_UPDATE_RANGE;
    }

    public record PlacementState(
            @NotNull Instance instance,
            @NotNull TunedBlock block,
            @Nullable BlockFace blockFace,
            @NotNull Location placeLocation,
            @Nullable Vector cursorLocation,
            @Nullable Location playerLocation,
            @Nullable ItemStack usedItemStack,
            boolean isPlayerShifting
    ) {
    }

    public record UpdateState(@NotNull Instance instance,
                              @NotNull Location blockLocation,
                              @NotNull TunedBlock currentBlock,
                              @NotNull BlockFace fromFace) {
    }

    public record Replacement(
            @NotNull TunedBlock block,
            @NotNull BlockFace blockFace,
            @NotNull Vector cursorLocation,
            @NotNull Material material
    ) {
    }
}