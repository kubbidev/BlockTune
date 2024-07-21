package me.kubbidev.blocktune.core.event.skill;

import lombok.Getter;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.skill.result.SkillResult;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PreSkillCastEvent extends SkillEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final SkillResult result;
    protected boolean cancelled;

    /**
     * Called after checking that a skill can be cast by an entity
     * right before actually applying its effects.
     *
     * @param skillMeta The info of the skill being cast.
     * @param result    The skill result.
     */
    public PreSkillCastEvent(SkillMetadata skillMeta, SkillResult result) {
        super(skillMeta);
        this.result = result;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}