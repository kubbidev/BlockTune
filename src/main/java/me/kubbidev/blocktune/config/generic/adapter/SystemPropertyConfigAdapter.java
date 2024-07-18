package me.kubbidev.blocktune.config.generic.adapter;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.config.ConfigKeys;
import org.jetbrains.annotations.Nullable;

public class SystemPropertyConfigAdapter extends StringBasedConfigurationAdapter {
    private static final String PREFIX = "blocktune.";

    private final BlockTune plugin;

    public SystemPropertyConfigAdapter(BlockTune plugin) {
        this.plugin = plugin;
    }

    @Override
    protected @Nullable String resolveValue(String path) {
        // e.g.
        // 'server'            -> blocktune.server
        // 'data.table_prefix' -> blocktune.data.table-prefix
        String key = PREFIX + path;

        String value = System.getProperty(key);
        if (value != null) {
            String printableValue = ConfigKeys.shouldCensorValue(path) ? "*****" : value;
            this.plugin.getLogger().info(String.format("Resolved configuration value from system property: %s = %s", key, printableValue));
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