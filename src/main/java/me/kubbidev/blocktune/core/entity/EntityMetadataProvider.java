package me.kubbidev.blocktune.core.entity;

import com.google.common.reflect.TypeToken;
import me.kubbidev.blocktune.core.entity.skillmod.SkillModifierMap;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.skill.handler.SkillHandler;
import me.kubbidev.nexuspowered.cooldown.CooldownMap;
import me.kubbidev.nexuspowered.metadata.Metadata;
import me.kubbidev.nexuspowered.metadata.MetadataKey;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Experimental
@ApiStatus.Internal
public final class EntityMetadataProvider {
    private EntityMetadataProvider() {
    }

    /**
     * Metadata key used to retrieve {@link LivingEntity} cooldown map from memory.
     */
    private static final MetadataKey<CooldownMap<SkillHandler<?>>> COOLDOWN_MAP = MetadataKey.create("cooldown_map", new TypeToken<>() {
    });

    /**
     * Gets the provided {@link LivingEntity}'s cooldown map associated to him.
     *
     * @param entity The entity owning the map.
     * @return cooldown map or new instance if not found
     */
    public static CooldownMap<SkillHandler<?>> retrieveCooldown(LivingEntity entity) {
        return Metadata.provide(entity).getOrPut(COOLDOWN_MAP, CooldownMap::create);
    }

    /**
     * Metadata key used to retrieve {@link org.bukkit.entity.LivingEntity} skill modifiers map from memory.
     */
    private static final MetadataKey<SkillModifierMap> SKILL_MODIFIER_MAP = MetadataKey.create("skill_modifier_map", SkillModifierMap.class);

    /**
     * Gets the provided {@link LivingEntity}'s skill modifier map associated to him.
     *
     * @param entity The entity owning the map.
     * @return skill modifier map or new instance if not found
     */
    public static SkillModifierMap retrieveModifier(LivingEntity entity) {
        return Metadata.provide(entity).getOrPut(SKILL_MODIFIER_MAP, () -> new SkillModifierMap(entity));
    }

    /**
     * Metadata key used to retrieve {@link org.bukkit.entity.LivingEntity} currently casted skills from memory.
     */
    private static final MetadataKey<List<String>> CURRENTLY_CASTED = MetadataKey.create("currently_casted", new TypeToken<>() {
    });

    /**
     * Gets the provided {@link LivingEntity}'s currently casted skills list associated to him.
     *
     * @param entity The entity owning the list.
     * @return currently casted skills list or new instance if not found
     */
    private static List<String> retrieveCurrentlyCasted(LivingEntity entity) {
        return Metadata.provide(entity).getOrPut(CURRENTLY_CASTED, ArrayList::new);
    }

    public static void onCastStart(SkillMetadata meta) {
        synchronized (CURRENTLY_CASTED) {
            retrieveCurrentlyCasted(meta.entity()).add(meta.cast().getHandler().getId());
        }
    }

    public static void onCastEnd(SkillMetadata meta) {
        synchronized (CURRENTLY_CASTED) {
            retrieveCurrentlyCasted(meta.entity()).remove(meta.cast().getHandler().getId());
        }
    }

    public static boolean isCasting(LivingEntity entity) {
        synchronized (CURRENTLY_CASTED) {
            return !retrieveCurrentlyCasted(entity).isEmpty();
        }
    }
}
