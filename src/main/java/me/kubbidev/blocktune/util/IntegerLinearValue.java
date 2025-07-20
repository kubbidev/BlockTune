package me.kubbidev.blocktune.util;

import me.kubbidev.spellcaster.manager.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class IntegerLinearValue extends LinearValue {

    public IntegerLinearValue(double base, double perLevel) {
        super(base, perLevel);
    }

    public IntegerLinearValue(double base, double perLevel, double min, double max) {
        super(base, perLevel, min, max);
    }

    public IntegerLinearValue(@NotNull IntegerLinearValue value) {
        super(value);
    }

    public IntegerLinearValue(@NotNull ConfigurationSection config) {
        super(config);
    }

    @Override
    public @NotNull String getDisplay(@NotNull ConfigManager configuration, int level) {
        return String.valueOf((int) calculate(level));
    }
}