package me.kubbidev.blocktune.util;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public interface Unlockable {

    @NotNull
    String key();

    boolean isUnlockedByDefault();

    void whenLocked(@NotNull LivingEntity entity);

    void whenUnlocked(@NotNull LivingEntity entity);
}