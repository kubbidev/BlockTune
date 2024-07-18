package me.kubbidev.blocktune.debug;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.server.MinecraftServer;
import me.kubbidev.blocktune.server.instance.Instance;
import me.kubbidev.blocktune.server.instance.block.TunedBlock;
import me.kubbidev.nexuspowered.Commands;
import org.bukkit.Location;

public final class BlockCommand {
    private BlockCommand() {
    }

    public static void register(BlockTune plugin) {
        Commands.create()
                .assertPlayer()
                .handler(context -> {
                    Location location = context.sender().getLocation();
                    Instance instance = MinecraftServer.getInstanceManager().getInstance(context.sender().getWorld().getUID());
                    if (instance != null) {
                        instance.setBlock(
                                location.getBlockX(),
                                location.getBlockY(),
                                location.getBlockZ(),
                                TunedBlock.PAPER_WALL.withHandler(PaperWallHandler.INSTANCE)
                        );
                    }
                })
                .registerAndBind(plugin, "create");

        Commands.create()
                .assertPlayer()
                .handler(context -> {
                    Location location = context.sender().getLocation();
                    Instance instance = MinecraftServer.getInstanceManager().getInstance(context.sender().getWorld().getUID());
                    if (instance != null) {
                        instance.setBlock(
                                location.getBlockX(),
                                location.getBlockY(),
                                location.getBlockZ(),
                                TunedBlock.AIR
                        );
                    }
                })
                .registerAndBind(plugin, "delete");
    }
}
