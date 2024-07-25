package me.kubbidev.blocktune.scoreboard;

import me.kubbidev.nexuspowered.util.Text;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("SpellCheckingInspection")
public class ScoreboardTemplate {
    public static final String[] SERVER_ADDRESS = {
            "<gold>kubbidev.com</gold>",
            "<gold><yellow>k</yellow>ubbidev.com</gold>",
            "<gold><yellow><white>k</white>u</yellow>bbidev.com</gold>",
            "<gold><yellow>k<white>u</white>b</yellow>bidev.com</gold>",
            "<gold>k<yellow>u<white>b</white>b</yellow>idev.com</gold>",
            "<gold>ku<yellow>b<white>b</white>i</yellow>dev.com</gold>",
            "<gold>kub<yellow>b<white>i</white>d</yellow>ev.com</gold>",
            "<gold>kubb<yellow>i<white>d</white>e</yellow>v.com</gold>",
            "<gold>kubbi<yellow>d<white>e</white>v</yellow>.com</gold>",
            "<gold>kubbid<yellow>e<white>v</white>.</yellow>com</gold>",
            "<gold>kubbide<yellow>v<white>.</white>c</yellow>om</gold>",
            "<gold>kubbidev<yellow>.<white>c</white>o</yellow>m</gold>",
            "<gold>kubbidev.<yellow>c<white>o</white></yellow>m</gold>",
            "<gold>kubbidev.c<yellow>o<white>m</white></yellow></gold>",
            "<gold>kubbidev.co<yellow>m</yellow></gold>",
            "<gold>kubbidev.com</gold>"
    };

    private final String[][] lines;
    private final String title;
    private final String header;
    private final String footer;

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    public ScoreboardTemplate() {
        this.title = "<red><bold>BLOCKTUNE</bold></red>";
        this.lines = new String[][]{
                new String[]{
                        "<gray>%server_time_dd/MM/yyyy%</gray>",
                        "",
                        "<red><bold>INFOS</bold></red>",
                        " <dark_gray>\u2503</dark_gray> Instance : <aqua>%player_world%</aqua>",
                        " <dark_gray>\u2503</dark_gray> x : <yellow>%player_x%</yellow>",
                        " <dark_gray>\u2503</dark_gray> y : <yellow>%player_y%</yellow>",
                        " <dark_gray>\u2503</dark_gray> z : <yellow>%player_z%</yellow>",
                        " <dark_gray>\u2503</dark_gray> Biome : <green>%player_biome_capitalized%</green>",
                        "",
                        "<red><bold>SERVER</bold></red>",
                        " <dark_gray>\u2503</dark_gray> Ram : <gold>%server_ram_used%</gold>/<gold>%server_ram_max%</gold>",
                        " <dark_gray>\u2503</dark_gray> TPS : <green>%server_tps_1%</green>",
                        " <dark_gray>\u2503</dark_gray> Online : <green>%server_online%</green>",
                        "",
                        "%blocktune_server_address%"
                }
        };
        this.header = Text.joinNewline("",
                "<dark_gray>\u00BB <red><bold>BLOCKTUNE</bold></red> \u00AB</dark_gray>",
                "%blocktune_server_address%",
                "",
                "Ping: <green>%player_ping%ms</green>   Online: <green>%server_online%</green>",
                ""
        );
        this.footer = Text.joinNewline("",
                " Need help? Join our support server ",
                "<dark_gray>➥</dark_gray> <red>/discord</red>",
                ""
        );
    }

    public enum ScoreboardType {
        NORMAL
    }

    public @NotNull String[] getLines(@NotNull ScoreboardType type) {
        return this.lines[type.ordinal()];
    }

    public @NotNull String getTitle() {
        return this.title;
    }

    public @NotNull String getFooter() {
        return footer;
    }

    public @NotNull String getHeader() {
        return this.header;
    }
}