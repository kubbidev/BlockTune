package me.kubbidev.blocktune.server.instance.block;

import me.kubbidev.blocktune.server.instance.Instance;
import me.kubbidev.blocktune.server.util.NamespaceID;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface used to provide block behavior. Set with {@link TunedBlock#withHandler(TunedBlockHandler)}.
 * <p>
 * Implementations are expected to be thread safe.
 */
public interface TunedBlockHandler {

    /**
     * Called when a block has been placed.
     *
     * @param placement the placement details
     */
    default void onPlace(@NotNull Placement placement) {
    }

    /**
     * Called when a block has been destroyed or replaced.
     *
     * @param destroy the destroy details
     */
    default void onDestroy(@NotNull Destroy destroy) {
    }

    default void tick(@NotNull Tick tick) {
    }

    default boolean isTickable() {
        return false;
    }

    /**
     * Gets the id of this handler.
     * <p>
     * Used to write the block entity in the anvil world format.
     *
     * @return the namespace id of this handler
     */
    @NotNull
    NamespaceID getNamespaceId();

    /**
     * Represents an object forwarded to {@link #onPlace(Placement)}.
     */
    sealed class Placement permits PlayerPlacement {
        private final TunedBlock block;
        private final Instance instance;
        private final Location blockLocation;

        @ApiStatus.Internal
        public Placement(TunedBlock block, Instance instance, Location blockLocation) {
            this.block = block;
            this.instance = instance;
            this.blockLocation = blockLocation;
        }

        public @NotNull TunedBlock getBlock() {
            return this.block;
        }

        public @NotNull Instance getInstance() {
            return this.instance;
        }

        public @NotNull Location getBlockLocation() {
            return this.blockLocation;
        }
    }

    final class PlayerPlacement extends Placement {
        private final Player player;
        private final EquipmentSlot hand;
        private final BlockFace blockFace;
        private final float cursorX;
        private final float cursorY;
        private final float cursorZ;

        @ApiStatus.Internal
        public PlayerPlacement(TunedBlock block, Instance instance, Location blockLocation,
                               Player player, EquipmentSlot hand, BlockFace blockFace,
                               float cursorX,
                               float cursorY,
                               float cursorZ) {
            super(block, instance, blockLocation);
            this.player = player;
            this.hand = hand;
            this.blockFace = blockFace;
            this.cursorX = cursorX;
            this.cursorY = cursorY;
            this.cursorZ = cursorZ;
        }

        public @NotNull Player getPlayer() {
            return this.player;
        }

        public @NotNull EquipmentSlot getHand() {
            return this.hand;
        }

        public @NotNull BlockFace getBlockFace() {
            return this.blockFace;
        }

        public float getCursorX() {
            return this.cursorX;
        }

        public float getCursorY() {
            return this.cursorY;
        }

        public float getCursorZ() {
            return this.cursorZ;
        }
    }

    sealed class Destroy permits PlayerDestroy {
        private final TunedBlock block;
        private final Instance instance;
        private final Location blockLocation;

        @ApiStatus.Internal
        public Destroy(TunedBlock block, Instance instance, Location blockLocation) {
            this.block = block;
            this.instance = instance;
            this.blockLocation = blockLocation;
        }

        public @NotNull TunedBlock getBlock() {
            return this.block;
        }

        public @NotNull Instance getInstance() {
            return this.instance;
        }

        public @NotNull Location getBlockLocation() {
            return this.blockLocation;
        }
    }

    final class PlayerDestroy extends Destroy {
        private final Player player;

        public PlayerDestroy(TunedBlock block, Instance instance, Location blockLocation, Player player) {
            super(block, instance, blockLocation);
            this.player = player;
        }

        public @NotNull Player getPlayer() {
            return this.player;
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    final class Tick {
        private final TunedBlock block;
        private final Instance instance;
        private final Location blockLocation;

        @ApiStatus.Internal
        public Tick(TunedBlock block, Instance instance, Location blockLocation) {
            this.block = block;
            this.instance = instance;
            this.blockLocation = blockLocation;
        }

        public @NotNull TunedBlock getBlock() {
            return this.block;
        }

        public @NotNull Instance getInstance() {
            return this.instance;
        }

        public @NotNull Location getBlockLocation() {
            return this.blockLocation;
        }
    }

    /**
     * Handler used for loaded blocks with unknown namespace
     * in order to do not lose the information while saving, and for runtime debugging purpose.
     */
    @ApiStatus.Internal
    final class Dummy implements TunedBlockHandler {
        private static final Map<String, TunedBlockHandler> DUMMY_CACHE = new ConcurrentHashMap<>();

        public static @NotNull TunedBlockHandler get(@NotNull String namespace) {
            return DUMMY_CACHE.computeIfAbsent(namespace, Dummy::new);
        }

        private final NamespaceID namespaced;

        public Dummy(String name) {
            this.namespaced = NamespaceID.from(name);
        }

        @Override
        public @NotNull NamespaceID getNamespaceId() {
            return this.namespaced;
        }
    }
}