package com.Jeka8833.TNTServer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class TNTUser {

    public static final byte STATUS_ONLINE = 3;
    public static final byte STATUS_AFK = 2;
    public static final byte STATUS_INVISIBLE = 1;
    public static final byte STATUS_OFFLINE = 0;

    public final UUID user;
    public UUID key;
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

    public TNTUser(final UUID user, final UUID key, final String version) {
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
        return user != null ? user.hashCode() : 0;
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
        user2key.put(tntUser.user, tntUser.key);
        keyUserList.put(tntUser.key, tntUser);
        tntUser.heartBeat();
    }

    public static void removeUser(final UUID key) {
        final TNTUser user = TNTUser.keyUserList.remove(key);
        if (user != null)
            TNTUser.user2key.remove(user.user);
    }

    public static final Map<UUID, TNTUser> keyUserList = new HashMap<>();
    public static final Map<UUID, UUID> user2key = new HashMap<>();
}
