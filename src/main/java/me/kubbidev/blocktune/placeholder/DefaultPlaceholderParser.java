package me.kubbidev.blocktune.placeholder;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class DefaultPlaceholderParser implements PlaceholderParser {
    public static final PlaceholderParser INSTANCE = new DefaultPlaceholderParser();

    private DefaultPlaceholderParser() {
    }

    @Override
    public @NotNull Component parse(@NotNull OfflinePlayer player, @NotNull String text) {
        return MINI_MESSAGE.deserialize(text);
    }
}