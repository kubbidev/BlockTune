package me.kubbidev.blocktune.core.skill;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.entity.EntityMetadataProvider;
import me.kubbidev.blocktune.core.skill.handler.SkillHandler;
import me.kubbidev.nexuspowered.cooldown.Cooldown;
import me.kubbidev.nexuspowered.cooldown.CooldownMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Can be used to cast a skill handler with configurable modifier input.
 */
public class SimpleSkill extends Skill {
    private final SkillHandler<?> handler;
    private final Map<String, Double> modifiers = new HashMap<>();

    public SimpleSkill(BlockTune plugin, SkillHandler<?> handler) {
        super(plugin);
        this.handler = handler;
    }

    @Override
    public boolean getResult(SkillMetadata meta) {
        return EntityMetadataProvider.retrieveCooldown(meta.entity()).testSilently(this.handler);
    }

    @Override
    public void whenCast(SkillMetadata meta) {
        long coolSeconds = Math.max((long) meta.parameter("cooldown"), 0L);

        Cooldown cooldown = Cooldown.of(coolSeconds, TimeUnit.SECONDS);
        cooldown.reset();

        CooldownMap<SkillHandler<?>> cooldownMap = EntityMetadataProvider.retrieveCooldown(meta.entity());
        cooldownMap.put(this.handler, cooldown);
    }

    @Override
    public SkillHandler<?> getHandler() {
        return this.handler;
    }

    @Override
    public double getParameter(String path) {
        return this.modifiers.getOrDefault(path, 0d);
    }

    public void registerModifier(String path, double value) {
        this.modifiers.put(path, value);
    }
}