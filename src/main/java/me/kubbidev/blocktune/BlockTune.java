package me.kubbidev.blocktune;

import me.kubbidev.blocktune.commands.*;
import me.kubbidev.blocktune.config.ConfigKeys;
import me.kubbidev.blocktune.manager.SpellManager;
import me.kubbidev.blocktune.spell.listener.AttackActionListener;
import me.kubbidev.blocktune.scoreboard.ScoreboardManager;
import me.kubbidev.blocktune.placeholder.DefaultPlaceholderParser;
import me.kubbidev.blocktune.placeholder.PlaceholderAPIHook;
import me.kubbidev.blocktune.placeholder.PlaceholderAPIParser;
import me.kubbidev.blocktune.placeholder.PlaceholderParser;
import me.kubbidev.nexuspowered.config.KeyedConfiguration;
import me.kubbidev.nexuspowered.plugin.ExtendedJavaPlugin;
import me.kubbidev.nexuspowered.util.Players;
import org.jetbrains.annotations.NotNull;

public final class BlockTune extends ExtendedJavaPlugin {
    // init during enable
    private KeyedConfiguration configuration;

    private PlaceholderParser placeholderParser;
    private ScoreboardManager scoreboardManager;

    private AttackActionListener actionListener;

    // gameplay features
    // todo implement reload method for spell manager
    private SpellManager spellManager;

    @Override
    public void load() {
    }

    @Override
    public void enable() {
        // load configuration
        getLogger().info("Loading configuration...");
        this.configuration = loadKeyedConfig("config.yml", ConfigKeys.getKeys());

        // register services
        if (isPluginPresent("PlaceholderAPI")) {
            PlaceholderAPIHook.INSTANCE.register(this);
            this.placeholderParser = PlaceholderAPIParser.INSTANCE;
        } else {
            this.placeholderParser = DefaultPlaceholderParser.INSTANCE;
        }

        // init scoreboard managers listener registering
        this.scoreboardManager = new ScoreboardManager(this);

        this.actionListener = new AttackActionListener(this);
        this.actionListener.onEnable();

        this.spellManager = new SpellManager(this);
        this.spellManager.load(false);

        // register listeners
        registerPlatformListeners();

        // register commands
        registerCommands();

        // register with the BlockTune API
        BlockTuneProvider.register(this);
        registerApiOnPlatform();
    }

    @Override
    public void disable() {
        // unregister api
        BlockTuneProvider.unregister();

        // disable listeners
        getActionListener().onDisable();

        // reload all scoreboard
        Players.forEach(player -> {
            getScoreboardManager().unregisterScoreboard(player);
            getScoreboardManager().registerScoreboard(player);
        });
    }

    public void reloadPlugin() {
        this.configuration.reload();
        // reload spells after the configuration reload itself
        this.spellManager.load(true);
    }

    private void registerPlatformListeners() {
        registerListener(this.scoreboardManager);
        registerListener(this.actionListener);
    }

    private void registerCommands() {
        HealCommand.register(this);
        FeedCommand.register(this);
        SpawnCommand.register(this);
    }

    private void registerApiOnPlatform() {
        provideService(BlockTune.class, this);
    }

    public @NotNull KeyedConfiguration getConfiguration() {
        return this.configuration;
    }

    public @NotNull PlaceholderParser getPlaceholderParser() {
        return this.placeholderParser;
    }

    public @NotNull ScoreboardManager getScoreboardManager() {
        return this.scoreboardManager;
    }

    public @NotNull AttackActionListener getActionListener() {
        return this.actionListener;
    }

    public @NotNull SpellManager getSpellManager() {
        return this.spellManager;
    }
}