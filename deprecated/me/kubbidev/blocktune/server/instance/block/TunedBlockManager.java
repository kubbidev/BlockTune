package me.kubbidev.blocktune.server.instance.block;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.server.instance.block.rule.TunedBlockPlacementRule;
import me.kubbidev.blocktune.server.util.NamespaceID;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class TunedBlockManager {
    private final BlockTune plugin;

    // namespace -> handler supplier
    private final Map<String, Supplier<TunedBlockHandler>> blockHandlerMap = new ConcurrentHashMap<>();
    // block id -> block placement rule
    private final Int2ObjectMap<TunedBlockPlacementRule> placementRuleMap = new Int2ObjectOpenHashMap<>();

    // prevent warning spam
    private final Set<String> dummyWarning = ConcurrentHashMap.newKeySet();

    public TunedBlockManager(BlockTune plugin) {
        this.plugin = plugin;
    }

    public void registerHandler(@NotNull String namespace, @NotNull Supplier<TunedBlockHandler> handlerSupplier) {
        this.blockHandlerMap.put(namespace, handlerSupplier);
    }

    public void registerHandler(@NotNull NamespaceID namespace, @NotNull Supplier<TunedBlockHandler> handlerSupplier) {
        registerHandler(namespace.toString(), handlerSupplier);
    }

    @Nullable
    public TunedBlockHandler getHandler(@NotNull String namespace) {
        Supplier<TunedBlockHandler> handler = this.blockHandlerMap.get(namespace);
        return handler == null ? null : handler.get();
    }

    @ApiStatus.Internal
    @NotNull
    public TunedBlockHandler getHandlerOrDummy(@NotNull String namespace) {
        TunedBlockHandler handler = getHandler(namespace);
        if (handler == null) {
            if (this.dummyWarning.add(namespace)) {
                this.plugin.getLogger().warning("""
                        Block %s does not have any corresponding handler, default to dummy.
                        You may want to register a handler for this namespace to prevent any data loss.
                        """.formatted(namespace));
            }
            handler = TunedBlockHandler.Dummy.get(namespace);
        }
        return handler;
    }

    /**
     * Registers a {@link TunedBlockPlacementRule}.
     *
     * @param blockPlacementRule the block placement rule to register
     * @throws IllegalArgumentException if <code>blockPlacementRule</code> block id is negative
     */
    public synchronized void registerBlockPlacementRule(@NotNull TunedBlockPlacementRule blockPlacementRule) {
        int id = blockPlacementRule.getBlock().id();
        Preconditions.checkArgument(id >= 0, "Block ID must be >= 0, got: " + id);
        this.placementRuleMap.put(id, blockPlacementRule);
    }

    /**
     * Gets the {@link TunedBlockPlacementRule} of the specific block.
     *
     * @param block the block to check
     * @return the block placement rule associated with the block, null if not any
     */
    public synchronized @Nullable TunedBlockPlacementRule getBlockPlacementRule(@NotNull TunedBlock block) {
        return this.placementRuleMap.get(block.id());
    }
}