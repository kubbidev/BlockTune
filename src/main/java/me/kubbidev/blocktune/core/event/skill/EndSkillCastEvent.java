package me.kubbidev.blocktune.core.event.skill;

import me.kubbidev.blocktune.core.skill.SkillMetadata;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EndSkillCastEvent extends SkillEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public EndSkillCastEvent(SkillMetadata skillMeta) {
        super(skillMeta);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
