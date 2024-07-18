package me.kubbidev.blocktune.config.generic.adapter;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.config.ConfigKeys;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class EnvironmentVariableConfigAdapter extends StringBasedConfigurationAdapter {
    private static final String PREFIX = "BLOCKTUNE_";

    private final BlockTune plugin;

    public EnvironmentVariableConfigAdapter(BlockTune plugin) {
        this.plugin = plugin;
    }

    @Override
    protected @Nullable String resolveValue(String path) {
        // e.g.
        // 'server'            -> BLOCKTUNE_SERVER
        // 'data.table_prefix' -> BLOCKTUNE_DATA_TABLE_PREFIX
        String key = PREFIX + path.toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_');

        String value = System.getenv(key);
        if (value != null) {
            String printableValue = ConfigKeys.shouldCensorValue(path) ? "*****" : value;
            this.plugin.getLogger().info(String.format("Resolved configuration value from environment variable: %s = %s", key, printableValue));
        }
        return value;
    }

    @Override
    public BlockTune getPlugin() {
        return this.plugin;
    }

    @Override
    public void reload() {
        // no-op
    }
}