package me.kubbidev.blocktune.entity;

import me.kubbidev.blocktune.spell.Ability;
import me.kubbidev.spellcaster.SpellCasterProvider;
import me.kubbidev.spellcaster.spell.Spell;
import me.kubbidev.spellcaster.spell.SpellMetadata;
import me.kubbidev.spellcaster.spell.handler.SpellHandler;
import org.jetbrains.annotations.NotNull;

public final class SmartEntitySpell extends Spell {
    private final Ability ability;

    public SmartEntitySpell(@NotNull Ability ability) {
        super(SpellCasterProvider.get());
        this.ability = ability;
    }

    @Override
    public boolean getResult(SpellMetadata meta) {
        return true;
    }

    @Override
    public void whenCast(SpellMetadata meta) {

    }

    @Override
    public SpellHandler<?> getHandler() {
        return this.ability.getHandler();
    }

    @Override
    public double getParameter(String path) {
        return this.ability.getParameters().getOrDefault(path, 0d);
    }
}
