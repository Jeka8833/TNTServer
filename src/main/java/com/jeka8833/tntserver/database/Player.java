package com.jeka8833.tntserver.database;

import com.jeka8833.tntserver.database.storage.HypixelPlayer;
import com.jeka8833.tntserver.database.storage.HypixelPlayerStorage;
import com.jeka8833.tntserver.database.storage.TNTPlayerStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Player {
    private static final long INACTIVE_TIME = TimeUnit.MINUTES.toMillis(1);
    private static final long HYPIXEL_CACHE = TimeUnit.MINUTES.toMillis(5);

    public final @NotNull UUID uuid;
    private long timeDelete;

    public volatile @Nullable HypixelPlayer hypixelPlayerInfo;
    public @Nullable TNTPlayerStorage tntPlayerInfo;

    public Player(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isInactive() {
        return timeDelete < System.currentTimeMillis();
    }

    public void playerCalled() {
        if (hypixelPlayerInfo instanceof HypixelPlayerStorage)
            timeDelete = System.currentTimeMillis() + HYPIXEL_CACHE;
        else
            timeDelete = System.currentTimeMillis() + INACTIVE_TIME;
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
