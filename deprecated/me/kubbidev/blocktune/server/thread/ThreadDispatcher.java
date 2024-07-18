package me.kubbidev.blocktune.server.thread;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue;
import io.netty.util.internal.shaded.org.jctools.queues.MpscUnboundedArrayQueue;
import me.kubbidev.blocktune.server.Tickable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.IntFunction;

/**
 * ThreadDispatcher can be used to dispatch updates (ticks) across a number of "partitions" (such as chunks) that
 * house {@link Tickable} instances. The parallelism of such updates is defined when the dispatcher
 * is constructed.
 * <p>
 * Instances of this class can be obtained by calling {@link ThreadDispatcher#of(ThreadProvider, int)}, or a similar
 * overload.
 */
public final class ThreadDispatcher<P> {
    private final ThreadProvider<P> provider;
    private final List<TickThread> threads;

    // partition -> dispatching context
    // defines how computation is dispatched to the threads
    private final Map<P, Partition> partitions = new WeakHashMap<>();
    // cache to retrieve the threading context from a tickable element
    private final Map<Tickable, Partition> elements = new WeakHashMap<>();
    // queue to update partition linked thread
    private final ArrayDeque<P> partitionUpdateQueue = new ArrayDeque<>();

    // Requests consumed at the end of each tick
    private final MessagePassingQueue<DispatchUpdate<P>> updates = new MpscUnboundedArrayQueue<>(1024);

    private ThreadDispatcher(ThreadProvider<P> provider, int threadCount, @NotNull IntFunction<? extends TickThread> threadGenerator) {
        this.provider = provider;
        TickThread[] threads = new TickThread[threadCount];
        Arrays.setAll(threads, threadGenerator);
        this.threads = List.of(threads);
        this.threads.forEach(Thread::start);
    }

    /**
     * Creates a new ThreadDispatcher using default thread names (ex. Ms-Tick-n).
     *
     * @param provider    the {@link ThreadProvider} instance to be used for defining thread IDs
     * @param threadCount the number of threads to create for this dispatcher
     * @param <P>         the dispatcher partition type
     * @return a new ThreadDispatcher instance
     */
    public static <P> @NotNull ThreadDispatcher<P> of(@NotNull ThreadProvider<P> provider, int threadCount) {
        return new ThreadDispatcher<>(provider, threadCount, TickThread::new);
    }

    /**
     * Creates a new ThreadDispatcher using the caller-provided thread name generator {@code nameGenerator}. This is
     * useful to disambiguate custom ThreadDispatcher instances from ones used in core Minestom code.
     *
     * @param provider      the {@link ThreadProvider} instance to be used for defining thread IDs
     * @param nameGenerator a function that should return unique names, given a thread index
     * @param threadCount   the number of threads to create for this dispatcher
     * @param <P>           the dispatcher partition type
     * @return a new ThreadDispatcher instance
     */
    public static <P> @NotNull ThreadDispatcher<P> of(@NotNull ThreadProvider<P> provider,
                                                      @NotNull IntFunction<String> nameGenerator, int threadCount) {
        return new ThreadDispatcher<>(provider, threadCount, index -> new TickThread(nameGenerator.apply(index)));
    }

    /**
     * Creates a single-threaded dispatcher that uses default thread names.
     *
     * @param <P> the dispatcher partition type
     * @return a new ThreadDispatcher instance
     */
    public static <P> @NotNull ThreadDispatcher<P> singleThread() {
        return of(ThreadProvider.counter(), 1);
    }

    /**
     * Gets the unmodifiable list of TickThreads used to dispatch updates.
     * <p>
     * This method is marked internal to reflect {@link TickThread}s own internal status.
     *
     * @return the TickThreads used to dispatch updates
     */
    @Unmodifiable
    @ApiStatus.Internal
    public @NotNull List<@NotNull TickThread> threads() {
        return this.threads;
    }

    /**
     * Prepares the update by creating the {@link TickThread} tasks.
     *
     * @param time the tick time in milliseconds
     */
    public synchronized void updateAndAwait(long time) {
        // update dispatcher
        this.updates.drain(update -> {
            switch (update) {
                case DispatchUpdate.PartitionLoad<P> chunkUpdate -> processLoadedPartition(chunkUpdate.partition());
                case DispatchUpdate.PartitionUnload<P> partitionUnload ->
                        processUnloadedPartition(partitionUnload.partition());
                case DispatchUpdate.ElementUpdate<P> elementUpdate ->
                        processUpdatedElement(elementUpdate.tickable(), elementUpdate.partition());
                case DispatchUpdate.ElementRemove<P> elementRemove -> processRemovedElement(elementRemove.tickable());
                case null, default -> throw new IllegalStateException("Unknown update type: " +
                        (update == null ? "null" : update.getClass().getSimpleName()));
            }
        });
        // tick all partitions
        CountDownLatch latch = new CountDownLatch(this.threads.size());
        for (TickThread thread : this.threads) {
            thread.startTick(latch, time);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called at the end of each tick to clear removed tickables, refresh the partition linked to a tickable, and
     * partition threads based on {@link ThreadProvider#findThread(Object)}.
     *
     * @param nanoTimeout max time in nanoseconds to update partitions
     */
    public void refreshThreads(long nanoTimeout) {
        switch (provider.refreshType()) {
            case NEVER -> {
                // Do nothing
            }
            case ALWAYS -> {
                long currentTime = System.nanoTime();
                int counter = this.partitionUpdateQueue.size();
                while (true) {
                    P partition = this.partitionUpdateQueue.pollFirst();
                    if (partition == null) {
                        break;
                    }
                    // update chunk's thread
                    Partition partitionEntry = this.partitions.get(partition);
                    assert partitionEntry != null;
                    TickThread previous = partitionEntry.thread;
                    TickThread next = retrieveThread(partition);
                    if (next != previous) {
                        partitionEntry.thread = next;
                        previous.entries().remove(partitionEntry);
                        next.entries().add(partitionEntry);
                    }
                    this.partitionUpdateQueue.addLast(partition);
                    if (--counter <= 0 || System.nanoTime() - currentTime >= nanoTimeout) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Refreshes all thread as per {@link ThreadDispatcher#refreshThreads(long)}, with a timeout of
     * {@link Long#MAX_VALUE}.
     */
    public void refreshThreads() {
        refreshThreads(Long.MAX_VALUE);
    }

    /**
     * Registers a new partition.
     *
     * @param partition the partition to register
     */
    public void createPartition(@NotNull P partition) {
        signalUpdate(new DispatchUpdate.PartitionLoad<>(partition));
    }

    /**
     * Deletes an existing partition.
     *
     * @param partition the partition to delete
     */
    public void deletePartition(@NotNull P partition) {
        signalUpdate(new DispatchUpdate.PartitionUnload<>(partition));
    }

    /**
     * Updates a {@link Tickable}, signalling that it is a part of {@code partition}.
     *
     * @param tickable  the Tickable to update
     * @param partition the partition the Tickable is part of
     */
    public void updateElement(@NotNull Tickable tickable, @NotNull P partition) {
        signalUpdate(new DispatchUpdate.ElementUpdate<>(tickable, partition));
    }

    /**
     * Removes a {@link Tickable}.
     *
     * @param tickable the Tickable to remove
     */
    public void removeElement(@NotNull Tickable tickable) {
        signalUpdate(new DispatchUpdate.ElementRemove<>(tickable));
    }

    /**
     * Shutdowns all the {@link TickThread tick threads}.
     * <p>
     * Action is irreversible.
     */
    public void shutdown() {
        this.threads.forEach(TickThread::shutdown);
    }

    private TickThread retrieveThread(P partition) {
        int threadId = this.provider.findThread(partition);
        int index = Math.abs(threadId) % this.threads.size();
        return this.threads.get(index);
    }

    private void signalUpdate(@NotNull DispatchUpdate<P> update) {
        this.updates.relaxedOffer(update);
    }

    private void processLoadedPartition(P partition) {
        if (this.partitions.containsKey(partition)) {
            return;
        }
        TickThread thread = retrieveThread(partition);
        Partition partitionEntry = new Partition(thread);
        thread.entries().add(partitionEntry);
        this.partitions.put(partition, partitionEntry);
        this.partitionUpdateQueue.add(partition);
        if (partition instanceof Tickable tickable) {
            processUpdatedElement(tickable, partition);
        }
    }

    private void processUnloadedPartition(P partition) {
        Partition partitionEntry = this.partitions.remove(partition);
        if (partitionEntry != null) {
            TickThread thread = partitionEntry.thread;
            thread.entries().remove(partitionEntry);
        }
        this.partitionUpdateQueue.remove(partition);
        if (partition instanceof Tickable tickable) {
            processRemovedElement(tickable);
        }
    }

    private void processRemovedElement(Tickable tickable) {
        Partition partition = this.elements.get(tickable);
        if (partition != null) {
            partition.elements.remove(tickable);
        }
    }

    private void processUpdatedElement(Tickable tickable, P partition) {
        Partition partitionEntry;

        partitionEntry = elements.get(tickable);
        // Remove from previous list
        if (partitionEntry != null) {
            partitionEntry.elements.remove(tickable);
        }
        // Add to new list
        partitionEntry = partitions.get(partition);
        if (partitionEntry != null) {
            this.elements.put(tickable, partitionEntry);
            partitionEntry.elements.add(tickable);
        }
    }

    /**
     * A data structure which may contain {@link Tickable}s, and is assigned a single {@link TickThread}.
     */
    public static final class Partition {
        private TickThread thread;
        private final List<Tickable> elements = new ArrayList<>();

        private Partition(TickThread thread) {
            this.thread = thread;
        }

        /**
         * The {@link TickThread} used by this partition.
         * <p>
         * This method is marked internal to reflect {@link TickThread}s own internal status.
         *
         * @return the TickThread used by this partition
         */
        @ApiStatus.Internal
        public @NotNull TickThread thread() {
            return this.thread;
        }

        /**
         * The {@link Tickable}s assigned to this partition.
         *
         * @return the tickables assigned to this partition
         */
        public @NotNull List<Tickable> elements() {
            return this.elements;
        }
    }

    @ApiStatus.Internal
    sealed interface DispatchUpdate<P> permits
            DispatchUpdate.PartitionLoad, DispatchUpdate.PartitionUnload,
            DispatchUpdate.ElementUpdate, DispatchUpdate.ElementRemove {
        record PartitionLoad<P>(@NotNull P partition) implements DispatchUpdate<P> {
        }

        record PartitionUnload<P>(@NotNull P partition) implements DispatchUpdate<P> {
        }

        record ElementUpdate<P>(@NotNull Tickable tickable, P partition) implements DispatchUpdate<P> {
        }

        record ElementRemove<P>(@NotNull Tickable tickable) implements DispatchUpdate<P> {
        }
    }
}