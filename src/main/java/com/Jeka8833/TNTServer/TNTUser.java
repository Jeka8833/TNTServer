package com.Jeka8833.TNTServer;

import com.Jeka8833.TNTServer.util.PlayerPing;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TNTUser {
    public static final byte STATUS_ONLINE = 3;
    public static final byte STATUS_AFK = 2;
    public static final byte STATUS_INVISIBLE = 1;
    public static final byte STATUS_OFFLINE = 0;


    public final @NotNull UUID uuid;
    public String version;
    public long timeLogin;
    public long forceBlock;
    public byte donate;


    @Nullable
    public PlayerPing ping;
    @Nullable
    public String gameInfo;
    private long nextTimeDead;
    public long forceActive;
    public long activeModules;

    @MagicConstant(flags = {STATUS_ONLINE, STATUS_AFK, STATUS_INVISIBLE, STATUS_OFFLINE})
    public byte status;
    public int fight;

    public TNTUser(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    public void heartBeat() {
        nextTimeDead = System.currentTimeMillis() + 60_000;
    }

    public boolean isUserDead() {
        return System.currentTimeMillis() > nextTimeDead;
    }

    public boolean hasInDB() {
        return version != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TNTUser user = (TNTUser) o;

        return uuid.equals(user.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
