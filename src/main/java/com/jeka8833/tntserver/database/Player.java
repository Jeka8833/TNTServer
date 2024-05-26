package com.jeka8833.tntserver.database;

import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.database.storage.*;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Player extends User {
    private static final long INACTIVE_TIME = TimeUnit.MINUTES.toNanos(1);
    private static final long HYPIXEL_CACHE = TimeUnit.MINUTES.toNanos(5);

    private final Object HYPIXEL_MUTEX = new Object();
    private long lastCallTime = System.nanoTime();
    public volatile @Nullable HypixelPlayer hypixelPlayerInfo;
    public @Nullable TNTPlayerStorage tntPlayerInfo;

    public @NotNull ServerType serverType = ServerType.UNKNOWN;

    public Player(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public boolean isInactive() {
        if (hypixelPlayerInfo instanceof HypixelPlayerLoading)
            return System.nanoTime() - lastCallTime > HYPIXEL_CACHE;
        else
            return System.nanoTime() - lastCallTime > INACTIVE_TIME;
    }

    public void playerCalled() {
        lastCallTime = System.nanoTime();
    }

    @Blocking
    public boolean tryAddToLoadingQueue(Predicate<HypixelPlayerLoading> addToQueueAndCheck,
                                        Consumer<HypixelPlayer> listener) {
        synchronized (HYPIXEL_MUTEX) {
            if (hypixelPlayerInfo instanceof HypixelPlayerLoading loading) {
                if (loading.isTimeout()) {
                    if (!addToQueueAndCheck.test(loading)) return false;
                    loading.updateTimeout();
                }

                loading.listeners().add(listener);
            } else {
                var loading = new HypixelPlayerLoading(new ArrayList<>());

                if (!addToQueueAndCheck.test(loading)) return false;

                loading.updateTimeout();
                loading.listeners().add(listener);

                hypixelPlayerInfo = loading;
            }
        }
        return true;
    }

    @Blocking
    public void setHypixelStorage(HypixelPlayer storage) {
        synchronized (HYPIXEL_MUTEX) {
            if ((storage instanceof HypixelPlayerLoading && hypixelPlayerInfo instanceof HypixelPlayerLoading) ||
                    (storage instanceof HypixelPlayerError && hypixelPlayerInfo instanceof HypixelPlayerStorage)) {
                return;
            }

            Iterable<Consumer<HypixelPlayer>> list = null;
            if (hypixelPlayerInfo instanceof HypixelPlayerLoading loading) {
                list = loading.listeners();
            }

            hypixelPlayerInfo = storage;

            if (list != null) {
                for (Consumer<HypixelPlayer> consumer : list) {
                    consumer.accept(storage);
                }
            }
        }
    }

    @Override
    public void disconnect() {
        tntPlayerInfo = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Player player = (Player) o;

        return uuid.equals(player.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
