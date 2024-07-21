package me.kubbidev.blocktune.core.event.skill;

import lombok.Getter;
import me.kubbidev.blocktune.core.event.LivingEntityEvent;
import me.kubbidev.blocktune.core.skill.Skill;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class SkillEvent extends LivingEntityEvent {
    private final SkillMetadata skillMeta;

    public SkillEvent(SkillMetadata skillMeta) {
        super(skillMeta.entity());
        this.skillMeta = skillMeta;
    }

    @NotNull
    public Skill getSkill() {
        return this.skillMeta.cast();
    }
}