package me.kubbidev.blocktune.core.event.skill;

import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.skill.result.SkillResult;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PostSkillCastEvent extends SkillEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    /**
     * Called after an entity has successfully cast a skill.
     *
     * @param skillMeta The info of the skill that has been cast.
     * @param result    The skill result.
     */
    public PostSkillCastEvent(SkillMetadata skillMeta, SkillResult result) {
        super(skillMeta, result);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}