package me.kubbidev.blocktune.core.skill;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.skill.handler.SkillHandler;
import me.kubbidev.blocktune.core.skill.handler.def.*;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum Ability {
    DANCE(new Dance(), ImmutableMap.<String, Double>builder()
            .put("damage", 4.0)
            .put("radius", 3.0)
            .put("knockback", 1.0)
            .put("repulsion", 1.0)
            .put("cooldown", 3.0)
            .build()),

    CLEAR_BLUE_SKY(new ClearBlueSky(), ImmutableMap.<String, Double>builder()
            .put("damage", 6.0)
            .put("radius", 3.0)
            .put("knockback", 1.0)
            .put("repulsion", 1.0)
            .put("cooldown", 6.0)
            .build()),

    RAGING_SUN(new RagingSun(), ImmutableMap.<String, Double>builder()
            .put("damage", 4.5)
            .put("radius", 3.0)
            .put("knockback", 1.0)
            .put("repulsion", 1.0)
            .put("cooldown", 4.0)
            .build()),

    BURNING_BONES_SUMMER_SUN(new BurningBonesSummerSun(), ImmutableMap.<String, Double>builder()
            .put("damage", 4.0)
            .put("radius", 3.0)
            .put("knockback", 1.5)
            .put("repulsion", 2.0)
            .put("cooldown", 4.0)
            .build()),

    SETTING_SUN_TRANSFORMATION(new SettingSunTransformation(), ImmutableMap.<String, Double>builder()
            .put("damage", 5.0)
            .put("radius", 4.0)
            .put("knockback", 0.75)
            .put("repulsion", 1.0)
            .put("cooldown", 4.0)
            .build()),

    SOLAR_HEAT_HAZE(new SolarHeatHaze(), ImmutableMap.<String, Double>builder()
            .put("damage", 4.5)
            .put("radius", 3.0)
            .put("knockback", 1.0)
            .put("repulsion", 1.0)
            .put("cooldown", 4.0)
            .build()),

    SUN_HALO_DRAGON_HEAD_DANCE(new SunHaloDragonHeadDance(), ImmutableMap.<String, Double>builder()
            .put("damage", 4.5)
            .put("radius", 4.0)
            .put("knockback", 1.0)
            .put("repulsion", 1.0)
            .put("cooldown", 7.0)
            .build());

    /**
     * The instance that handles all effects and behavior of the skill.
     * <p>
     * This handler encapsulates the logic required to apply the skill's effects,
     * ensuring that the skill behaves as intended when cast by an entity.
     */
    private final SkillHandler<?> handler;

    /**
     * The default parameter values used to cast the skill.
     * <p>
     * These parameters define the core attributes of the skill, they are used to
     * initialize the skill with consistent, balanced values across different uses.
     */
    private final Map<String, Double> parameters;

    /**
     * Converts this enumeration into a {@link Skill} that can be
     * utilized by the plugin.
     * <p>
     * This method creates a new {@link SimpleSkill} instance,
     * associates it with the appropriate handler, and registers the default parameters.
     * <p>
     * This method ensures that the skill is properly configured and ready for use,
     * leveraging the predefined settings encapsulated in the enum.
     * </p>
     *
     * @param plugin The {@link BlockTune} instance
     * @return A fully configured {@link Skill} ready to be used
     */
    public Skill toSkill(BlockTune plugin) {
        SimpleSkill skill = new SimpleSkill(plugin, this.handler);

        // register all predefined parameters to the skill instance
        this.parameters.forEach(skill::registerModifier);
        return skill;
    }
}