package me.kubbidev.blocktune.commands;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.nexuspowered.Commands;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.kubbidev.spellcaster.InternalMethod.getAttributeValue;
import static me.kubbidev.spellcaster.InternalMethod.heal;

public final class HealCommand {

    @SuppressWarnings("resource")
    public static void register(@NotNull BlockTune plugin) {
        Commands.create().assertOp().assertPlayer()
            .handler(context -> {
                Player player = context.sender();
                heal(player, getAttributeValue(player, Attribute.MAX_HEALTH));
            })
            .registerAndBind(plugin, "heal");
    }
}
