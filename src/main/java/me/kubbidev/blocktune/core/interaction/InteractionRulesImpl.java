package me.kubbidev.blocktune.core.interaction;

import me.kubbidev.blocktune.config.generic.adapter.ConfigurationAdapter;
import me.kubbidev.blocktune.core.interaction.relation.Relationship;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class InteractionRulesImpl implements InteractionRules {
    private final ConfigurationAdapter adapter;
    private final Map<InteractionRule, Boolean> interactionRules = new HashMap<>();

    public final boolean supportSkillsOnMobs;

    public InteractionRulesImpl(@NotNull ConfigurationAdapter adapter) {
        this.adapter = adapter;
        this.supportSkillsOnMobs = adapter.getBoolean("interaction-rules.support-skills-on-mobs", true);

        for (Relationship relationship : Relationship.values()) {
            addInteractionRule(relationship, true, true);
            addInteractionRule(relationship, false, true);
            addInteractionRule(relationship, true, false);
            addInteractionRule(relationship, false, false);

            if (relationship != Relationship.SELF) {
                this.interactionRules.put(new InteractionRule(relationship, true, false), false);
            }
        }
        this.interactionRules.put(new InteractionRule(Relationship.PARTY_OTHER, true, true), true);
        this.interactionRules.put(new InteractionRule(Relationship.GUILD_ENEMY, true, true), true);
    }

    @Override
    public boolean isSupportSkillsOnMobs() {
        return this.supportSkillsOnMobs;
    }

    @Override
    public boolean isEnabled(@NotNull InteractionType interaction, @NotNull Relationship relationship, boolean pvp) {
        return this.interactionRules.getOrDefault(buildInteractionRule(relationship, interaction.isOffense(), pvp), true);
    }

    @NotNull
    private InteractionRule buildInteractionRule(@NotNull Relationship relationship, boolean offensive, boolean pvp) {
        return new InteractionRule(relationship, offensive, pvp);
    }

    private void addInteractionRule(@NotNull Relationship relationship, boolean offensive, boolean pvp) {
        String interactionPath = "interaction-rules." + (pvp ? "pvp-on." : "pvp-off.") + (offensive ? "offense." : "support.")
                + relationship.name().toLowerCase(Locale.ROOT)
                .replace("_", "-")
                .replace(" ", "-");

        this.interactionRules.put(buildInteractionRule(relationship, offensive, pvp),
                this.adapter.getBoolean(interactionPath, true)
        );
    }

    /**
     * Represent a interaction rule ready to be evaluate.
     *
     * @param relationship The entity relationship
     * @param offensive    If the interaction type is offensive, false otherwise
     * @param pvp          If the PvP is enabled at a specific location
     */
    private record InteractionRule(@NotNull Relationship relationship, boolean offensive, boolean pvp) {

    }
}
