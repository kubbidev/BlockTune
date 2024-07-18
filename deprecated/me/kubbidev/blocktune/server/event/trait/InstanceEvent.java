package me.kubbidev.blocktune.server.event.trait;

import me.kubbidev.blocktune.server.instance.Instance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Represents any event targeting an {@link Instance}.
 */
public abstract class InstanceEvent extends Event {
    private final Instance instance;

    public InstanceEvent(@NotNull Instance instance, boolean isAsync) {
        super(isAsync);
        this.instance = instance;
    }

    public InstanceEvent(@NotNull Instance instance) {
        this(instance, false);
    }

    /**
     * Gets the instance.
     *
     * @return instance
     */
    @NotNull
    public Instance getInstance() {
        return this.instance;
    }
}