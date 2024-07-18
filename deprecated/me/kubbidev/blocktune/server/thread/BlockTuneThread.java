package me.kubbidev.blocktune.server.thread;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public class BlockTuneThread extends Thread {
    public BlockTuneThread(@NotNull String name) {
        super(name);
    }
}