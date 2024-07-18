package me.kubbidev.blocktune.server.registry;

import org.jetbrains.annotations.Nullable;

public interface ProtocolObject {

    default @Nullable Object registry() {
        return null;
    }
}