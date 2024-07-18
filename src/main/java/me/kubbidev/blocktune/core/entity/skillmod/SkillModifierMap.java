package me.kubbidev.blocktune.core.entity.skillmod;

import me.kubbidev.blocktune.core.entity.modfier.ModifierMap;
import me.kubbidev.blocktune.core.entity.modfier.ModifierType;
import me.kubbidev.blocktune.core.skill.Skill;
import me.kubbidev.blocktune.core.skill.handler.SkillHandler;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class SkillModifierMap extends ModifierMap<SkillModifier> {
    public SkillModifierMap(LivingEntity entity) {
        super(entity);
    }

    public double calculateValue(@NotNull Skill cast, @NotNull String parameter) {
        return calculateValue(cast.getHandler(), cast.getParameter(parameter), parameter);
    }

    public double calculateValue(@NotNull SkillHandler<?> skill, double base, @NotNull String parameter) {
        for (SkillModifier mod : getModifiers())
            if (mod.getType() == ModifierType.FLAT
                    && mod.getParameter().equals(parameter)
                    && mod.getSkills().contains(skill)) {
                base += mod.getValue();
            }

        for (SkillModifier mod : getModifiers())
            if (mod.getType() == ModifierType.RELATIVE
                    && mod.getParameter().equals(parameter)
                    && mod.getSkills().contains(skill)) {
                base *= 1 + mod.getValue() / 100;
            }

        return base;
    }
}