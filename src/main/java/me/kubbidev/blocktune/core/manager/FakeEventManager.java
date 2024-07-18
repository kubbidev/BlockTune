package me.kubbidev.blocktune.core.manager;

import me.kubbidev.blocktune.core.event.attack.fake.FakeEntityDamageByEntityEvent;
import me.kubbidev.blocktune.core.event.attack.fake.FakeEventCaller;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;

public final class FakeEventManager {
    private final Map<Class<?>, Collection<FakeEventCaller<?>>> callers = new HashMap<>();

    public FakeEventManager() {

        /*
        This will add compatibility with many plugins with the need
        for code-specific hooks.
        This classifies damage events with 0 damage as fake.
        These will be instantly ignored!
         */
        registerFakeEventCaller(EntityDamageEvent.class, e -> e.getDamage() == 0);

        // internal fake events
        registerFakeEventCaller(EntityDamageEvent.class, e -> e instanceof FakeEntityDamageByEntityEvent);
    }

    public <E extends Event> void registerFakeEventCaller(Class<E> eventClass, FakeEventCaller<E> caller) {
        Objects.requireNonNull(eventClass, "Event class cannot be null");
        Objects.requireNonNull(caller, "Fake event caller cannot be null");
        this.callers.computeIfAbsent(eventClass, u -> new ArrayList<>()).add(caller);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean isFake(Event event) {
        Objects.requireNonNull(event, "Event cannot be null");

        for (Map.Entry<Class<?>, Collection<FakeEventCaller<?>>> entry : this.callers.entrySet()) {
            if (entry.getKey().isInstance(entry)) {
                for (FakeEventCaller caller : entry.getValue()) if (caller.isFake(event)) return true;
            }
        }
        return false;
    }
}