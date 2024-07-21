package me.kubbidev.blocktune.core.skill.handler;

import me.kubbidev.blocktune.core.entity.EntityMetadataProvider;
import me.kubbidev.blocktune.core.event.skill.EndSkillCastEvent;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public abstract class SkillHandlerRunnable implements Runnable {
    private BukkitTask task;
    // the skill meta used to init this runnable
    private SkillMetadata meta;

    /**
     * Returns true if this task has been cancelled.
     *
     * @return true if the task has been cancelled
     * @throws IllegalStateException if task was not scheduled yet
     */
    public synchronized boolean isCancelled() throws IllegalStateException {
        checkScheduled();
        return task.isCancelled();
    }

    /**
     * Attempts to cancel this task.
     *
     * @throws IllegalStateException if task was not scheduled yet
     */
    private synchronized void cancel() throws IllegalStateException {
        Bukkit.getScheduler().cancelTask(getTaskId());
        onEnd();
        if (this.meta == null) {
            return;
        }
        // remove this metadata from casting in the caster metadata map instance
        EntityMetadataProvider.onCastEnd(this.meta);
        // call the end event on cancel
        EndSkillCastEvent called = new EndSkillCastEvent(this.meta);
        called.callEvent();
    }

    public abstract boolean shouldCancel();

    protected abstract void tick();

    protected abstract void onStart();

    protected abstract void onEnd();

    @Override
    public final void run() {
        if (shouldCancel()) {
            cancel();
        } else {
            tick();
        }
    }

    /**
     * Schedules this to repeatedly run until cancelled, starting after the
     * specified number of server ticks.
     *
     * @param meta the reference to the skill scheduling task
     * @return a BukkitTask that contains the id number
     * @throws IllegalArgumentException if meta is null
     * @throws IllegalStateException    if this was already scheduled
     * @see SkillHandlerRunnable#runTask(SkillMetadata, long, long)
     */
    @NotNull
    public synchronized BukkitTask runTask(@NotNull SkillMetadata meta) throws IllegalArgumentException, IllegalStateException {
        return runTask(meta, 0L, 1L);
    }

    /**
     * Schedules this to repeatedly run until cancelled, starting after the
     * specified number of server ticks.
     *
     * @param meta   the reference to the skill scheduling task
     * @param delay  the ticks to wait before running the task
     * @param period the ticks to wait between runs
     * @return a BukkitTask that contains the id number
     * @throws IllegalArgumentException if meta is null
     * @throws IllegalStateException    if this was already scheduled
     */
    @NotNull
    public synchronized BukkitTask runTask(@NotNull SkillMetadata meta, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        // attach this metadata as casting in the entity metadata map instance
        EntityMetadataProvider.onCastStart(meta);
        onStart();
        this.meta = meta;
        return setupTask(Bukkit.getScheduler().runTaskTimer(meta.plugin(), this, delay, period));
    }

    /**
     * Gets the task id for this runnable.
     *
     * @return the task id that this runnable was scheduled as
     * @throws IllegalStateException if task was not scheduled yet
     */
    public synchronized int getTaskId() throws IllegalStateException {
        checkScheduled();
        return this.task.getTaskId();
    }

    private void checkScheduled() {
        if (this.task == null) {
            throw new IllegalStateException("Not scheduled yet");
        }
    }

    private void checkNotYetScheduled() {
        if (this.task != null) {
            throw new IllegalStateException("Already scheduled as " + this.task.getTaskId());
        }
    }

    @NotNull
    private BukkitTask setupTask(@NotNull final BukkitTask task) {
        this.task = task;
        return task;
    }
}
