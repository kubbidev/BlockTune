package me.kubbidev.blocktune.server.monitoring.instance;

import me.kubbidev.blocktune.server.MinecraftServer;
import me.kubbidev.blocktune.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InstanceChunkCollector implements InstanceMonitor.ChunkListener {
    private static final Class<?> CHUNK_ACCESS_CLASS;
    private static final Class<?> CHUNK_HOLDER_CLASS;
    private static final Method GET_TICKING_CHUNK_METHOD;

    private static final Method GET_VISIBLE_CHUNK_HOLDERS_METHOD;
    private static final Method GET_HANDLE_METHOD;

    private static final Field LEVEL_CHUNK_X_FIELD;
    private static final Field LEVEL_CHUNK_Z_FIELD;

    private final InstanceMonitor monitor;

    InstanceChunkCollector(@NotNull InstanceMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void onChunkLoaded(Object nmsChunk, @NotNull Instance instance) {
        try {
            this.monitor.onChunkLoad(instance,
                    (Integer) LEVEL_CHUNK_X_FIELD.get(nmsChunk),
                    (Integer) LEVEL_CHUNK_Z_FIELD.get(nmsChunk));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onChunkUnloaded(Object nmsChunk, @NotNull Instance instance) {
        try {
            int x = (Integer) LEVEL_CHUNK_X_FIELD.get(nmsChunk);
            int z = (Integer) LEVEL_CHUNK_Z_FIELD.get(nmsChunk);
            this.monitor.onChunkUnload(instance, x, z);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    List<Object> collectChunks(@NotNull Instance instance) {
        List<Object> result = new ArrayList<>();
        try {
            Object handle = GET_HANDLE_METHOD.invoke(instance.getBukkitInstance());
            // retrieve all loaded chunks by server and loop them in way to filter them later
            // to determine which chunk need to be reported
            for (Object chunk : (Iterable<?>) GET_VISIBLE_CHUNK_HOLDERS_METHOD.invoke(null, handle)) {
                Object chunkAccess = GET_TICKING_CHUNK_METHOD.invoke(chunk);
                // determine if the current iterating chunk is actually ticking blocks
                // if so, add it to the chunks result list
                if (chunkAccess != null) {
                    result.add(chunkAccess);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            MinecraftServer.getExceptionManager().handleException(e);
            // save fail exception, return an empty list if any nms error occurred
            return Collections.emptyList();
        }
        return result;
    }

    static {
        try {
            CHUNK_ACCESS_CLASS = Class.forName("net.minecraft.world.level.chunk.ChunkAccess");
            CHUNK_HOLDER_CLASS = Class.forName("net.minecraft.server.level.ChunkHolder");
            GET_TICKING_CHUNK_METHOD = CHUNK_HOLDER_CLASS.getDeclaredMethod("getTickingChunk");
            GET_TICKING_CHUNK_METHOD.setAccessible(true);

            Class<?> chunkSystemClass = Class.forName("ca.spottedleaf.moonrise.patches.chunk_system.ChunkSystem");
            Class<?> serverLevelClass = Class.forName("net.minecraft.server.level.ServerLevel");
            GET_VISIBLE_CHUNK_HOLDERS_METHOD = chunkSystemClass.getDeclaredMethod("getVisibleChunkHolders", serverLevelClass);
            GET_VISIBLE_CHUNK_HOLDERS_METHOD.setAccessible(true);

            Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit.CraftWorld");
            GET_HANDLE_METHOD = craftWorldClass.getDeclaredMethod("getHandle");
            GET_HANDLE_METHOD.setAccessible(true);

            LEVEL_CHUNK_X_FIELD = CHUNK_ACCESS_CLASS.getDeclaredField("locX");
            LEVEL_CHUNK_Z_FIELD = CHUNK_ACCESS_CLASS.getDeclaredField("locZ");
            LEVEL_CHUNK_X_FIELD.setAccessible(true);
            LEVEL_CHUNK_Z_FIELD.setAccessible(true);

        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
