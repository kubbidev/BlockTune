package me.kubbidev.blocktune.server.util.async;

import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.CompletableFuture;

@ApiStatus.Internal
public final class AsyncUtil {
    public static final CompletableFuture<Void> VOID_FUTURE = CompletableFuture.completedFuture(null);

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> empty() {
        return (CompletableFuture<T>) VOID_FUTURE;
    }
}