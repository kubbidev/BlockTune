package me.kubbidev.blocktune.util;

import me.kubbidev.spellcaster.manager.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class LinearValue {

    public static final LinearValue ZERO = new LinearValue(0, 0, 0, 0);

    private final double base;
    private final double perLevel;

    private final double min;
    private final double max;

    private final boolean hasMin;
    private final boolean hasMax;

    /**
     * A number formula which depends on the entity level.
     * <p>
     * It can be used to handle skill modifiers so that the ability gets better with the skill level, or as an attribute value to make them
     * scale with the class level.
     *
     * @param base     The base value.
     * @param perLevel The value increment per level.
     */
    public LinearValue(double base, double perLevel) {
        this.base = base;
        this.perLevel = perLevel;
        this.min = 0;
        this.max = 0;
        this.hasMin = false;
        this.hasMax = false;
    }

    /**
     * A number formula which depends on the entity level.
     * <p>
     * It can be used to handle skill modifiers so that the ability gets better with the skill level, or as an attribute value to make them
     * scale with the class level.
     *
     * @param base     The base value.
     * @param perLevel The value increment per level.
     * @param min      The minimum value.
     * @param max      The maximum value.
     */
    public LinearValue(double base, double perLevel, double min, double max) {
        this.base = base;
        this.perLevel = perLevel;
        this.min = min;
        this.max = max;
        this.hasMin = true;
        this.hasMax = true;
    }

    public LinearValue(@NotNull LinearValue value) {
        this.base = value.base;
        this.perLevel = value.perLevel;
        this.min = value.min;
        this.max = value.max;
        this.hasMin = value.hasMin;
        this.hasMax = value.hasMax;
    }

    public LinearValue(@NotNull ConfigurationSection config) {
        this.base = config.getDouble("base");
        this.perLevel = config.getDouble("per-level");
        this.hasMin = config.contains("min");
        this.hasMax = config.contains("max");
        this.min = this.hasMin ? config.getDouble("min") : 0;
        this.max = this.hasMax ? config.getDouble("max") : 0;
    }

    public double getBaseValue() {
        return this.base;
    }

    public double getPerLevel() {
        return this.perLevel;
    }

    public double getMax() {
        return this.max;
    }

    public double getMin() {
        return this.min;
    }

    public boolean hasMax() {
        return this.hasMax;
    }

    public boolean hasMin() {
        return this.hasMin;
    }

    public @NotNull String getDisplay(@NotNull ConfigManager configuration, int level) {
        return configuration.getDecimalsFormat().format(calculate(level));
    }

    public double calculate(int level) {
        double value = this.base + this.perLevel * (level - 1);

        if (this.hasMin) {
            value = Math.max(this.min, value);
        }
        if (this.hasMax) {
            value = Math.min(this.max, value);
        }
        return value;
    }
}