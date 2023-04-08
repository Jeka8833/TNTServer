package com.Jeka8833.TNTServer;

import com.Jeka8833.TNTServer.util.PlayerPing;
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

    public @NotNull UUID user;
    public @Nullable UUID key;
    public @Nullable PlayerPing playerPing;
    public String version;
    public String gameInfo;

    public long timeLogin;
    private long lastTimePacket;

    public long activeModules;

    public long forceBlock;
    public long forceActive;

    public byte donate;
    public byte status;

    public int fight;

    public TNTUser(@NotNull UUID user, final String version) {
        this(user, null, version);
    }

    public TNTUser(@NotNull UUID user, @Nullable UUID key, final String version) {
        this.user = user;
        this.key = key;
        this.version = version;
    }

    public void heartBeat() {
        lastTimePacket = System.currentTimeMillis();
    }

    public boolean isUserDead() {
        return System.currentTimeMillis() - lastTimePacket > 60_000;
    }

    public boolean isClient() {
        return version != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TNTUser tntUser)) return false;

        return Objects.equals(user, tntUser.user);
    }

    @Override
    public int hashCode() {
        return user.hashCode();
    }

    @Override
    public String toString() {
        return "TNTUser{" +
                "user=" + user +
                ", key=" + key +
                ", version='" + version + '\'' +
                ", gameInfo='" + gameInfo + '\'' +
                ", timeLogin=" + timeLogin +
                ", lastTimePacket=" + lastTimePacket +
                ", activeModules=" + activeModules +
                ", forceBlock=" + forceBlock +
                ", forceActive=" + forceActive +
                ", donate=" + donate +
                ", status=" + status +
                ", fight=" + fight +
                '}';
    }

    public static void addUser(final TNTUser tntUser) {
        uuid2User.put(tntUser.user, tntUser);
        tntUser.heartBeat();
    }

    public static final Map<UUID, TNTUser> uuid2User = new ConcurrentHashMap<>();
}
