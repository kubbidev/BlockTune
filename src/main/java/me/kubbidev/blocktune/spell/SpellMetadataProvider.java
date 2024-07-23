package me.kubbidev.blocktune.spell;

import com.google.common.reflect.TypeToken;
import me.kubbidev.nexuspowered.metadata.Metadata;
import me.kubbidev.nexuspowered.metadata.MetadataKey;
import me.kubbidev.spellcaster.spell.SpellMetadata;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Experimental
@ApiStatus.Internal
public final class SpellMetadataProvider {
    private SpellMetadataProvider() {
    }

    /**
     * Metadata key used to retrieve {@link org.bukkit.entity.LivingEntity} currently casted spells from memory.
     */
    private static final MetadataKey<List<String>> CURRENTLY_CASTED = MetadataKey.create("currently_casted", new TypeToken<>() {
    });

    /**
     * Gets the provided {@link LivingEntity}'s currently casted spells list associated to him.
     *
     * @param entity The entity owning the list.
     * @return currently casted spells list or new instance if not found
     */
    private static List<String> retrieveCurrentlyCasted(LivingEntity entity) {
        return Metadata.provide(entity).getOrPut(CURRENTLY_CASTED, ArrayList::new);
    }

    public static void onCastStart(SpellMetadata meta) {
        synchronized (CURRENTLY_CASTED) {
            retrieveCurrentlyCasted(meta.entity()).add(meta.cast().getHandler().getId());
        }
    }

    public static void onCastEnd(SpellMetadata meta) {
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
