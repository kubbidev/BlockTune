package me.kubbidev.blocktune.config;

import me.kubbidev.blocktune.config.generic.KeyedConfiguration;
import me.kubbidev.blocktune.config.generic.key.ConfigKey;
import me.kubbidev.blocktune.config.generic.key.SimpleConfigKey;
import me.kubbidev.blocktune.core.interaction.InteractionRulesImpl;
import me.kubbidev.blocktune.core.interaction.InteractionRules;
import me.kubbidev.blocktune.core.interaction.InteractionType;
import me.kubbidev.blocktune.core.interaction.relation.Relationship;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static me.kubbidev.blocktune.config.generic.key.ConfigKeyFactory.*;

/**
 * All of the {@link ConfigKey}s used by BlockTune.
 *
 * <p>The {@link #getKeys()} method and associated behaviour allows this class
 * to function a bit like an enum, but with generics.</p>
 */
@SuppressWarnings("CodeBlock2Expr")
public final class ConfigKeys {
    private ConfigKeys() {
    }

    /**
     * Main number formatting separator symbol used in every decimal formatter across the plugin.
     */
    public static final ConfigKey<DecimalFormatSymbols> DECIMAL_FORMAT_SEPARATOR = notReloadable(key(c -> {
        Function<String, Character> getFirstChar = string -> {
            return string == null || string.isEmpty() ? '.' : string.charAt(0);
        };

        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.ROOT);
        decimalFormatSymbols.setDecimalSeparator(getFirstChar.apply(c.getString("decimal-format-separator", ".")));
        return decimalFormatSymbols;
    }));

    /**
     * Whether or not BlockTune should display damage indicators.
     */
    public static final ConfigKey<Boolean> INDICATOR_DAMAGE_ENABLED = booleanKey("game-indicators.damage.enabled", true);

    /**
     * Whether or not BlockTune should display regeneration indicators.
     */
    public static final ConfigKey<Boolean> INDICATOR_REGENERATION_ENABLED = booleanKey("game-indicators.regeneration.enabled", true);

    /**
     * If BlockTune should applied specific rules on entities actions when casting skills or damaging others.
     */
    public static final ConfigKey<InteractionRules> INTERACTION_RULES = key(c -> {
        boolean isEnabled = c.getBoolean("interaction-rules.enabled", true);
        return isEnabled ? new InteractionRulesImpl(c) : new InteractionRules() {
            @Override
            public boolean isSupportSkillsOnMobs() {
                return true;
            }

            @Override
            public boolean isEnabled(@NotNull InteractionType interaction, @NotNull Relationship relationship, boolean pvp) {
                return true;
            }
        };
    });

    /**
     * A list of the keys defined in this class.
     */
    private static final List<SimpleConfigKey<?>> KEYS = KeyedConfiguration.initialise(ConfigKeys.class);

    public static List<? extends ConfigKey<?>> getKeys() {
        return KEYS;
    }

    /**
     * Check if the value at the given path should be censored in console/log output
     *
     * @param path the path
     * @return true if the value should be censored
     */
    public static boolean shouldCensorValue(final String path) {
        final String lower = path.toLowerCase(Locale.ROOT);
        return lower.contains("password") || lower.contains("uri");
    }
}