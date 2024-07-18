package me.kubbidev.blocktune.core.skill.handler;

import lombok.Getter;
import me.kubbidev.blocktune.core.UtilityMethod;
import me.kubbidev.blocktune.core.skill.Skill;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.skill.result.SkillResult;

import java.util.*;

/**
 * {@link SkillHandler} are skills subtracted from all of there data.
 *
 * @param <T> Skill result class being used by that skill behaviour
 */
@Getter
public abstract class SkillHandler<T extends SkillResult> {
    private final String id;
    private final Set<String> parameters = new HashSet<>();

    /**
     * Global random number generator used throughout the class.
     */
    protected static final Random random = new Random();

    /**
     * Used by default skill handlers.
     */
    public SkillHandler() {
        this.id = UtilityMethod.convertToKebabCase(getClass().getSimpleName())
                .toLowerCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");

        registerParameters("cooldown", "mana", "stamina", "timer", "delay");
    }

    /**
     * Used by default skill handlers.
     *
     * @param id The skill handler identifier
     */
    public SkillHandler(String id) {
        this.id = id.toLowerCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");

        registerParameters("cooldown", "mana", "stamina", "timer", "delay");
    }

    public void registerParameters(String... params) {
        registerParameters(Arrays.asList(params));
    }

    public void registerParameters(Collection<String> params) {
        this.parameters.addAll(params);
    }


    /**
     * Gets the skill result used to check if the skill can be cast.
     * <p>
     * This method evaluates custom conditions, checks if the caster has an entity
     * in their line of sight, if he is on the ground...
     * <p>
     * Runs first before {@link Skill#getResult(SkillMetadata)}
     *
     * @param meta The info of skill being cast.
     * @return A skill result
     */
    public abstract T getResult(SkillMetadata meta);

    /**
     * This is where the actual skill effects are applied.
     * <p>
     * Runs last, after {@link Skill#whenCast(SkillMetadata)}
     *
     * @param result The skill result.
     * @param meta   The info of skill being cast.
     */
    public abstract void whenCast(T result, SkillMetadata meta);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SkillHandler<?> other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}