package me.kubbidev.blocktune;

import lombok.Getter;
import me.kubbidev.blocktune.config.BlockTuneConfiguration;
import me.kubbidev.blocktune.config.generic.adapter.*;
import me.kubbidev.blocktune.core.ai.TanjiroEntity;
import me.kubbidev.blocktune.core.listener.AttackActionListener;
import me.kubbidev.blocktune.core.listener.AttackEventListener;
import me.kubbidev.blocktune.core.manager.DamageManager;
import me.kubbidev.blocktune.core.manager.EntityManager;
import me.kubbidev.blocktune.core.manager.FakeEventManager;
import me.kubbidev.blocktune.core.manager.IndicatorManager;
import me.kubbidev.blocktune.event.ConfigReloadEvent;
import me.kubbidev.blocktune.scoreboard.ScoreboardManager;
import me.kubbidev.blocktune.placeholder.DefaultPlaceholderParser;
import me.kubbidev.blocktune.placeholder.PlaceholderAPIHook;
import me.kubbidev.blocktune.placeholder.PlaceholderAPIParser;
import me.kubbidev.blocktune.placeholder.PlaceholderParser;
import me.kubbidev.nexuspowered.Commands;
import me.kubbidev.nexuspowered.command.argument.Argument;
import me.kubbidev.nexuspowered.plugin.ExtendedJavaPlugin;
import me.kubbidev.nexuspowered.util.Players;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
public final class BlockTune extends ExtendedJavaPlugin implements Listener {
    // init during enable
    private BlockTuneConfiguration configuration;

    private DamageManager damageManager;
    private EntityManager entityManager;

    private final IndicatorManager indicatorManager = new IndicatorManager();
    private final FakeEventManager fakeEventManager = new FakeEventManager();

    private PlaceholderParser placeholderParser;
    private ScoreboardManager scoreboardManager;

    private AttackActionListener actionListener;

    @Override
    public void load() {
    }

    @Override
    public void enable() {
        // load configuration
        getLogger().info("Loading configuration...");
        ConfigurationAdapter configFileAdapter = provideConfigurationAdapter();
        this.configuration = new BlockTuneConfiguration(this, new MultiConfigurationAdapter(this,
                new SystemPropertyConfigAdapter(this),
                new EnvironmentVariableConfigAdapter(this),
                configFileAdapter
        ));

        // register services
        if (isPluginPresent("PlaceholderAPI")) {
            PlaceholderAPIHook.INSTANCE.register(this);
            this.placeholderParser = PlaceholderAPIParser.INSTANCE;
        } else {
            this.placeholderParser = DefaultPlaceholderParser.INSTANCE;
        }

        this.damageManager = new DamageManager(this);
        this.entityManager = new EntityManager(this);

        // load indicators from configuration file
        this.indicatorManager.load(this);

        // init scoreboard managers listener registering
        this.scoreboardManager = new ScoreboardManager(this);

        // register listeners
        registerListener(this);
        registerListener(this.damageManager);
        registerListener(this.scoreboardManager);
        registerListener(new AttackEventListener(this));

        this.actionListener = new AttackActionListener(this);
        this.actionListener.onEnable();

        registerListener(this.actionListener);

        // register with the BlockTune API
        getServer().getServicesManager().register(BlockTune.class, this, this, ServicePriority.Normal);
        BlockTuneProvider.register(this);

        Commands.create()
                .assertPlayer()
                .handler(context -> {
                    Argument argument = context.arg(0);
                    int amount = 1;
                    if (argument.isPresent()) {
                        amount = argument.parseOrFail(Integer.class);
                    }

                    for (int i = 0; i < amount; i++) {
                        TanjiroEntity entity = new TanjiroEntity(this, context.sender().getLocation());
                        entity.spawn();
                    }
                })
                .registerAndBind(this, "spawn");
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConfigReload(ConfigReloadEvent e) {
        this.indicatorManager.reload(this);
    }

    private ConfigurationAdapter provideConfigurationAdapter() {
        return new BukkitConfigAdapter(this, resolveConfig("config.yml").toFile());
    }

    private Path resolveConfig(String fileName) {
        Path configFile = getConfigDirectory().resolve(fileName);

        // if the config doesn't exist, create it based on the template in the resources dir
        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configFile.getParent());
            } catch (IOException e) {
                // ignore
            }

            try (InputStream is = getResourceStream(fileName)) {
                Files.copy(is, configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return configFile;
    }

    // provide information about the platform

    /**
     * Gets the plugins configuration directory
     *
     * @return the config directory
     */
    public Path getConfigDirectory() {
        return getDataFolder().toPath().toAbsolutePath();
    }

    /**
     * Gets a bundled resource file from the jar
     *
     * @param path the path of the file
     * @return the file as an input stream
     */
    public InputStream getResourceStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}