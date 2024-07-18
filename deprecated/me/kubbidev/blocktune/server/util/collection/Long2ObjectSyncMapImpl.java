package me.kubbidev.blocktune.server.util.collection;

import it.unimi.dsi.fastutil.longs.AbstractLong2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.LongFunction;

import static java.util.Objects.requireNonNull;

/* package */ final class Long2ObjectSyncMapImpl<V> extends AbstractLong2ObjectMap<V> implements Long2ObjectSyncMap<V> {
    @Serial
    private static final long serialVersionUID = 1;

    /**
     * A single implicit lock when dealing with {@code dirty} mutations.
     */
    private transient final Object lock = new Object();

    /**
     * The read only map that does not require a lock and does not allow mutations.
     */
    private transient volatile Long2ObjectMap<ExpungingEntry<V>> read;

    /**
     * Represents whether the {@code dirty} map has changes the {@code read} map
     * does not have yet.
     */
    private transient volatile boolean amended;

    /**
     * The read/write map that requires a lock and allows mutations.
     */
    private transient Long2ObjectMap<ExpungingEntry<V>> dirty;

    /**
     * Represents the amount of times an attempt has been made to access the
     * {@code dirty} map while {@code amended} is {@code true}.
     */
    private transient int misses;

    private transient final IntFunction<Long2ObjectMap<ExpungingEntry<V>>> function;

    private transient EntrySetView entrySet;

    /* package */ Long2ObjectSyncMapImpl(final @NotNull IntFunction<Long2ObjectMap<ExpungingEntry<V>>> function, final int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("Initial capacity must be greater than 0");
        this.function = function;
        this.read = function.apply(initialCapacity);
    }

    // Query Operations

    @Override
    public int size() {
        this.promote();
        int size = 0;
        for (final ExpungingEntry<V> value : this.read.values()) {
            if (value.exists()) size++;
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        this.promote();
        for (final ExpungingEntry<V> value : this.read.values()) {
            if (value.exists()) return false;
        }
        return true;
    }

    @Override
    public boolean containsValue(final @Nullable Object value) {
        for (final Long2ObjectMap.Entry<V> entry : this.long2ObjectEntrySet()) {
            if (Objects.equals(entry.getValue(), value)) return true;
        }
        return false;
    }

    @Override
    public boolean containsKey(final long key) {
        final ExpungingEntry<V> entry = this.getEntry(key);
        return entry != null && entry.exists();
    }

    @Override
    public @Nullable V get(final long key) {
        final ExpungingEntry<V> entry = this.getEntry(key);
        return entry != null ? entry.get() : null;
    }

    @Override
    public @NotNull V getOrDefault(final long key, final @NotNull V defaultValue) {
        requireNonNull(defaultValue, "defaultValue");
        final ExpungingEntry<V> entry = this.getEntry(key);
        return entry != null ? entry.getOr(defaultValue) : defaultValue;
    }

    public @Nullable ExpungingEntry<V> getEntry(final long key) {
        ExpungingEntry<V> entry = this.read.get(key);
        if (entry == null && this.amended) {
            synchronized (this.lock) {
                if ((entry = this.read.get(key)) == null && this.amended && this.dirty != null) {
                    entry = this.dirty.get(key);
                    // The slow path should be avoided, even if the value does
                    // not match or is present. So we mark a miss, to eventually
                    // promote and take a faster path.
                    this.missLocked();
                }
            }
        }
        return entry;
    }

    @Override
    public @Nullable V computeIfAbsent(final long key, final @NotNull LongFunction<? extends V> mappingFunction) {
        requireNonNull(mappingFunction, "mappingFunction");
        ExpungingEntry<V> entry = this.read.get(key);
        InsertionResult<V> result = entry != null ? entry.computeIfAbsent(key, mappingFunction) : null;
        if (result != null && result.operation() == InsertionResultImpl.UPDATED) return result.current();
        synchronized (this.lock) {
            if ((entry = this.read.get(key)) != null) {
                // If the entry was expunged, unexpunge, add the entry
                // back to the dirty map.
                if (entry.tryUnexpungeAndCompute(key, mappingFunction)) {
                    if (entry.exists()) this.dirty.put(key, entry);
                    return entry.get();
                } else {
                    result = entry.computeIfAbsent(key, mappingFunction);
                }
            } else if (this.dirty != null && (entry = this.dirty.get(key)) != null) {
                result = entry.computeIfAbsent(key, mappingFunction);
                if (result.current() == null) this.dirty.remove(key);
                // The slow path should be avoided, even if the value does
                // not match or is present. So we mark a miss, to eventually
                // promote and take a faster path.
                this.missLocked();
            } else {
                if (!this.amended) {
                    // Adds the first new key to the dirty map and marks it as
                    // amended.
                    this.dirtyLocked();
                    this.amended = true;
                }
                final V computed = mappingFunction.apply(key);
                if (computed != null) this.dirty.put(key, new ExpungingEntryImpl<>(computed));
                return computed;
            }
        }
        return result.current();
    }

    @Override
    public @Nullable V computeIfAbsent(final long key, final @NotNull Long2ObjectFunction<? extends V> mappingFunction) {
        requireNonNull(mappingFunction, "mappingFunction");
        ExpungingEntry<V> entry = this.read.get(key);
        InsertionResult<V> result = entry != null ? entry.computeIfAbsentPrimitive(key, mappingFunction) : null;
        if (result != null && result.operation() == InsertionResultImpl.UPDATED) return result.current();
        synchronized (this.lock) {
            if ((entry = this.read.get(key)) != null) {
                // If the entry was expunged, unexpunge, add the entry
                // back to the dirty map.
                if (entry.tryUnexpungeAndComputePrimitive(key, mappingFunction)) {
                    if (entry.exists()) this.dirty.put(key, entry);
                    return entry.get();
                } else {
                    result = entry.computeIfAbsentPrimitive(key, mappingFunction);
                }
            } else if (this.dirty != null && (entry = this.dirty.get(key)) != null) {
                result = entry.computeIfAbsentPrimitive(key, mappingFunction);
                if (result.current() == null) this.dirty.remove(key);
                // The slow path should be avoided, even if the value does
                // not match or is present. So we mark a miss, to eventually
                // promote and take a faster path.
                this.missLocked();
            } else {
                if (!this.amended) {
                    // Adds the first new key to the dirty map and marks it as
                    // amended.
                    this.dirtyLocked();
                    this.amended = true;
                }
                final V computed = mappingFunction.get(key);
                if (computed != null) this.dirty.put(key, new ExpungingEntryImpl<>(computed));
                return computed;
            }
        }
        return result.current();
    }

    @Override
    public @Nullable V computeIfPresent(final long key, final @NotNull BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
        requireNonNull(remappingFunction, "remappingFunction");
        ExpungingEntry<V> entry = this.read.get(key);
        InsertionResult<V> result = entry != null ? entry.computeIfPresent(key, remappingFunction) : null;
        if (result != null && result.operation() == InsertionResultImpl.UPDATED) return result.current();
        synchronized (this.lock) {
            if ((entry = this.read.get(key)) != null) {
                result = entry.computeIfPresent(key, remappingFunction);
            } else if (this.dirty != null && (entry = this.dirty.get(key)) != null) {
                result = entry.computeIfPresent(key, remappingFunction);
                if (result.current() == null) this.dirty.remove(key);
                // The slow path should be avoided, even if the value does
                // not match or is present. So we mark a miss, to eventually
                // promote and take a faster path.
                this.missLocked();
            }
        }
        return result != null ? result.current() : null;
    }

    @Override
    public @Nullable V compute(final long key, final @NotNull BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
        requireNonNull(remappingFunction, "remappingFunction");
        ExpungingEntry<V> entry = this.read.get(key);
        InsertionResult<V> result = entry != null ? entry.compute(key, remappingFunction) : null;
        if (result != null && result.operation() == InsertionResultImpl.UPDATED) return result.current();
        synchronized (this.lock) {
            if ((entry = this.read.get(key)) != null) {
                // If the entry was expunged, unexpunge, add the entry
                // back to the dirty map if the value is not null.
                if (entry.tryUnexpungeAndCompute(key, remappingFunction)) {
                    if (entry.exists()) this.dirty.put(key, entry);
                    return entry.get();
                } else {
                    result = entry.compute(key, remappingFunction);
                }
            } else if (this.dirty != null && (entry = this.dirty.get(key)) != null) {
                result = entry.compute(key, remappingFunction);
                if (result.current() == null) this.dirty.remove(key);
                // The slow path should be avoided, even if the value does
                // not match or is present. So we mark a miss, to eventually
                // promote and take a faster path.
                this.missLocked();
            } else {
                if (!this.amended) {
                    // Adds the first new key to the dirty map and marks it as
                    // amended.
                    this.dirtyLocked();
                    this.amended = true;
                }
                final V computed = remappingFunction.apply(key, null);
                if (computed != null) this.dirty.put(key, new ExpungingEntryImpl<>(computed));
                return computed;
            }
        }
        return result.current();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public @Nullable V putIfAbsent(final long key, final @NotNull V value) {
        requireNonNull(value, "value");
        ExpungingEntry<V> entry = this.read.get(key);
        InsertionResult<V> result = entry != null ? entry.setIfAbsent(value) : null;
        if (result != null && result.operation() == InsertionResultImpl.UPDATED) return result.previous();
        synchronized (this.lock) {
            if ((entry = this.read.get(key)) != null) {
                // If the entry was expunged, unexpunge, add the entry
                // back to the dirty map and return null, as we know there
                // was no previous value.
                if (entry.tryUnexpungeAndSet(value)) {
                    this.dirty.put(key, entry);
                    return null;
                } else {
                    result = entry.setIfAbsent(value);
                }
            } else if (this.dirty != null && (entry = this.dirty.get(key)) != null) {
                result = entry.setIfAbsent(value);
                // The slow path should be avoided, even if the value does
                // not match or is present. So we mark a miss, to eventually
                // promote and take a faster path.
                this.missLocked();
            } else {
                if (!this.amended) {
                    // Adds the first new key to the dirty map and marks it as
                    // amended.
                    this.dirtyLocked();
                    this.amended = true;
                }
                this.dirty.put(key, new ExpungingEntryImpl<>(value));
                return null;
            }
        }
        return result.previous();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public @Nullable V put(final long key, final @NotNull V value) {
        requireNonNull(value, "value");
        ExpungingEntry<V> entry = this.read.get(key);
        V previous = entry != null ? entry.get() : null;
        if (entry != null && entry.trySet(value)) return previous;
        synchronized (this.lock) {
            if ((entry = this.read.get(key)) != null) {
                previous = entry.get();
                // If the entry was expunged, unexpunge and add the entry
                // back to the dirty map.
                if (entry.tryUnexpungeAndSet(value)) {
                    this.dirty.put(key, entry);
                } else {
                    entry.set(value);
                }
            } else if (this.dirty != null && (entry = this.dirty.get(key)) != null) {
                previous = entry.get();
                entry.set(value);
            } else {
                if (!this.amended) {
                    // Adds the first new key to the dirty map and marks it as
                    // amended.
                    this.dirtyLocked();
                    this.amended = true;
                }
                this.dirty.put(key, new ExpungingEntryImpl<>(value));
                return null;
            }
        }
        return previous;
    }

    @Override
    public @Nullable V remove(final long key) {
        ExpungingEntry<V> entry = this.read.get(key);
        if (entry == null && this.amended) {
            synchronized (this.lock) {
                if ((entry = this.read.get(key)) == null && this.amended && this.dirty != null) {
                    entry = this.dirty.remove(key);
                    // The slow path should be avoided, even if the value does
                    // not match or is present. So we mark a miss, to eventually
                    // promote and take a faster path.
                    this.missLocked();
                }
            }
        }
        return entry != null ? entry.clear() : null;
    }

    @Override
    public boolean remove(final long key, final @NotNull Object value) {
        requireNonNull(value, "value");
        ExpungingEntry<V> entry = this.read.get(key);
        if (entry == null && this.amended) {
            synchronized (this.lock) {
                if ((entry = this.read.get(key)) == null && this.amended && this.dirty != null) {
                    final boolean present = ((entry = this.dirty.get(key)) != null && entry.replace(value, null));
                    if (present) this.dirty.remove(key);
                    // The slow path should be avoided, even if the value does
                    // not match or is present. So we mark a miss, to eventually
                    // promote and take a faster path.
                    this.missLocked();
                    return present;
                }
            }
        }
        return entry != null && entry.replace(value, null);
    }

    @Override
    public @Nullable V replace(final long key, final @NotNull V value) {
        requireNonNull(value, "value");
        final ExpungingEntry<V> entry = this.getEntry(key);
        return entry != null ? entry.tryReplace(value) : null;
    }

    @Override
    public boolean replace(final long key, final @NotNull V oldValue, final @NotNull V newValue) {
        requireNonNull(oldValue, "oldValue");
        requireNonNull(newValue, "newValue");
        final ExpungingEntry<V> entry = this.getEntry(key);
        return entry != null && entry.replace(oldValue, newValue);
    }

    // Bulk Operations

    @Override
    public void forEach(final @NotNull BiConsumer<? super Long, ? super V> action) {
        requireNonNull(action, "action");
        this.promote();
        V value;
        for (final Long2ObjectMap.Entry<ExpungingEntry<V>> that : this.read.long2ObjectEntrySet()) {
            if ((value = that.getValue().get()) != null) {
                action.accept(that.getLongKey(), value);
            }
        }
    }

    @Override
    public void putAll(final @NotNull Map<? extends Long, ? extends V> map) {
        requireNonNull(map, "map");
        for (final Map.Entry<? extends Long, ? extends V> entry : map.entrySet()) {
            this.put((long) entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void replaceAll(final @NotNull BiFunction<? super Long, ? super V, ? extends V> function) {
        requireNonNull(function, "function");
        this.promote();
        ExpungingEntry<V> entry;
        V value;
        for (final Long2ObjectMap.Entry<ExpungingEntry<V>> that : this.read.long2ObjectEntrySet()) {
            if ((value = (entry = that.getValue()).get()) != null) {
                entry.tryReplace(function.apply(that.getLongKey(), value));
            }
        }
    }

    @Override
    public void clear() {
        synchronized (this.lock) {
            this.read = this.function.apply(this.read.size());
            this.dirty = null;
            this.amended = false;
            this.misses = 0;
        }
    }

    // Views

    @Override
    public @NotNull ObjectSet<Entry<V>> long2ObjectEntrySet() {
        if (this.entrySet != null) return this.entrySet;
        return this.entrySet = new EntrySetView();
    }

    private void promote() {
        if (this.amended) {
            synchronized (this.lock) {
                if (this.amended) {
                    this.promoteLocked();
                }
            }
        }
    }

    private void missLocked() {
        this.misses++;
        if (this.misses < this.dirty.size()) return;
        this.promoteLocked();
    }

    private void promoteLocked() {
        this.read = this.dirty;
        this.amended = false;
        this.dirty = null;
        this.misses = 0;
    }

    private void dirtyLocked() {
        if (this.dirty != null) return;
        this.dirty = this.function.apply(this.read.size());
        Long2ObjectMaps.fastForEach(this.read, (entry) -> {
            if (!entry.getValue().tryExpunge()) {
                this.dirty.put(entry.getLongKey(), entry.getValue());
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    /* package */ static final class ExpungingEntryImpl<V> implements ExpungingEntry<V> {
        private static final AtomicReferenceFieldUpdater<ExpungingEntryImpl, Object> UPDATER = AtomicReferenceFieldUpdater
                .newUpdater(ExpungingEntryImpl.class, Object.class, "value");
        private static final Object EXPUNGED = new Object();
        private volatile Object value;

        /* package */ ExpungingEntryImpl(final @NotNull V value) {
            this.value = value;
        }

        @Override
        public boolean exists() {
            return this.value != null && this.value != ExpungingEntryImpl.EXPUNGED;
        }

        @Override
        public @Nullable V get() {
            return this.value == ExpungingEntryImpl.EXPUNGED ? null : (V) this.value;
        }

        @Override
        public @NotNull V getOr(final @NotNull V other) {
            final Object value = this.value;
            return value != null && value != ExpungingEntryImpl.EXPUNGED ? (V) this.value : other;
        }

        @Override
        public @NotNull InsertionResult<V> setIfAbsent(final @NotNull V value) {
            for (; ; ) {
                final Object previous = this.value;
                if (previous == ExpungingEntryImpl.EXPUNGED)
                    return new InsertionResultImpl<>(InsertionResultImpl.EXPUNGED, null, null);
                if (previous != null)
                    return new InsertionResultImpl<>(InsertionResultImpl.UNCHANGED, (V) previous, (V) previous);
                if (ExpungingEntryImpl.UPDATER.compareAndSet(this, null, value)) {
                    return new InsertionResultImpl<>(InsertionResultImpl.UPDATED, null, value);
                }
            }
        }

        @Override
        public @NotNull InsertionResult<V> computeIfAbsent(final long key, final @NotNull LongFunction<? extends V> function) {
            V next = null;
            for (; ; ) {
                final Object previous = this.value;
                if (previous == ExpungingEntryImpl.EXPUNGED)
                    return new InsertionResultImpl<>(InsertionResultImpl.EXPUNGED, null, null);
                if (previous != null)
                    return new InsertionResultImpl<>(InsertionResultImpl.UNCHANGED, (V) previous, (V) previous);
                if (ExpungingEntryImpl.UPDATER.compareAndSet(this, null, next != null ? next : (next = function.apply(key)))) {
                    return new InsertionResultImpl<>(InsertionResultImpl.UPDATED, null, next);
                }
            }
        }

        @Override
        public @NotNull InsertionResult<V> computeIfAbsentPrimitive(final long key, final @NotNull Long2ObjectFunction<? extends V> function) {
            V next = null;
            for (; ; ) {
                final Object previous = this.value;
                if (previous == ExpungingEntryImpl.EXPUNGED)
                    return new InsertionResultImpl<>(InsertionResultImpl.EXPUNGED, null, null);
                if (previous != null)
                    return new InsertionResultImpl<>(InsertionResultImpl.UNCHANGED, (V) previous, (V) previous);
                if (ExpungingEntryImpl.UPDATER.compareAndSet(this, null, next != null ? next : (next = function.containsKey(key) ? function.get(key) : null))) {
                    return new InsertionResultImpl<>(InsertionResultImpl.UPDATED, null, next);
                }
            }
        }

        @Override
        public @NotNull InsertionResult<V> computeIfPresent(final long key, final @NotNull BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
            V next = null;
            for (; ; ) {
                final Object previous = this.value;
                if (previous == ExpungingEntryImpl.EXPUNGED)
                    return new InsertionResultImpl<>(InsertionResultImpl.EXPUNGED, null, null);
                if (previous == null) return new InsertionResultImpl<>(InsertionResultImpl.UNCHANGED, null, null);
                if (ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, next != null ? next : (next = remappingFunction.apply(key, (V) previous)))) {
                    return new InsertionResultImpl<>(InsertionResultImpl.UPDATED, (V) previous, next);
                }
            }
        }

        @Override
        public @NotNull InsertionResult<V> compute(final long key, final @NotNull BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
            V next = null;
            for (; ; ) {
                final Object previous = this.value;
                if (previous == ExpungingEntryImpl.EXPUNGED)
                    return new InsertionResultImpl<>(InsertionResultImpl.EXPUNGED, null, null);
                if (ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, next != null ? next : (next = remappingFunction.apply(key, (V) previous)))) {
                    return new InsertionResultImpl<>(InsertionResultImpl.UPDATED, (V) previous, next);
                }
            }
        }

        @Override
        public void set(final @NotNull V value) {
            ExpungingEntryImpl.UPDATER.set(this, value);
        }

        @Override
        public boolean replace(final @NotNull Object compare, final @Nullable V value) {
            for (; ; ) {
                final Object previous = this.value;
                if (previous == ExpungingEntryImpl.EXPUNGED || !Objects.equals(previous, compare)) return false;
                if (ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, value)) return true;
            }
        }

        @Override
        public @Nullable V clear() {
            for (; ; ) {
                final Object previous = this.value;
                if (previous == null || previous == ExpungingEntryImpl.EXPUNGED) return null;
                if (ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, null)) return (V) previous;
            }
        }

        @Override
        public boolean trySet(final @NotNull V value) {
            for (; ; ) {
                final Object previous = this.value;
                if (previous == ExpungingEntryImpl.EXPUNGED) return false;
                if (ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, value)) return true;
            }
        }

        @Override
        public @Nullable V tryReplace(final @NotNull V value) {
            for (; ; ) {
                final Object previous = this.value;
                if (previous == null || previous == ExpungingEntryImpl.EXPUNGED) return null;
                if (ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, value)) return (V) previous;
            }
        }

        @Override
        public boolean tryExpunge() {
            while (this.value == null) {
                if (ExpungingEntryImpl.UPDATER.compareAndSet(this, null, ExpungingEntryImpl.EXPUNGED)) return true;
            }
            return this.value == ExpungingEntryImpl.EXPUNGED;
        }

        @Override
        public boolean tryUnexpungeAndSet(final @NotNull V value) {
            return ExpungingEntryImpl.UPDATER.compareAndSet(this, ExpungingEntryImpl.EXPUNGED, value);
        }

        @Override
        public boolean tryUnexpungeAndCompute(final long key, final @NotNull LongFunction<? extends V> function) {
            if (this.value == ExpungingEntryImpl.EXPUNGED) {
                final Object value = function.apply(key);
                return ExpungingEntryImpl.UPDATER.compareAndSet(this, ExpungingEntryImpl.EXPUNGED, value);
            }
            return false;
        }

        @Override
        public boolean tryUnexpungeAndComputePrimitive(final long key, final @NotNull Long2ObjectFunction<? extends V> function) {
            if (this.value == ExpungingEntryImpl.EXPUNGED) {
                final Object value = function.containsKey(key) ? function.get(key) : null;
                return ExpungingEntryImpl.UPDATER.compareAndSet(this, ExpungingEntryImpl.EXPUNGED, value);
            }
            return false;
        }

        @Override
        public boolean tryUnexpungeAndCompute(final long key, final @NotNull BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
            if (this.value == ExpungingEntryImpl.EXPUNGED) {
                final Object value = remappingFunction.apply(key, null);
                return ExpungingEntryImpl.UPDATER.compareAndSet(this, ExpungingEntryImpl.EXPUNGED, value);
            }
            return false;
        }
    }

    /* package */ record InsertionResultImpl<V>(byte operation, V previous, V current) implements InsertionResult<V> {
        private static final byte UNCHANGED = 0x00;
        private static final byte UPDATED = 0x01;
        private static final byte EXPUNGED = 0x02;

        /* package */ InsertionResultImpl(final byte operation, final @Nullable V previous, final @Nullable V current) {
            this.operation = operation;
            this.previous = previous;
            this.current = current;
        }

        @Override
        public @Nullable V previous() {
            return this.previous;
        }

        @Override
        public @Nullable V current() {
            return this.current;
        }
    }

    /* package */ final class MapEntry implements Long2ObjectMap.Entry<V> {
        private final long key;
        private V value;

        /* package */ MapEntry(final long key, final @NotNull V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public long getLongKey() {
            return this.key;
        }

        @Override
        public @NotNull V getValue() {
            return this.value;
        }

        @Override
        public @Nullable V setValue(final @NotNull V value) {
            requireNonNull(value, "value");
            final V previous = Long2ObjectSyncMapImpl.this.put(this.key, value);
            this.value = value;
            return previous;
        }

        @Override
        public @NotNull String toString() {
            return "Long2ObjectSyncMapImpl.MapEntry{key=" + this.getLongKey() + ", value=" + this.getValue() + "}";
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Long2ObjectMap.Entry<?> other)) {
                return false;
            }
            return Objects.equals(this.getLongKey(), other.getLongKey())
                    && Objects.equals(this.getValue(), other.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getLongKey(), this.getValue());
        }
    }

    /* package */ final class EntrySetView extends AbstractObjectSet<Entry<V>> {
        @Override
        public int size() {
            return Long2ObjectSyncMapImpl.this.size();
        }

        @Override
        public boolean contains(final @Nullable Object entry) {
            if (!(entry instanceof Long2ObjectMap.Entry<?> mapEntry)) {
                return false;
            }
            final V value = Long2ObjectSyncMapImpl.this.get(mapEntry.getLongKey());
            return value != null && Objects.equals(value, mapEntry.getValue());
        }

        @Override
        public boolean add(final Long2ObjectMap.@NotNull Entry<V> entry) {
            requireNonNull(entry, "entry");
            return Long2ObjectSyncMapImpl.this.put(entry.getLongKey(), entry.getValue()) == null;
        }

        @Override
        public boolean remove(final @Nullable Object entry) {
            if (!(entry instanceof Long2ObjectMap.Entry<?> mapEntry)) {
                return false;
            }
            return Long2ObjectSyncMapImpl.this.remove(mapEntry.getLongKey(), mapEntry.getValue());
        }

        @Override
        public void clear() {
            Long2ObjectSyncMapImpl.this.clear();
        }

        @Override
        public @NotNull ObjectIterator<Entry<V>> iterator() {
            Long2ObjectSyncMapImpl.this.promote();
            return new EntryIterator(Long2ObjectSyncMapImpl.this.read.long2ObjectEntrySet().iterator());
        }
    }

    /* package */ final class EntryIterator implements ObjectIterator<Long2ObjectMap.Entry<V>> {
        private final Iterator<Entry<ExpungingEntry<V>>> backingIterator;
        private Long2ObjectMap.Entry<V> next;
        private Long2ObjectMap.Entry<V> current;

        /* package */ EntryIterator(final @NotNull Iterator<Long2ObjectMap.Entry<ExpungingEntry<V>>> backingIterator) {
            this.backingIterator = backingIterator;
            this.advance();
        }

        @Override
        public boolean hasNext() {
            return this.next != null;
        }

        @Override
        public Long2ObjectMap.@NotNull Entry<V> next() {
            final Long2ObjectMap.@NotNull Entry<V> current;
            if ((current = this.next) == null) throw new NoSuchElementException();
            this.current = current;
            this.advance();
            return current;
        }

        @Override
        public void remove() {
            final Long2ObjectMap.@NotNull Entry<V> current;
            if ((current = this.current) == null) throw new IllegalStateException();
            this.current = null;
            Long2ObjectSyncMapImpl.this.remove(current.getLongKey());
        }

        private void advance() {
            this.next = null;
            while (this.backingIterator.hasNext()) {
                final Long2ObjectMap.Entry<ExpungingEntry<V>> entry;
                final V value;
                if ((value = (entry = this.backingIterator.next()).getValue().get()) != null) {
                    this.next = new MapEntry(entry.getLongKey(), value);
                    return;
                }
            }
        }
    }
}