package me.kubbidev.blocktune.scoreboard;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.nexuspowered.Schedulers;
import me.kubbidev.nexuspowered.scheduler.Task;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager implements Listener {
    private final BlockTune plugin;
    private final ScoreboardTemplate scoreboardTemplate = new ScoreboardTemplate();

    // uuid -> player identifier, cache use to keep track of all scoreboard instances running
    private final Map<UUID, Scoreboard> registeredScoreboard = new ConcurrentHashMap<>();

    public ScoreboardManager(@NotNull BlockTune plugin) {
        this.plugin = plugin;
    }

    public @NotNull BlockTune getPlugin() {
        return this.plugin;
    }

    public @NotNull ScoreboardTemplate getScoreboardTemplate() {
        return this.scoreboardTemplate;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        registerScoreboard(e.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerQuitEvent e) {
        unregisterScoreboard(e.getPlayer());
    }

    public void registerScoreboard(@NotNull Player player) {
        Scoreboard scoreboard = new Scoreboard(player);
        this.registeredScoreboard.put(player.getUniqueId(), scoreboard);
        // start the scoreboard thread after registration
        startScoreboardThread(scoreboard);
    }

    private void startScoreboardThread(@NotNull Scoreboard scoreboard) {
        UpdateScoreboardTask thread = new UpdateScoreboardTask(this, scoreboard);
        // create a new task that will loop indefinitely and update the scoreboard every time it ticks
        Task task = Schedulers.sync().runRepeating(thread, 1L, 2L);
        task.bindWith(scoreboard);
    }

    public void unregisterScoreboard(@NotNull Player player) {
        Scoreboard scoreboard = this.registeredScoreboard.remove(player.getUniqueId());
        if (scoreboard != null) {
            scoreboard.unregister();
        }
    }

    public @NotNull Collection<Scoreboard> getScoreboards() {
        return this.registeredScoreboard.values();
    }
}