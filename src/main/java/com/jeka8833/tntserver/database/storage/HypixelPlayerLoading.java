package com.jeka8833.tntserver.database.storage;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class HypixelPlayerLoading implements HypixelPlayer {
    private static final long TIMEOUT_TIME = TimeUnit.SECONDS.toNanos(30);

    private final @NotNull Collection<Consumer<HypixelPlayer>> listeners;
    private long startTime;

    public HypixelPlayerLoading(@NotNull Collection<Consumer<HypixelPlayer>> listeners) {
        this.listeners = listeners;
    }

    @NotNull
    public Collection<Consumer<HypixelPlayer>> listeners() {
        return listeners;
    }

    public boolean isTimeout() {
        return System.nanoTime() - startTime > TIMEOUT_TIME;
    }

    public void updateTimeout() {
        startTime = System.nanoTime();
    }
}
