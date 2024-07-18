package me.kubbidev.blocktune.config;

import lombok.Getter;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.config.generic.KeyedConfiguration;
import me.kubbidev.blocktune.config.generic.adapter.ConfigurationAdapter;
import me.kubbidev.blocktune.event.ConfigReloadEvent;

import java.text.DecimalFormat;

@Getter
public class BlockTuneConfiguration extends KeyedConfiguration {
    private final BlockTune plugin;

    private DecimalFormat decimalFormat;
    private DecimalFormat decimalsFormat;

    public BlockTuneConfiguration(BlockTune plugin, ConfigurationAdapter adapter) {
        super(adapter, ConfigKeys.getKeys());
        this.plugin = plugin;

        init();
    }

    @Override
    protected void load(boolean initial) {
        super.load(initial);
        this.decimalFormat = formatFrom("0.#");
        this.decimalsFormat = formatFrom("0.##");
    }

    @Override
    public void reload() {
        super.reload();
        ConfigReloadEvent called = new ConfigReloadEvent(getPlugin());
        called.callEvent();
    }

    /**
     * The plugin mostly cache the return value of that method in fields
     * for easy access, therefore a server restart is required when editing the
     * decimal-separator option in the config
     *
     * @param pattern Something like "0.#"
     * @return New decimal format with the decimal separator given by the config.
     */
    public DecimalFormat formatFrom(String pattern) {
        return new DecimalFormat(pattern, get(ConfigKeys.DECIMAL_FORMAT_SEPARATOR));
    }
}
