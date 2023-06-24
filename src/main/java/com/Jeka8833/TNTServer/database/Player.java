package com.Jeka8833.TNTServer.database;

import com.Jeka8833.TNTServer.database.storage.HypixelPlayer;
import com.Jeka8833.TNTServer.database.storage.TNTPlayerStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Player {
    private static final int INACTIVE_TIME = 60_000;

    public final @NotNull UUID uuid;
    private long timeDelete;

    public @Nullable HypixelPlayer hypixelPlayerInfo;
    public @Nullable TNTPlayerStorage tntPlayerInfo;

    public Player(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isInactive() {
        return timeDelete < System.currentTimeMillis();
    }

    public void playerCalled() {
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
