package com.jeka8833.tntserver.database.storage;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;

public final class HypixelPlayerLoading implements HypixelPlayer {
    private final @NotNull Collection<Consumer<HypixelPlayer>> listeners;
    private long timeout;

    public HypixelPlayerLoading(@NotNull Collection<Consumer<HypixelPlayer>> listeners) {
        this.listeners = listeners;
    }

    @NotNull
    public Collection<Consumer<HypixelPlayer>> listeners() {
        return listeners;
    }

    public boolean isTimeout() {
        return System.currentTimeMillis() > timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = System.currentTimeMillis() + timeout * 1_000L;
    }
}
