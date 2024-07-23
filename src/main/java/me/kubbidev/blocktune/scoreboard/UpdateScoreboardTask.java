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

    public UpdateScoreboardTask(@NotNull ScoreboardManager scoreboardManager, @NotNull Scoreboard scoreboard) {
        this.player = scoreboard.getPlayer();

        this.scoreboardManager = scoreboardManager;
        this.scoreboard = scoreboard;

        // init scoreboard lines with empty lines (used when registering the objective for now)
        this.scoreboard.updateLines(new Component[scoreboardManager.getScoreboardTemplate().getLines().length]);
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

        ScoreboardTemplate template = this.scoreboardManager.getScoreboardTemplate();
        this.scoreboard.updateLines(decorate(template.getLines()));
        this.scoreboard.updateTitle(decorate(template.getTitle()));

        this.player.sendPlayerListHeaderAndFooter(
                decorate(template.getHeader()),
                decorate(template.getFooter())
        );
    }
}