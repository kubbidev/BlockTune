package me.kubbidev.blocktune.spell;

import com.google.common.collect.ImmutableMap;
import me.kubbidev.blocktune.spell.handler.def.*;
import me.kubbidev.spellcaster.SpellCasterProvider;
import me.kubbidev.spellcaster.spell.SimpleSpell;
import me.kubbidev.spellcaster.spell.Spell;
import me.kubbidev.spellcaster.spell.handler.SpellHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public enum Ability {
    DANCE(new Dance(), ImmutableMap.<String, Double>builder()
            .put("damage", 24.0)
            .put("radius", 3.0)
            .put("knockback", 1.0)
            .put("repulsion", 1.0)
            .put("cooldown", 3.0)
            .build()),

    CLEAR_BLUE_SKY(new ClearBlueSky(), ImmutableMap.<String, Double>builder()
            .put("damage", 36.0)
            .put("radius", 3.0)
            .put("knockback", 1.0)
            .put("repulsion", 1.0)
            .put("cooldown", 6.0)
            .build()),

    RAGING_SUN(new RagingSun(), ImmutableMap.<String, Double>builder()
            .put("damage", 27.0)
            .put("radius", 3.0)
            .put("knockback", 1.0)
            .put("repulsion", 1.0)
            .put("cooldown", 4.0)
            .build()),

    BURNING_BONES_SUMMER_SUN(new BurningBonesSummerSun(), ImmutableMap.<String, Double>builder()
            .put("damage", 24.0)
            .put("radius", 3.0)
            .put("knockback", 1.5)
            .put("repulsion", 2.0)
            .put("cooldown", 4.0)
            .build()),

    SETTING_SUN_TRANSFORMATION(new SettingSunTransformation(), ImmutableMap.<String, Double>builder()
            .put("damage", 30.0)
            .put("radius", 4.0)
            .put("knockback", 0.75)
            .put("repulsion", 1.0)
            .put("cooldown", 4.0)
            .build()),

    SOLAR_HEAT_HAZE(new SolarHeatHaze(), ImmutableMap.<String, Double>builder()
            .put("damage", 27.0)
            .put("radius", 3.0)
            .put("knockback", 1.0)
            .put("repulsion", 1.0)
            .put("cooldown", 4.0)
            .build()),

    BENEFICENT_RADIANCE(new BeneficentRadiance(), ImmutableMap.<String, Double>builder()
            .put("damage", 24.0)
            .put("radius", 4.0)
            .put("knockback", 1.5)
            .put("repulsion", 2.0)
            .put("cooldown", 5.0)
            .build()),

    SUNFLOWER_THRUST(new SunflowerThrust(), ImmutableMap.<String, Double>builder()
            .put("damage", 30.0)
            .put("radius", 3.0)
            .put("knockback", 2.0)
            .put("repulsion", 1.0)
            .put("cooldown", 5.0)
            .build()),

    SUN_HALO_DRAGON_HEAD_DANCE(new SunHaloDragonHeadDance(), ImmutableMap.<String, Double>builder()
            .put("damage", 27.0)
            .put("radius", 4.0)
            .put("knockback", 1.0)
            .put("repulsion", 1.0)
            .put("cooldown", 7.0)
            .build()),

    FIRE_WHEEL(new FireWheel(), ImmutableMap.<String, Double>builder()
            .put("damage", 27.0)
            .put("radius", 3.0)
            .put("knockback", 1.0)
            .put("repulsion", 1.0)
            .put("cooldown", 4.0)
            .build());

    /**
     * The instance that handles all effects and behavior of the spell.
     * <p>
     * This handler encapsulates the logic required to apply the spell's effects,
     * ensuring that the spell behaves as intended when cast by an entity.
     */
    private final SpellHandler<?> handler;

    /**
     * The default parameter values used to cast the spell.
     * <p>
     * These parameters define the core attributes of the spell, they are used to
     * initialize the spell with consistent, balanced values across different uses.
     */
    private final Map<String, Double> parameters;

    Ability(SpellHandler<?> handler, Map<String, Double> parameters) {
        this.handler = handler;
        this.parameters = parameters;
    }

    public @NotNull SpellHandler<?> getHandler() {
        return this.handler;
    }

    public @NotNull Map<String, Double> getParameters() {
        return this.parameters;
    }

    /**
     * Converts this enumeration into a {@link Spell} that can be
     * utilized by the plugin.
     * <p>
     * This method creates a new {@link SimpleSpell} instance,
     * associates it with the appropriate handler, and registers the default parameters.
     * <p>
     * This method ensures that the spell is properly configured and ready for use,
     * leveraging the predefined settings encapsulated in the enum.
     * </p>
     *
     * @return A fully configured {@link Spell} ready to be used
     */
    public Spell toSpell() {
        SimpleSpell spell = new SimpleSpell(SpellCasterProvider.get(), this.handler);

        // register all predefined parameters to the spell instance
        this.parameters.forEach(spell::registerModifier);
        return spell;
    }
}