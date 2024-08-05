package com.jeka8833.tntserver.database;

import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.database.storage.TNTPlayerStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Player extends User {
    private static final long INACTIVE_TIME = TimeUnit.MINUTES.toNanos(5);
    public @Nullable TNTPlayerStorage tntPlayerInfo;
    public @NotNull ServerType serverType = ServerType.UNKNOWN;
    private long lastCallTime = System.nanoTime();

    public Player(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public boolean isInactive() {
        return System.nanoTime() - lastCallTime > INACTIVE_TIME;
    }

    public void playerCalled() {
        lastCallTime = System.nanoTime();
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
