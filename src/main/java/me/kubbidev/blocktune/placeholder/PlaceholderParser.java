package me.kubbidev.blocktune.placeholder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public interface PlaceholderParser {

    MiniMessage MINI_MESSAGE = buildMiniMessage();

    @NotNull
    private static MiniMessage buildMiniMessage() {
        MiniMessage.Builder builder = MiniMessage.builder();
        // configure the mini message serializer as you want here..
        return builder.build();
    }

    /**
     * Placeholder parsers ALREADY compute color codes!
     * <p>
     * No need to use {@link MiniMessage#deserialize(Object)} when using this method.
     *
     * @return {@link Component} with parsed placeholders AND color codes
     */
    @NotNull
    Component parse(@NotNull OfflinePlayer player, @NotNull String text);
}