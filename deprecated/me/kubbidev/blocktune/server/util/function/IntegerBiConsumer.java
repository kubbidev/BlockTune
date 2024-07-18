package me.kubbidev.blocktune.server.util.function;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@FunctionalInterface
public interface IntegerBiConsumer {
    void accept(int v1, int v2);
}