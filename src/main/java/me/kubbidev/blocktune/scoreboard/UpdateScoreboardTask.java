package me.kubbidev.blocktune.scoreboard;

import me.kubbidev.nexuspowered.scheduler.Task;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class UpdateScoreboardTask implements Consumer<Task> {
    private final Player player;

    private final ScoreboardManager scoreboardManager;
    private final Scoreboard scoreboard;

    // scoreboard type used to evaluate the amount and type of lines to display
    private ScoreboardTemplate.ScoreboardType lastType = ScoreboardTemplate.ScoreboardType.NORMAL;

    public UpdateScoreboardTask(@NotNull ScoreboardManager scoreboardManager, @NotNull Scoreboard scoreboard) {
        this.player = scoreboard.getPlayer();

        this.scoreboardManager = scoreboardManager;
        this.scoreboard = scoreboard;

        // init scoreboard lines with empty lines (used when registering the objective for now)
        resetObjective(this.lastType);
    }

    public void resetObjective(@NotNull ScoreboardTemplate.ScoreboardType type) {
        Component[] lines = new Component[this.scoreboardManager.getScoreboardTemplate().getLines(type).length];
        // reset lines size to display
        this.scoreboard.updateLines(lines);
        this.scoreboard.resetObjective();
    }

    @NotNull
    private Component decorate(@NotNull String text) {
        return this.scoreboardManager.getPlugin().getPlaceholderParser().parse(this.player, text);
    }

    @NotNull
    private Component[] decorate(@NotNull String[] text) {
        Component[] newText = new Component[text.length];
        for (int i = 0; i < newText.length; i++) {
            newText[i] = decorate(text[text.length - 1 - i]);
        }
        return newText;
    }

    @Override
    public void accept(Task task) {
        if (!this.player.isOnline()) {
            return;
        }

        // retrieve the player actual scoreboard type here
        ScoreboardTemplate.ScoreboardType type
                = ScoreboardTemplate.ScoreboardType.NORMAL;

        if (this.lastType != type) {
            this.lastType = type;
            resetObjective(type);
        }

        ScoreboardTemplate template = this.scoreboardManager.getScoreboardTemplate();
        this.scoreboard.updateLines(decorate(template.getLines(type)));
        this.scoreboard.updateTitle(decorate(template.getTitle()));

        this.player.sendPlayerListHeaderAndFooter(
                decorate(template.getHeader()),
                decorate(template.getFooter())
        );
    }
}