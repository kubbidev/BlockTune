package me.kubbidev.blocktune.config;

import me.kubbidev.nexuspowered.config.KeyedConfiguration;
import me.kubbidev.nexuspowered.config.key.ConfigKey;
import me.kubbidev.nexuspowered.config.key.SimpleConfigKey;

import java.util.List;

import static me.kubbidev.nexuspowered.config.key.ConfigKeyFactory.booleanKey;

/**
 * All of the {@link ConfigKey}s used by BlockTune.
 *
 * <p>The {@link #getKeys()} method and associated behaviour allows this class
 * to function a bit like an enum, but with generics.</p>
 */
public final class ConfigKeys {

    private ConfigKeys() {
    }

    /**
     * When set to true, passive spells must be bound in order to take effect otherwise, unlocked spells will take effect right away.
     */
    public static final ConfigKey<Boolean> PASSIVE_SPELLS_NEED_BOUND = booleanKey("passive-spell-need-bound", true);

    /**
     * A list of the keys defined in this class.
     */
    private static final List<SimpleConfigKey<?>> KEYS = KeyedConfiguration.initialise(ConfigKeys.class);

    public static List<? extends ConfigKey<?>> getKeys() {
        return KEYS;
    }
}
