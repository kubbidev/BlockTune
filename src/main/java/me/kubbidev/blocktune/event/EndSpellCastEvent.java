package me.kubbidev.blocktune.event;

import me.kubbidev.spellcaster.event.spell.SpellEvent;
import me.kubbidev.spellcaster.spell.SpellMetadata;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EndSpellCastEvent extends SpellEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public EndSpellCastEvent(SpellMetadata spellMeta) {
        super(spellMeta);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
