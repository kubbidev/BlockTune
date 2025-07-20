package me.kubbidev.blocktune.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class PlaceholderAPIParser implements PlaceholderParser {

    public static final PlaceholderParser INSTANCE = new PlaceholderAPIParser();

    private PlaceholderAPIParser() {
    }

    @Override
    public @NotNull Component parse(@NotNull OfflinePlayer player, @NotNull String text) {
        String translatedText = PlaceholderAPI.setPlaceholders(player, text);
        // now that we have translated this text with placeholder api, dont forget to
        // also use the default parsing method as well
        return PlaceholderParser.MINI_MESSAGE.deserialize(translatedText);
    }
}