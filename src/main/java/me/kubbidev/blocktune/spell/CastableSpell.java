package me.kubbidev.blocktune.spell;

import me.kubbidev.spellcaster.SpellCaster;
import me.kubbidev.spellcaster.spell.Spell;
import me.kubbidev.spellcaster.spell.SpellMetadata;
import me.kubbidev.spellcaster.spell.handler.SpellHandler;
import me.kubbidev.spellcaster.spell.trigger.TriggerType;
import org.jetbrains.annotations.NotNull;

public class CastableSpell extends Spell {

    public CastableSpell(@NotNull SpellCaster plugin, @NotNull TriggerType trigger) {
        super(plugin, trigger);
    }

    @Override
    public boolean getResult(SpellMetadata meta) {
        return false;
    }

    @Override
    public void whenCast(SpellMetadata meta) {

    }

    @Override
    public SpellHandler<?> getHandler() {
        return null;
    }

    @Override
    public double getParameter(String path) {
        return 0;
    }
}
