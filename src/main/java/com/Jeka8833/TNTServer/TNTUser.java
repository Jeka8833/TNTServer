package com.Jeka8833.TNTServer;

import com.Jeka8833.TNTServer.util.PlayerPing;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

        heartBeat();
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
        if (!(o instanceof TNTUser tntUser)) return false;

        return Objects.equals(uuid, tntUser.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "TNTUser{" +
                "user=" + uuid +
                ", version='" + version + '\'' +
                ", gameInfo='" + gameInfo + '\'' +
                ", timeLogin=" + timeLogin +
                ", lastTimePacket=" + nextTimeDead +
                ", activeModules=" + activeModules +
                ", forceBlock=" + forceBlock +
                ", forceActive=" + forceActive +
                ", donate=" + donate +
                ", status=" + status +
                ", fight=" + fight +
                '}';
    }

    public static final Map<UUID, TNTUser> uuid2User = new ConcurrentHashMap<>();

    @Nullable
    public static TNTUser getUser(@NotNull UUID uuid) {
        TNTUser user = uuid2User.get(uuid);
        if (user != null) user.heartBeat();

        return user;
    }
}
