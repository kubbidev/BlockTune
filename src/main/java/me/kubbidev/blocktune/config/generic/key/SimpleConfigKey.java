package me.kubbidev.blocktune.config.generic.key;

import lombok.Setter;
import me.kubbidev.blocktune.config.generic.adapter.ConfigurationAdapter;

import java.util.function.Function;

/**
 * Basic {@link ConfigKey} implementation.
 *
 * @param <T> the value type
 */
public class SimpleConfigKey<T> implements ConfigKey<T> {
    private final Function<? super ConfigurationAdapter, ? extends T> function;

    @Setter
    private int ordinal = -1;

    @Setter
    private boolean reloadable = true;

    SimpleConfigKey(Function<? super ConfigurationAdapter, ? extends T> function) {
        this.function = function;
    }

    @Override
    public T get(ConfigurationAdapter adapter) {
        return this.function.apply(adapter);
    }

    @Override
    public int ordinal() {
        return this.ordinal;
    }

    @Override
    public boolean reloadable() {
        return this.reloadable;
    }

}