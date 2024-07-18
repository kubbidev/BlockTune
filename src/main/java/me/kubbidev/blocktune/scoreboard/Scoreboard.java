package me.kubbidev.blocktune.scoreboard;

import lombok.Getter;
import me.kubbidev.nexuspowered.terminable.TerminableConsumer;
import me.kubbidev.nexuspowered.terminable.composite.CompositeTerminable;
import me.kubbidev.nexuspowered.terminable.module.TerminableModule;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Scoreboard implements TerminableConsumer {
    private static final String[] SCOREBOARD_LINES;

    // the backing terminable registry
    private final CompositeTerminable compositeTerminable = CompositeTerminable.create();

    static {
        SCOREBOARD_LINES = new String[15];
        for (int i = 0; i < 15; i++) {
            SCOREBOARD_LINES[i] = '§' + Integer.toHexString(i) + "§r";
        }
    }

    private final org.bukkit.scoreboard.Scoreboard scoreboard;

    @Getter
    private final Player player;
    private final List<Component> currentLines = new ArrayList<>();

    private Objective objective;
    private Component title = Component.empty();

    public Scoreboard(@NotNull Player player) {
        Objects.requireNonNull(player, "player");

        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        for (int i = 0; i < 15; i++) {
            Team team = this.scoreboard.registerNewTeam(SCOREBOARD_LINES[i]);
            team.addEntry(SCOREBOARD_LINES[i]);
        }
        this.player = player;
        this.player.setScoreboard(this.scoreboard);
    }

    public void updateTitle(@NotNull Component title) {
        Objects.requireNonNull(title, "title");
        if (this.title.equals(title)) {
            return;
        }
        this.title = title;
        this.objective.displayName(title);
    }

    public void updateLines(@NotNull Component @Nullable [] lines) {
        Objects.requireNonNull(lines, "lines");
        if (lines.length > 15) {
            lines = Arrays.copyOf(lines, 15);
        }
        synchronized (this.currentLines) {
            this.currentLines.clear();
            Collections.addAll(this.currentLines, lines);
            int i = 0;
            for (Component line : this.currentLines) {
                Team teamAsLine = this.scoreboard.getTeam(SCOREBOARD_LINES[i]);
                if (teamAsLine == null) {
                    continue;
                }
                Component old = teamAsLine.prefix();
                // set a condition to avoid updating lines at each iteration, to reduce stress on the server,
                // which would have to send a new team package for each team present at each tick
                if (shouldUpdateLine(old, line)) {
                    teamAsLine.prefix(line);
                }
                ++i;
            }
        }
    }

    private boolean shouldUpdateLine(@NotNull Component old, @NotNull Component line) {
        // todo go more deeply in the equals predicate
        return !Objects.equals(old, line);
    }

    void resetObjective() {
        if (this.objective != null) {
            this.objective.unregister();
        }
        this.objective = this.scoreboard.registerNewObjective("blocktune:main", Criteria.DUMMY, this.title);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int bound = this.currentLines.size();
        for (int i = 0; i < bound; i++) {
            Score score = this.objective.getScore(SCOREBOARD_LINES[i]);
            score.setScore(i);
        }
    }

    void unregister() {
        this.objective.unregister();
        // stop listening
        this.compositeTerminable.closeAndReportException();
    }

    @Override
    public <T extends AutoCloseable> @NotNull T bind(@NotNull T terminable) {
        return this.compositeTerminable.bind(terminable);
    }

    @Override
    public <T extends TerminableModule> @NotNull T bindModule(@NotNull T module) {
        return this.compositeTerminable.bindModule(module);
    }
}