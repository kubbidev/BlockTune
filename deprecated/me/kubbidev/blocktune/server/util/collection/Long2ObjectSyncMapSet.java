package me.kubbidev.blocktune.server.util.collection;

import it.unimi.dsi.fastutil.longs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/* package */ final class Long2ObjectSyncMapSet extends AbstractLongSet implements LongSet, Serializable {
    @Serial
    private static final long serialVersionUID = 1;
    private final Long2ObjectSyncMap<Boolean> map;
    private final LongSet set;

    /* package */ Long2ObjectSyncMapSet(final @NotNull Long2ObjectSyncMap<Boolean> map) {
        this.map = map;
        this.set = map.keySet();
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean contains(final long key) {
        return this.map.containsKey(key);
    }

    @Override
    public boolean remove(final long key) {
        return this.map.remove(key) != null;
    }

    @Override
    public boolean add(final long key) {
        return this.map.put(key, Boolean.TRUE) == null;
    }

    @Override
    public boolean containsAll(final @NotNull LongCollection collection) {
        return this.set.containsAll(collection);
    }

    @Override
    public boolean removeAll(final @NotNull LongCollection collection) {
        return this.set.removeAll(collection);
    }

    @Override
    public boolean retainAll(final @NotNull LongCollection collection) {
        return this.set.retainAll(collection);
    }

    @Override
    public @NotNull LongIterator iterator() {
        return this.set.iterator();
    }

    @Override
    public @NotNull LongSpliterator spliterator() {
        return this.set.spliterator();
    }

    @Override
    public long[] toArray(long[] original) {
        return this.set.toArray(original);
    }

    @Override
    public long[] toLongArray() {
        return this.set.toLongArray();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(final @Nullable Object other) {
        return other == this || this.set.equals(other);
    }

    @Override
    public @NotNull String toString() {
        return this.set.toString();
    }

    @Override
    public int hashCode() {
        return this.set.hashCode();
    }
}