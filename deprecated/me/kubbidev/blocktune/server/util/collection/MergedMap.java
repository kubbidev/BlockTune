package me.kubbidev.blocktune.server.util.collection;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ApiStatus.Internal
public final class MergedMap<K, V> extends AbstractMap<K, V> {
    private final Map<K, V> first, second;

    public MergedMap(Map<K, V> first, Map<K, V> second) {
        this.first = Objects.requireNonNull(first);
        this.second = Objects.requireNonNull(second);
    }

    // mandatory methods

    final Set<Entry<K, V>> entrySet = new AbstractSet<>() {
        @Override
        public @NotNull Iterator<Map.Entry<K, V>> iterator() {
            return stream().iterator();
        }

        @SuppressWarnings("ReplaceInefficientStreamCount")
        @Override
        public int size() {
            return (int) stream().count();
        }

        @Override
        public Stream<Entry<K, V>> stream() {
            return Stream.concat(first.entrySet().stream(), secondStream())
                    .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
        }

        @Override
        public Stream<Entry<K, V>> parallelStream() {
            return stream().parallel();
        }

        @Override
        public Spliterator<Entry<K, V>> spliterator() {
            return stream().spliterator();
        }
    };

    Stream<Entry<K, V>> secondStream() {
        return this.second.entrySet().stream().filter(e -> !this.first.containsKey(e.getKey()));
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return this.entrySet;
    }

    // optimizations

    @Override
    public boolean containsKey(Object key) {
        return this.first.containsKey(key) || this.second.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.first.containsValue(value) ||
                secondStream().anyMatch(Predicate.isEqual(value));
    }

    @Override
    public V get(Object key) {
        V v = this.first.get(key);
        return v != null ? v : this.second.get(key);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V v = this.first.get(key);
        return v != null ? v : second.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        this.first.forEach(action);
        this.second.forEach((k, v) -> {
            if (!this.first.containsKey(k)) {
                action.accept(k, v);
            }
        });
    }
}