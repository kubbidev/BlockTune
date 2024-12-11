package me.kubbidev.blocktune.spell;

import com.google.common.collect.ImmutableList;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.util.LinearValue;
import me.kubbidev.nexuspowered.util.Text;
import me.kubbidev.spellcaster.InternalMethod;
import me.kubbidev.spellcaster.SpellCasterProvider;
import me.kubbidev.spellcaster.spell.handler.SpellHandler;
import me.kubbidev.spellcaster.spell.trigger.TriggerType;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.*;

public class RegisteredSpell {
    private final BlockTune plugin;
    private final SpellHandler<?> handler;

    private final Map<String, LinearValue> defaultParameters = new HashMap<>();
    private final Map<String, DecimalFormat> decimalFormats = new HashMap<>();

    private final Component name;
    private final ItemStack icon;

    private final List<String> lore;
    private final TriggerType triggerType;

    public RegisteredSpell(@NotNull BlockTune plugin, @NotNull SpellHandler<?> handler, @NotNull ConfigurationSection config) {
        this.plugin = plugin;
        this.handler = handler;

        this.name = Text.fromMiniMessage(Objects.requireNonNull(config.getString("name"), "Could not find skill name"));
        this.icon = InternalMethod.readIcon(Objects.requireNonNull(config.getString("icon"), "Could not find skill icon"));
        // raw spell lore without mini message deserialization
        this.lore = Objects.requireNonNull(config.getStringList("lore"), "Could not find skill lore");

        // trigger type
        this.triggerType = getHandler().isTriggerable()
                ? TriggerType.valueOf(config.getString("passive-type", "CAST").toUpperCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_")) : TriggerType.API;


        for (String param : handler.getParameters()) {
            ConfigurationSection section = config.getConfigurationSection(param);
            if (section == null) {
                this.defaultParameters.put(param, LinearValue.ZERO);
            } else {
                String decimalFormat = config.getString("decimal-format");
                if (decimalFormat != null) {
                    this.decimalFormats.put(param, new DecimalFormat(decimalFormat));
                }
                this.defaultParameters.put(param, new LinearValue(section));
            }
        }
    }

    public @NotNull BlockTune getPlugin() {
        return this.plugin;
    }

    public @NotNull SpellHandler<?> getHandler() {
        return this.handler;
    }

    public @NotNull Component getName() {
        return this.name;
    }

    public @NotNull List<String> getLore() {
        return ImmutableList.copyOf(this.lore);
    }

    public @NotNull ItemStack getIcon() {
        return this.icon.clone();
    }

    public @NotNull TriggerType getTrigger() {
        return Objects.requireNonNull(this.triggerType, "Spell has no trigger");
    }

    public boolean hasParameter(@NotNull String parameter) {
        return this.defaultParameters.containsKey(parameter);
    }

    public void addParameter(@NotNull String parameter, @NotNull LinearValue linear) {
        this.defaultParameters.put(parameter, linear);
    }

    public @NotNull LinearValue getParameter(@NotNull String parameter) {
        return this.defaultParameters.get(parameter);
    }

    public @NotNull DecimalFormat getDecimalFormat(@NotNull String parameter) {
        return this.decimalFormats.getOrDefault(parameter, SpellCasterProvider.get().getConfiguration().getDecimalFormat());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RegisteredSpell other)) {
            return false;
        }
        return this.handler.equals(other.handler);
    }

    @Override
    public int hashCode() {
        return this.handler.hashCode();
    }
}