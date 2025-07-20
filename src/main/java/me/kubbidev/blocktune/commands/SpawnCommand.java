package me.kubbidev.blocktune.commands;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.entity.TanjiroEntity;
import me.kubbidev.nexuspowered.Commands;
import me.kubbidev.nexuspowered.command.argument.Argument;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public final class SpawnCommand {

    @SuppressWarnings("resource")
    public static void register(@NotNull BlockTune plugin) {
        Commands.create()
            .assertPlayer()
            .handler(context -> {
                Argument argumentAmount = context.arg(0);
                int amount = 1;
                if (argumentAmount.isPresent()) {
                    amount = argumentAmount.parseOrFail(Integer.class);
                }

                Argument argumentAttackSpeed = context.arg(1);
                int attackSpeed = new Random().nextInt(10, 61);
                if (argumentAttackSpeed.isPresent()) {
                    attackSpeed = argumentAttackSpeed.parseOrFail(Integer.class);
                }

                for (int i = 0; i < amount; i++) {
                    TanjiroEntity entity = new TanjiroEntity(plugin, context.sender().getLocation());
                    entity.setAttackSpeed(attackSpeed);
                    entity.spawn();
                }
            })
            .registerAndBind(plugin, "spawn");
    }
}
