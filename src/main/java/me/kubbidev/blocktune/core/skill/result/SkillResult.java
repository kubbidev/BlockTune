package me.kubbidev.blocktune.core.skill.result;

/**
 * When the player tries to cast a skill a skill result instance is created.
 * <p>
 * This instance determines if that skill can be cast under
 * the circumstances provided by the SkillMetadata.
 */
@FunctionalInterface
public interface SkillResult {

    /**
     * @return True if the ability was cast successfully.
     * <p>
     * This method is used to apply extra ability conditions
     * (player must be on the ground, must look at an entity...)
     * @implNote Any calculation should be ideally made in the constructor,
     * or in the worst case, cached as to minimize the impact on performance of this method.
     * <p>
     * For this reason, no instance of metadata is provided as parameter.
     */
    boolean isSuccessful();
}