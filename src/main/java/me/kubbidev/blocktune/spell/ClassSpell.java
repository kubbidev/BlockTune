package me.kubbidev.blocktune.spell;

import com.google.common.base.Preconditions;
import me.kubbidev.blocktune.config.ConfigKeys;
import me.kubbidev.blocktune.util.IntegerLinearValue;
import me.kubbidev.blocktune.util.LinearValue;
import me.kubbidev.blocktune.util.Placeholders;
import me.kubbidev.blocktune.util.Unlockable;
import me.kubbidev.nexuspowered.util.ImmutableCollectors;
import me.kubbidev.spellcaster.entity.EntityMetadataProvider;
import me.kubbidev.spellcaster.entity.spell.PassiveSpell;
import me.kubbidev.spellcaster.entity.spellmod.SpellModifierMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ClassSpell implements Unlockable {
    private final RegisteredSpell spell;

    private final int unlockLevel;
    private final int maxSkillLevel;

    private final boolean unlockedByDefault;
    private final boolean permanent;
    private final boolean upgradable;

    private final Map<String, LinearValue> parameters = new HashMap<>();

    public ClassSpell(@NotNull RegisteredSpell spell, int unlockLevel, int maxSkillLevel) {
        this(spell, unlockLevel, maxSkillLevel, true);
    }

    public ClassSpell(@NotNull RegisteredSpell spell, int unlockLevel, int maxSkillLevel, boolean unlockedByDefault) {
        this(spell, unlockLevel, maxSkillLevel, unlockedByDefault, spell.getPlugin().getConfiguration().get(ConfigKeys.PASSIVE_SPELLS_NEED_BOUND));
    }

    public ClassSpell(@NotNull RegisteredSpell spell, int unlockLevel, int maxSkillLevel, boolean unlockedByDefault, boolean needsBinding) {
        this(spell, unlockLevel, maxSkillLevel, unlockedByDefault, needsBinding, true);
    }

    /**
     * Class used to save information about spells IN A PROFESS CONTEXT
     * i.e at which level the {@link me.kubbidev.spellcaster.spell.Spell} can be unlocked, etc.
     * <p>
     * This constructor can be used by other plugins to register class
     * spells directly without the use of class config files.
     */
    public ClassSpell(RegisteredSpell spell, int unlockLevel, int maxSkillLevel, boolean unlockedByDefault, boolean permanent, boolean upgradable) {
        this.spell = spell;
        this.unlockLevel = unlockLevel;
        this.maxSkillLevel = maxSkillLevel;
        this.unlockedByDefault = unlockedByDefault;
        this.permanent = permanent;
        this.upgradable = upgradable;

        for (String param : spell.getHandler().getParameters()) {
            this.parameters.put(param, spell.getParameter(param));
        }
    }

    public @NotNull RegisteredSpell getSpell() {
        return this.spell;
    }

    public int getUnlockLevel() {
        return this.unlockLevel;
    }

    public boolean hasMaxLevel() {
        return this.maxSkillLevel > 0;
    }

    public int getMaxSkillLevel() {
        return this.maxSkillLevel;
    }

    @Override
    public @NotNull String key() {
        return "spell:" + this.spell.getHandler().getId().toLowerCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");
    }

    @Override
    public boolean isUnlockedByDefault() {
        return this.unlockedByDefault;
    }

    @Override
    public void whenLocked(@NotNull LivingEntity entity) {

    }

    @Override
    public void whenUnlocked(@NotNull LivingEntity entity) {

    }

    public boolean isPermanent() {
        return this.permanent;
    }

    public boolean isUpgradable() {
        return this.upgradable;
    }

    public void addParameter(@NotNull String parameter, @NotNull LinearValue value) {
        Preconditions.checkArgument(this.parameters.containsKey(parameter), "Could not find parameter '" + parameter + "'");
        this.parameters.put(parameter, value);
    }

    public double getParameter(@NotNull String parameter, int level) {
        return Objects.requireNonNull(this.parameters.get(parameter), "Could not find parameter '" + parameter + "'").calculate(level);
    }

    public @NotNull List<Component> calculateLore(@NotNull Player player, int level) {
        SpellModifierMap modifierMap = EntityMetadataProvider.getSpellModifierMap(player);
        Placeholders placeholders = new Placeholders(this.spell.getPlugin());

        this.parameters.keySet().forEach(string -> {
            double calculatedValue = modifierMap.calculateValue(
                    this.spell.getHandler(), getParameter(string, level), string
            );
            String formattedValue = this.spell.getDecimalFormat(string).format(calculatedValue);
            placeholders.register(string, formattedValue);
        });

        return this.spell.getLore().stream()
                .map(s -> placeholders.apply(player, s))
                .collect(ImmutableCollectors.toList());
    }

    private @NotNull LinearValue readLinearValue(@NotNull LinearValue current, @NotNull ConfigurationSection config) {
        return current instanceof IntegerLinearValue ? new IntegerLinearValue(config) : new LinearValue(config);
    }

    public @NotNull PassiveSpell toPassive(@NotNull LivingEntity entity) {
        Preconditions.checkArgument(this.spell.getTrigger().isPassive(), "Spell is active");
        return new PassiveSpell()
    }
}
