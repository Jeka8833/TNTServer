package com.jeka8833.tntserver.old.database.storage;

import com.jeka8833.tntserver.old.user.player.GameServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Player extends User {
    private static final long INACTIVE_TIME = TimeUnit.MINUTES.toNanos(5);
    public @Nullable TNTPlayerStorage tntPlayerInfo;
    public @NotNull GameServer serverType = GameServer.UNKNOWN;
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
    }
}
