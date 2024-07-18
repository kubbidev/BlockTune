package me.kubbidev.blocktune.server.instance.block;

import me.kubbidev.blocktune.server.instance.Instance;
import me.kubbidev.blocktune.server.registry.Registry;
import me.kubbidev.blocktune.server.registry.StaticProtocolObject;
import me.kubbidev.blocktune.server.util.NamespaceID;
import org.bukkit.Location;
import org.jetbrains.annotations.*;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Represents a block that can be placed anywhere.
 * Block objects are expected to be reusable and therefore do not
 * retain placement data (e.g. block location)
 * <p>
 * Implementations are expected to be immutable.
 */
public sealed interface TunedBlock extends StaticProtocolObject, TunedBlocks permits TunedBlockImpl {

    /**
     * Creates a new block with the the property {@code property} sets to {@code value}.
     *
     * @param property the property name
     * @param value    the property value
     * @return a new block with its property changed
     * @throws IllegalArgumentException if the property or value are invalid
     */
    @Contract(pure = true)
    @NotNull
    TunedBlock withProperty(@NotNull String property, @NotNull String value);

    /**
     * Changes multiple properties at once.
     * <p>
     * Equivalent to calling {@link #withProperty(String, String)} for each map entry.
     *
     * @param properties map containing all the properties to change
     * @return a new block with its properties changed
     * @throws IllegalArgumentException if the property or value are invalid
     * @see #withProperty(String, String)
     */
    @Contract(pure = true)
    @NotNull
    TunedBlock withProperties(@NotNull Map<@NotNull String, @NotNull String> properties);

    /**
     * Creates a new block with the specified {@link TunedBlockHandler handler}.
     *
     * @param handler the new block handler, null to remove
     * @return a new block with the specified handler
     */
    @Contract(pure = true)
    @NotNull
    TunedBlock withHandler(@Nullable TunedBlockHandler handler);

    /**
     * Returns the block handler.
     *
     * @return the block handler, null if not present
     */
    @Contract(pure = true)
    @Nullable
    TunedBlockHandler handler();

    /**
     * Returns the block properties.
     *
     * @return the block properties map
     */
    @Unmodifiable
    @Contract(pure = true)
    @NotNull
    Map<String, String> properties();

    /**
     * Returns this block type with default properties, no tags and no handler.
     * As found in the {@link TunedBlocks} listing.
     *
     * @return the default block
     */
    @Contract(pure = true)
    @NotNull
    TunedBlock defaultState();

    /**
     * Returns a property value from {@link #properties()}.
     *
     * @param property the property name
     * @return the property value, null if not present (due to an invalid property name)
     */
    @Contract(pure = true)
    default String getProperty(@NotNull String property) {
        return properties().get(property);
    }

    @Contract(pure = true)
    @ApiStatus.Experimental
    @NotNull
    Collection<@NotNull TunedBlock> possibleStates();

    /**
     * Returns the block registry.
     * <p>
     * Registry data is directly linked to {@link #stateId()}.
     *
     * @return the block registry
     */
    @Contract(pure = true)
    @NotNull
    Registry.BlockEntry registry();

    @Override
    default @NotNull NamespaceID namespace() {
        return registry().namespace();
    }

    @Override
    default int id() {
        return registry().id();
    }

    default int stateId() {
        return registry().stateId();
    }

    default boolean compare(@NotNull TunedBlock block, @NotNull Comparator comparator) {
        return comparator.test(this, block);
    }

    default boolean compare(@NotNull TunedBlock block) {
        return compare(block, Comparator.ID);
    }

    static @NotNull Collection<@NotNull TunedBlock> values() {
        return TunedBlockImpl.values();
    }

    static @Nullable TunedBlock fromNamespaceId(@NotNull String namespace) {
        return TunedBlockImpl.getSafe(namespace);
    }

    static @Nullable TunedBlock fromNamespaceId(@NotNull NamespaceID namespace) {
        return fromNamespaceId(namespace.asString());
    }

    static @Nullable TunedBlock fromStateId(int stateId) {
        return TunedBlockImpl.getState(stateId);
    }

    static @Nullable TunedBlock fromBlockId(int blockId) {
        return TunedBlockImpl.getId(blockId);
    }

    @FunctionalInterface
    interface Comparator extends BiPredicate<TunedBlock, TunedBlock> {
        Comparator IDENTITY = (b1, b2) -> b1 == b2;

        Comparator ID = (b1, b2) -> b1.id() == b2.id();

        Comparator STATE = (b1, b2) -> b1.stateId() == b2.stateId();
    }

    /**
     * Represents an element which can place blocks at location.
     * <p>
     * Notably used by {@link Instance}.
     */
    interface Setter {

        void setBlock(int x, int y, int z, @NotNull TunedBlock block);

        default void setBlock(@NotNull Location blockLocation, @NotNull TunedBlock block) {
            setBlock(blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ(), block);
        }
    }

    interface Getter {
        @Nullable
        TunedBlock getBlock(int x, int y, int z, @NotNull Condition condition);

        default @Nullable TunedBlock getBlock(@NotNull Location location, @NotNull Condition condition) {
            return getBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), condition);
        }

        default @NotNull TunedBlock getBlock(int x, int y, int z) {
            return Objects.requireNonNull(getBlock(x, y, z, Condition.NONE));
        }

        default @NotNull TunedBlock getBlock(@NotNull Location location) {
            return Objects.requireNonNull(getBlock(location, Condition.NONE));
        }

        /**
         * Represents a hint to retrieve blocks more efficiently.
         * Implementing interfaces do not have to honor this.
         */
        @ApiStatus.Experimental
        enum Condition {
            /**
             * Returns a block no matter what.
             * {@link TunedBlock#AIR} being the default result.
             */
            NONE,
            /**
             * Hints that the method should return only if the block is cached.
             * <p>
             * Useful if you are only interested in a block handler or nbt.
             */
            CACHED,
            /**
             * Hints that we only care about the block type.
             * <p>
             * Useful if you need to retrieve registry information about the block.
             * Be aware that the returned block may not return the proper handler/nbt.
             */
            TYPE
        }
    }
}
