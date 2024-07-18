package me.kubbidev.blocktune.core.event.skill;

import lombok.Getter;
import me.kubbidev.blocktune.core.event.LivingEntityEvent;
import me.kubbidev.blocktune.core.skill.Skill;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.skill.result.SkillResult;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class SkillEvent extends LivingEntityEvent {
    private final SkillMetadata skillMeta;
    private final SkillResult result;

    public SkillEvent(SkillMetadata skillMeta, SkillResult result) {
        super(skillMeta.entity());
        this.skillMeta = skillMeta;
        this.result = result;
    }

    @NotNull
    public Skill getSkill() {
        return this.skillMeta.cast();
    }
}