package me.kubbidev.blocktune.manager;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.spell.RegisteredSpell;
import me.kubbidev.spellcaster.InternalMethod;
import me.kubbidev.spellcaster.spell.handler.SpellHandler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class SpellManager {
    private final BlockTune plugin;

    /**
     * All registered spell handlers accessible by any external plugins.
     */
    private final Map<String, SpellHandler<?>> handlers = new HashMap<>();

    /**
     * All registered spells with their default parameters value set.
     */
    private final Map<String, RegisteredSpell> spells = new LinkedHashMap<>();

    private boolean registration = true;

    public SpellManager(@NotNull BlockTune plugin) {
        this.plugin = plugin;
    }

    public void registerSpellHandler(@NotNull SpellHandler<?> handler) {
        Preconditions.checkArgument(this.handlers.putIfAbsent(handler.getId(), handler) == null,
                "A spell handler with the same name already exists");

        if (!this.registration && handler instanceof Listener) {
            Bukkit.getPluginManager().registerEvents((Listener) handler, this.plugin);
        }
    }

    public @NotNull SpellHandler<?> getHandlerOrThrow(String id) {
        return Objects.requireNonNull(this.handlers.get(id), "Could not find handler with ID '" + id + "'");
    }

    public @Nullable SpellHandler<?> getHandler(@NotNull String handlerId) {
        return this.handlers.get(handlerId);
    }

    /**
     * @return Currently registered spell handlers.
     */
    public @NotNull Collection<SpellHandler<?>> getHandlers() {
        return this.handlers.values();
    }

    public void registerSpell(@NotNull RegisteredSpell spell) {
        Preconditions.checkArgument(this.spells.putIfAbsent(spell.getHandler().getId(), spell) == null,
                "A spell with the same name already exists");
    }

    public @NotNull RegisteredSpell getSpellOrThrow(@NotNull String id) {
        return Objects.requireNonNull(this.spells.get(id), "Could not find spell with ID '" + id + "'");
    }

    public @Nullable RegisteredSpell getSpell(@NotNull String spellId) {
        return this.spells.get(spellId);
    }

    /**
     * @return Currently registered spells.
     */
    public @NotNull Collection<RegisteredSpell> getSpells() {
        return this.spells.values();
    }

    public void load(boolean clearBefore) {
        File spellsFile = this.plugin.getBundledFile("spell");
        if (clearBefore) {
            this.handlers.values().stream().filter(handler -> handler instanceof Listener).map(handler -> (Listener) handler)
                    .forEach(HandlerList::unregisterAll);

            this.handlers.clear();
        } else {
            this.registration = false;
        }

        for (SpellHandler<?> handler : getHandlers()) {
            File handlerFile = new File(spellsFile, handler.getId() + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(handlerFile);

            // if the spell configuration don't already exists (empty) fill it with default value
            if (!handlerFile.exists()) {
                config.set("name", InternalMethod.caseOnWords(handler.getId().toLowerCase(Locale.ROOT)
                        .replace("_", " ")
                        .replace("-", " ")));

                config.set("lore", ImmutableList.<String>builder()
                        .add("This is the default spell description")
                        .add("The description support MiniMessage!")
                        .add("")
                        .add("<cooldown>s cooldown")
                        .build());
                config.set("icon", "BOOK");
                try {
                    for (String parameter : handler.getParameters()) {
                        config.set(parameter + ".base", 0);
                        config.set(parameter + ".per-level", 0);
                        config.set(parameter + ".min", 0);
                        config.set(parameter + ".max", 0);
                    }
                    config.save(handlerFile);
                } catch (IOException e) {
                    this.plugin.getLogger().severe("Could not save " + handler.getId() + ".yml: " + e.getMessage());
                }
            }
            try {
                RegisteredSpell spell = new RegisteredSpell(this.plugin, handler, config);
                registerSpell(spell);
            } catch (RuntimeException e) {
                this.plugin.getLogger().warning("Could not load spell '" + handler.getId() + "': " + e.getMessage());
            }
        }
    }
}