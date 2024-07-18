package me.kubbidev.blocktune.scoreboard;

import me.kubbidev.nexuspowered.util.Text;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("SpellCheckingInspection")
public class ScoreboardTemplate {
    public static final String[] SERVER_ADDRESS = {
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
            "<gold>kubbidev.com</gold>",
            "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>",
            "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>",
            "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>",
            "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>",
            "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>",
            "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>",
            "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>",
            "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>",
            "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>", "<gold>kubbidev.com</gold>"
    };

    private final String[] lines;
    private final String title;
    private final String header;
    private final String footer;

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    public ScoreboardTemplate() {
        this.title = "<red><bold>BLOCKTUNE</bold></red>";
        this.lines = new String[]{
                "<gray>%blocktune_system_date%</gray>",
                "",
                "<red><bold>INFOS</bold></red>",
                " <dark_gray>\u2503</dark_gray> Instance : <aqua>%blocktune_player_world%</aqua>",
                "",
                "<red><bold>SERVEUR</bold></red>",
                " <dark_gray>\u2503</dark_gray> Connectés : <green>%blocktune_server_online%</green>",
                "",
                "%blocktune_server_address%",
        };
        this.header = Text.joinNewline("",
                "<dark_gray>\u00BB <red><bold>BLOCKTUNE</bold></red> \u00AB</dark_gray>",
                "%blocktune_server_address%",
                "",
                "Ping: <green>%blocktune_player_ping%ms</green>   Connectés: <green>%blocktune_server_online%</green>",
                ""
        );
        this.footer = Text.joinNewline("",
                " Besoin d'aide ? Rejoins notre support ",
                "<dark_gray>➥</dark_gray> <red>/discord</red>",
                ""
        );
    }

    public @NotNull String[] getLines() {
        return this.lines;
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