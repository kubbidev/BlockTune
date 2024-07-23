package me.kubbidev.blocktune;

import me.kubbidev.blocktune.ai.TanjiroEntity;
import me.kubbidev.blocktune.spell.listener.AttackActionListener;
import me.kubbidev.blocktune.scoreboard.ScoreboardManager;
import me.kubbidev.blocktune.placeholder.DefaultPlaceholderParser;
import me.kubbidev.blocktune.placeholder.PlaceholderAPIHook;
import me.kubbidev.blocktune.placeholder.PlaceholderAPIParser;
import me.kubbidev.blocktune.placeholder.PlaceholderParser;
import me.kubbidev.nexuspowered.Commands;
import me.kubbidev.nexuspowered.command.argument.Argument;
import me.kubbidev.nexuspowered.plugin.ExtendedJavaPlugin;
import me.kubbidev.nexuspowered.util.Players;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

import static me.kubbidev.spellcaster.InternalMethod.getAttributeValue;
import static me.kubbidev.spellcaster.InternalMethod.heal;

public final class BlockTune extends ExtendedJavaPlugin {
    // init during enable
    private PlaceholderParser placeholderParser;
    private ScoreboardManager scoreboardManager;

    private AttackActionListener actionListener;

    @Override
    public void load() {
    }

    @Override
    public void enable() {
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

        // register listeners
        registerPlatformListeners();

        // register with the BlockTune API
        BlockTuneProvider.register(this);
        registerApiOnPlatform();

        Commands.create().assertOp().assertPlayer()
                .handler(context -> {
                    Player player = context.sender();
                    heal(player, getAttributeValue(player, Attribute.GENERIC_MAX_HEALTH));
                })
                .registerAndBind(this, "heal");

        Commands.create().assertOp().assertPlayer()
                .handler(context -> context.sender().setFoodLevel(20))
                .registerAndBind(this, "feed");

        Commands.create()
                .assertPlayer()
                .handler(context -> {
                    Argument argumentAmount = context.arg(0);
                    int amount = 1;
                    if (argumentAmount.isPresent()) {
                        amount = argumentAmount.parseOrFail(Integer.class);
                    }

                    Argument argumentAttackSpeed = context.arg(1);
                    int attackSpeed = new Random().nextInt(10, 61);
                    if (argumentAttackSpeed.isPresent()) {
                        attackSpeed = argumentAttackSpeed.parseOrFail(Integer.class);
                    }

                    for (int i = 0; i < amount; i++) {
                        TanjiroEntity entity = new TanjiroEntity(this, context.sender().getLocation());
                        entity.setAttackSpeed(attackSpeed);
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

    private void registerPlatformListeners() {
        registerListener(this.scoreboardManager);
        registerListener(this.actionListener);
    }

    private void registerApiOnPlatform() {
        provideService(BlockTune.class, this);
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
}