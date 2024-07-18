package me.kubbidev.blocktune.container;

import me.kubbidev.blocktune.BlockTuneProvider;
import me.kubbidev.nexuspowered.serialize.BlockPosition;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockContainer implements PersistentDataContainer {
    // pattern regex used to parse block identifier from storage container
    private static final Pattern IDENTIFIER_REGEX;

    // namespace key use to tell if a block container can be destroyed or not
    private static final NamespacedKey PERSISTENCE_KEY;

    static {
        IDENTIFIER_REGEX = Pattern.compile("^x(\\d+)y(-?\\d+)z(\\d+)$");
        PERSISTENCE_KEY = new NamespacedKey(BlockTuneProvider.get(), "protected");
    }

    private final BlockPosition position;
    private final NamespacedKey identifier;

    private final PersistentDataContainer chunkContainer;
    private final PersistentDataContainer blockContainer;

    public BlockContainer(Block block) {
        this.position = BlockPosition.of(block);
        this.identifier = getBlockIdentifier(block);

        this.chunkContainer = block.getChunk().getPersistentDataContainer();
        this.blockContainer = getPersistentDataContainer();
    }

    private PersistentDataContainer getPersistentDataContainer() {
        PersistentDataContainer chunkContainer = this.chunkContainer;
        PersistentDataContainer blockContainer;

        blockContainer = chunkContainer.get(this.identifier, PersistentDataType.TAG_CONTAINER);
        if (blockContainer == null) {
            blockContainer = chunkContainer.getAdapterContext().newPersistentDataContainer();
        }
        return blockContainer;
    }

    public Block getBlock() {
        return this.position.toBlock();
    }

    public BlockPosition getPosition() {
        return this.position;
    }

    public boolean isProtected() {
        return this.blockContainer.has(PERSISTENCE_KEY, PersistentDataType.BOOLEAN);
    }

    public void setProtected(boolean isProtected) {
        if (isProtected) {
            this.blockContainer.set(PERSISTENCE_KEY, PersistentDataType.BOOLEAN, true);
        } else {
            this.blockContainer.remove(PERSISTENCE_KEY);
        }
    }

    public void clearContainer() {
        Set<NamespacedKey> keys = this.blockContainer.getKeys();
        for (NamespacedKey key : keys) {
            this.blockContainer.remove(key);
        }
        this.saveContainer();
    }

    public void copyTo(Block block) {
        copyTo(new BlockContainer(block), true);
    }

    private void saveContainer() {
        BlockContainerListener.setDirty(this.position);
        if (this.blockContainer.isEmpty()) {
            this.chunkContainer.remove(this.identifier);
        } else {
            this.chunkContainer.set(this.identifier, PersistentDataType.TAG_CONTAINER, this.blockContainer);
        }
    }

    private static NamespacedKey getBlockIdentifier(Block block) {
        return new NamespacedKey(BlockTuneProvider.get(), getBlockIdentifierKey(block));
    }

    private static String getBlockIdentifierKey(Block block) {
        int x = block.getX() & 15;
        int y = block.getY();
        int z = block.getZ() & 15;
        return "x" + x + "y" + y + "z" + z;
    }

    /**
     * Gets if the block have a {@link BlockContainer} attach to him.
     *
     * @param block to block process
     * @return true if containing a container, otherwise false
     */
    public static boolean hasBlockContainer(Block block) {
        return block.getChunk().getPersistentDataContainer().has(getBlockIdentifier(block), PersistentDataType.TAG_CONTAINER);
    }

    public static boolean isBlockProtected(Block block) {
        return new BlockContainer(block).isProtected();
    }

    @Nullable
    public static Block getBlockFromIdentifierKey(String key, Chunk chunk) {
        Matcher matcher = IDENTIFIER_REGEX.matcher(key);
        if (!matcher.matches()) {
            return null;
        } else {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            if (x < 0 || x > 15 || z < 0 || z > 15
                    || y < chunk.getWorld().getMinHeight()
                    || y > chunk.getWorld().getMaxHeight() - 1) {
                return null;
            } else {
                return chunk.getBlock(x, y, z);
            }
        }
    }

    @Override
    public <P, C> void set(@NotNull NamespacedKey key, @NotNull PersistentDataType<P, C> type, @NotNull C value) {
        this.blockContainer.set(key, type, value);
        saveContainer();
    }

    @Override
    public void remove(@NotNull NamespacedKey key) {
        this.blockContainer.remove(key);
        saveContainer();
    }

    @Override
    public void readFromBytes(byte @NotNull [] bytes, boolean clear) throws IOException {
        this.blockContainer.readFromBytes(bytes, clear);
    }

    @Override
    public <P, C> boolean has(@NotNull NamespacedKey key, @NotNull PersistentDataType<P, C> type) {
        return this.blockContainer.has(key, type);
    }

    @Override
    public boolean has(@NotNull NamespacedKey key) {
        return this.blockContainer.has(key);
    }

    @Override
    public <P, C> @Nullable C get(@NotNull NamespacedKey key, @NotNull PersistentDataType<P, C> type) {
        return this.blockContainer.get(key, type);
    }

    @Override
    public <P, C> @NotNull C getOrDefault(@NotNull NamespacedKey key, @NotNull PersistentDataType<P, C> type, @NotNull C defaultValue) {
        return this.blockContainer.getOrDefault(key, type, defaultValue);
    }

    @Override
    public @NotNull Set<NamespacedKey> getKeys() {
        return this.blockContainer.getKeys();
    }

    @Override
    public boolean isEmpty() {
        return this.blockContainer.isEmpty();
    }

    @Override
    public void copyTo(@NotNull PersistentDataContainer other, boolean replace) {
        this.blockContainer.copyTo(other, replace);
    }

    @Override
    public @NotNull PersistentDataAdapterContext getAdapterContext() {
        return this.blockContainer.getAdapterContext();
    }

    @Override
    public byte @NotNull [] serializeToBytes() throws IOException {
        return this.blockContainer.serializeToBytes();
    }
}
