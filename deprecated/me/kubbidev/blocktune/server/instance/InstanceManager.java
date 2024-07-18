package me.kubbidev.blocktune.server.instance;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.server.MinecraftServer;
import me.kubbidev.blocktune.server.event.instance.InstanceRegisterEvent;
import me.kubbidev.blocktune.server.event.instance.InstanceUnregisterEvent;
import me.kubbidev.blocktune.server.thread.ThreadDispatcher;
import me.kubbidev.nexuspowered.Events;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Used to register {@link Instance}.
 */
public final class InstanceManager {
    private final BlockTune plugin;
    private final Set<Instance> instances = new CopyOnWriteArraySet<>();

    public InstanceManager(@NotNull BlockTune plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers an {@link Instance} internally.
     * <p>
     * Note: not necessary if you created your instance using {@link #createInstanceContainer(World)}
     * but only if you instantiated your instance object manually
     *
     * @param instance the {@link Instance} to register
     */
    @ApiStatus.Internal
    public void registerInstance(@NotNull Instance instance) {
        UNSAFE_registerInstance(instance);
    }

    /**
     * Creates and register an {@link InstanceContainer} with the specified {@link UUID}.
     *
     * @param bukkitInstance the bukkit world instance
     * @param loader         the chunk loader
     * @return the created {@link InstanceContainer}
     */
    @ApiStatus.Experimental
    @ApiStatus.Internal
    public @NotNull InstanceContainer createInstanceContainer(@NotNull World bukkitInstance, @Nullable IChunkLoader loader) {
        InstanceContainer instanceContainer = new InstanceContainer(this.plugin, bukkitInstance, loader);
        registerInstance(instanceContainer);
        return instanceContainer;
    }

    public @NotNull InstanceContainer createInstanceContainer(@NotNull World world) {
        return createInstanceContainer(world, null);
    }

    /**
     * Unregisters the {@link Instance} internally.
     * <p>
     * If {@code instance} is an {@link InstanceContainer} all chunks are unloaded.
     *
     * @param instance the {@link Instance} to unregister
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @ApiStatus.Internal
    public void unregisterInstance(@NotNull Instance instance) {
        synchronized (instance) {
            InstanceUnregisterEvent event = new InstanceUnregisterEvent(instance);
            Events.call(event);

            // unload all chunks
            if (instance instanceof InstanceContainer) {
                instance.getChunks().forEach(instance::unloadChunk);
                ThreadDispatcher<Chunk> dispatcher = MinecraftServer.process().dispatcher();
                instance.getChunks().forEach(dispatcher::deletePartition);
            }
            // unregister
            instance.setRegistered(false);
            this.instances.remove(instance);
        }
    }

    /**
     * Gets all the registered instances.
     *
     * @return an unmodifiable {@link Set} containing all the registered instances
     */
    public @NotNull Set<@NotNull Instance> getInstances() {
        return Collections.unmodifiableSet(this.instances);
    }

    /**
     * Gets an instance by the given UUID.
     *
     * @param uuid UUID of the instance
     * @return the instance with the given UUID, null if not found
     */
    public @Nullable Instance getInstance(@NotNull UUID uuid) {
        Optional<Instance> instance = getInstances()
                .stream()
                .filter(someInstance -> someInstance.getIdentifier().equals(uuid))
                .findFirst();
        return instance.orElse(null);
    }

    /**
     * Registers an {@link Instance} internally.
     *
     * @param instance the {@link Instance} to register
     */
    private void UNSAFE_registerInstance(@NotNull Instance instance) {
        instance.setRegistered(true);
        this.instances.add(instance);
//        ThreadDispatcher<Chunk> dispatcher = MinecraftServer.process().dispatcher();
//        instance.getChunks().forEach(dispatcher::createPartition);
        InstanceRegisterEvent event = new InstanceRegisterEvent(instance);
        Events.call(event);
    }
}