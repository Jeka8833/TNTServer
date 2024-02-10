package com.jeka8833.tntserver.gamechat;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record PlayerMute(UUID criminal, ZonedDateTime unbanAt, String reason) {
    private static final Map<UUID, PlayerMute> MUTE_LIST = new ConcurrentHashMap<>();

    public static void setMuteList(PlayerMute[] players) {
        MUTE_LIST.clear();
        for (PlayerMute player : players) {
            MUTE_LIST.put(player.criminal(), player);
        }
    }

    public static PlayerMute getPlayer(UUID player) {
        return MUTE_LIST.get(player);
    }

    public boolean isMuted() {
        return ZonedDateTime.now().isBefore(unbanAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerMute playerMute = (PlayerMute) o;

        return criminal.equals(playerMute.criminal);
    }

    @Override
    public int hashCode() {
        return criminal.hashCode();
    }
}
