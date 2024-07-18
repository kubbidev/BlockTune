package me.kubbidev.blocktune.core.event.attack.fake;

import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Some plugins call "fake" Bukkit events to check for specific
 * restrictions.
 * <p>
 * The event cancellation state gives the value of such restriction.
 * <p>
 * The most common type of fake events are:
 * <br>- damage events for fighting restrictions
 * <br>- block break/place events for building/mining restrictions
 * <p>
 * The concept of fake events is much more compatible AND resilient
 * than natively implementing specific checks as long as plugins
 * that do fire fake events are handled.
 * <p>
 * Fake events are used very commonly in BlockTune and other plugins.
 * <p>
 * For instance, to check if a player is able to fight with/target another
 * entity/player before casting the damage method.
 * <p>
 * Here are some of the plugins that use fake events:
 * <br>- BlockTune
 * <br>- Any plugin that uses 0 as damage for fake damage events
 *
 * @param <E> The class of event fired.
 */
@FunctionalInterface
public interface FakeEventCaller<E extends Event> {

    boolean isFake(@NotNull E called);
}