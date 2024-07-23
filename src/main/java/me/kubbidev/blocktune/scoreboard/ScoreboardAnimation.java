package me.kubbidev.blocktune.scoreboard;

import org.jetbrains.annotations.NotNull;

public class ScoreboardAnimation<T> {
    private final T[] frames;
    private int currentFrame;

    private boolean looping;
    private boolean backAndForth;
    private boolean forward;

    public ScoreboardAnimation(@NotNull T[] frames) {
        if (frames == null || frames.length == 0) {
            throw new IllegalArgumentException("Frames array cannot be null or empty.");
        }
        this.frames = frames;
        this.currentFrame = 0;
        this.looping = true;
        this.backAndForth = false;
        this.forward = true;
    }

    public T getCurrentFrame() {
        return this.frames[this.currentFrame];
    }

    public void nextFrame() {
        if (this.backAndForth) {
            if (this.forward) {
                if (this.currentFrame < this.frames.length - 1) {
                    this.currentFrame++;
                } else {
                    this.forward = false;
                    this.currentFrame--;
                }
            } else {
                if (this.currentFrame > 0) {
                    this.currentFrame--;
                } else {
                    this.forward = true;
                    this.currentFrame++;
                }
            }
        } else {
            if (this.currentFrame < this.frames.length - 1) {
                this.currentFrame++;
            } else if (this.looping) {
                this.currentFrame = 0;
            }
        }
    }

    public void previousFrame() {
        if (this.backAndForth) {
            if (!this.forward) {
                if (this.currentFrame < this.frames.length - 1) {
                    this.currentFrame++;
                } else {
                    this.forward = true;
                    this.currentFrame--;
                }
            } else {
                if (this.currentFrame > 0) {
                    this.currentFrame--;
                } else {
                    this.forward = false;
                    this.currentFrame++;
                }
            }
        } else {
            if (this.currentFrame > 0) {
                this.currentFrame--;
            } else if (this.looping) {
                this.currentFrame = frames.length - 1;
            }
        }
    }

    public void reset() {
        this.currentFrame = 0;
        this.forward = true;
    }

    public void setFrame(int frame) {
        if (frame < 0 || frame >= this.frames.length) {
            throw new IllegalArgumentException("Frame index out of bounds.");
        }
        this.currentFrame = frame;
    }

    public boolean isLooping() {
        return this.looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public boolean isBackAndForth() {
        return this.backAndForth;
    }

    public void setBackAndForth(boolean backAndForth) {
        this.backAndForth = backAndForth;
    }
}