package com.jeka8833.tntserver.database;

import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.database.storage.*;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Player extends User {
    private static final long INACTIVE_TIME = TimeUnit.MINUTES.toMillis(1);
    private static final long HYPIXEL_CACHE = TimeUnit.MINUTES.toMillis(5);

    private final Object HYPIXEL_MUTEX = new Object();
    private long timeDelete;
    public volatile @Nullable HypixelPlayer hypixelPlayerInfo;
    public @Nullable TNTPlayerStorage tntPlayerInfo;

    public @NotNull ServerType serverType = ServerType.UNKNOWN;

    public Player(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public boolean isInactive() {
        return timeDelete < System.currentTimeMillis();
    }

    public void playerCalled() {
        if (hypixelPlayerInfo instanceof HypixelPlayerStorage)
            timeDelete = System.currentTimeMillis() + HYPIXEL_CACHE;
        else
            timeDelete = System.currentTimeMillis() + INACTIVE_TIME;
    }

    @Blocking
    public boolean tryAddToLoadingQueue(Predicate<HypixelPlayerLoading> addToQueueAndCheck,
                                        Consumer<HypixelPlayer> listener) {
        synchronized (HYPIXEL_MUTEX) {
            if (hypixelPlayerInfo instanceof HypixelPlayerLoading loading) {
                if (loading.isTimeout()) {
                    if (!addToQueueAndCheck.test(loading)) return false;
                    loading.setTimeout(30);
                }

                loading.listeners().add(listener);
            } else {
                var loading = new HypixelPlayerLoading(new ArrayList<>());

                if (!addToQueueAndCheck.test(loading)) return false;

                loading.setTimeout(30);
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

            Collection<Consumer<HypixelPlayer>> list = null;
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
