package me.kubbidev.blocktune.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.scoreboard.ScoreboardTemplate;
import me.kubbidev.blocktune.scoreboard.ScoreboardAnimation;
import me.kubbidev.nexuspowered.Schedulers;
import me.kubbidev.nexuspowered.util.Players;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * A placeholder that just passes on to {@link me.clip.placeholderapi.PlaceholderAPI} to do all the parsing.
 */
public final class PlaceholderAPIHook extends PlaceholderExpansion implements Runnable {
    public static final PlaceholderAPIHook INSTANCE = new PlaceholderAPIHook();

    // constant variable used on request to parse time to a more human readable way
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // cached server address animation currently being animated
    private final ScoreboardAnimation<String> serverAddress;

    private PlaceholderAPIHook() {
        this.serverAddress = new ScoreboardAnimation<>(ScoreboardTemplate.SERVER_ADDRESS);
        this.serverAddress.setBackAndForth(true);
    }

    @SuppressWarnings("resource")
    public void register(@NotNull BlockTune plugin) {
        super.register();
        Schedulers.sync().runRepeating(this, 1L, 2L).bindWith(plugin);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "blocktune";
    }

    @Override
    public @NotNull String getAuthor() {
        return "kubbidev";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public @Nullable String onPlaceholderRequest(@Nullable Player player, @NotNull String params) {
        return switch (params) {
            case "system_date" -> DATE_FORMATTER.format(LocalDate.now());
            case "server_online" -> String.valueOf(Players.all().size());
            case "server_address" -> getServerAddressCurrentFrame();
            default -> {
                if (player == null) yield null;
                /*
                    store variables to avoid reuse, this method needs a lot of optimization
                    cause it may be call more than 20 times per seconds for each players on the server
                */
                yield switch (params) {
                    case "player_world" -> player.getWorld().getName();
                    case "player_ping" -> String.valueOf(player.getPing());
                    default -> null;
                };
            }
        };
    }

    public String getServerAddressCurrentFrame() {
        return this.serverAddress.getCurrentFrame();
    }

    @Override
    public void run() {
        // server address animation
        this.serverAddress.nextFrame();
    }
}
