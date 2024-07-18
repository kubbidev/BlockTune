package me.kubbidev.blocktune.core.skill;

import lombok.Getter;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.event.skill.PostSkillCastEvent;
import me.kubbidev.blocktune.core.event.skill.PreSkillCastEvent;
import me.kubbidev.blocktune.core.skill.handler.SkillHandler;
import me.kubbidev.blocktune.core.skill.result.SkillResult;
import me.kubbidev.blocktune.core.skill.trigger.TriggerMetadata;
import org.bukkit.entity.LivingEntity;

@Getter
public abstract class Skill {
    private final BlockTune plugin;

    public Skill(BlockTune plugin) {
        this.plugin = plugin;
    }

    public SkillResult cast(LivingEntity caster) {
        return cast(new TriggerMetadata(caster));
    }

    public SkillResult cast(TriggerMetadata triggerMeta) {
        return cast(triggerMeta.toSkillMetadata(this));
    }

    @SuppressWarnings("unchecked")
    public <T extends SkillResult> SkillResult cast(SkillMetadata meta) {
        SkillHandler<T> handler = (SkillHandler<T>) getHandler();

        // lower level skill restrictions
        T result = handler.getResult(meta);
        if (!result.isSuccessful()) return result;

        // high level skill restrictions
        if (!getResult(meta)) return result;

        // call first bukkit event
        PreSkillCastEvent called = new PreSkillCastEvent(meta, result);
        if (!called.callEvent()) {
            return result;
        }

        // if the delay is null we cast normally the skill
        int delayTicks = (int) (meta.parameter("delay") * 20);
        if (delayTicks <= 0) {
            castInstantly(meta, result);
        }
        // TODO: implement delayed casting
        return result;
    }

    /**
     * Called when the casting delay (potentially zero) is passed.
     * <p>
     * This does not call {@link PreSkillCastEvent} and does not
     * check for both high & low level skill conditions.
     * <p>
     * This method however calls {@link PostSkillCastEvent} after skill casting.
     */
    @SuppressWarnings("unchecked")
    public <T extends SkillResult> void castInstantly(SkillMetadata meta, T result) {
        // high level skill effects
        whenCast(meta);

        // lower level skill effects
        SkillHandler<T> handler = (SkillHandler<T>) getHandler();
        handler.whenCast(result, meta);

        // call second bukkit event
        PostSkillCastEvent called = new PostSkillCastEvent(meta, result);
        called.callEvent();
    }

    /**
     * This method should be used to check for resource costs
     * or other skill limitations.
     * <p>
     * Runs last after {@link SkillHandler#getResult(SkillMetadata)}.
     *
     * @param meta the info of skill being cast.
     * @return True if the skill can be cast, otherwise false
     */
    public abstract boolean getResult(SkillMetadata meta);

    /**
     * This is not where the actual skill effects are applied.
     * <p>
     * This method should be used to handle resource costs or
     * cooldown messages if required.
     * <p>
     * Runs first before {@link SkillHandler#whenCast(SkillResult, SkillMetadata)}.
     *
     * @param meta The info of skill being cast.
     */
    public abstract void whenCast(SkillMetadata meta);

    /**
     * Gets the {@link SkillHandler} containing all effects used to
     * to be applied on skill casting.
     *
     * @return The handler instance of this skill.
     */
    public abstract SkillHandler<?> getHandler();

    /**
     * !! WARNING !! Final skill parameter values also depend
     * on the entity's skill modifiers, and this method does NOT
     * take them into account.
     *
     * @param path The modifier name.
     * @return The skill parameter value unaffected by skill modifiers.
     * @see SkillMetadata#parameter(String)
     */
    public abstract double getParameter(String path);
}