package me.kubbidev.blocktune.commands;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.nexuspowered.Commands;
import org.jetbrains.annotations.NotNull;

public final class FeedCommand {

    @SuppressWarnings("resource")
    public static void register(@NotNull BlockTune plugin) {
        Commands.create().assertOp().assertPlayer()
                .handler(context -> {
                    context.sender().setFoodLevel(20);
                    context.sender().setSaturation(20);
                })
                .registerAndBind(plugin, "feed");
    }
}
