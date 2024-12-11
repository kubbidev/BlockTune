package me.kubbidev.blocktune.util;

import me.kubbidev.blocktune.BlockTune;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class Placeholders implements BiFunction<OfflinePlayer, String, Component> {
    private final BlockTune plugin;
    private final Map<String, String> placeholders = new HashMap<>();

    public Placeholders(@NotNull BlockTune plugin) {
        this.plugin = plugin;
    }

    public void register(@Nullable String path, @Nullable Object obj) {
        this.placeholders.put(path, String.valueOf(obj));
    }

    public @Nullable String getPlaceholder(@NotNull String placeholder) {
        return this.placeholders.get(placeholder);
    }

    @Override
    public Component apply(OfflinePlayer player, String s) {
        String e = s;
        while (e.contains("{") && e.substring(e.indexOf('{')).contains("}")) {
            int begin = e.indexOf('{');
            int end = e.indexOf('}');

            String holder = e.substring(begin + 1, end);
            String found = getPlaceholder(holder);

            if (found != null) {
                s = s.replace("{" + holder + "}", found);
            }
            e = e.substring(end + 1);
        }

        return this.plugin.getPlaceholderParser().parse(player, s);
    }
}