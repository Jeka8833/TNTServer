package com.jeka8833.tntserver.database;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayersDatabase {
    private static final Map<UUID, Player> uuid2User = new ConcurrentHashMap<>();

    public static void clearInactivePeople() {
        uuid2User.values().removeIf(Player::isInactive);
    }

    @NotNull
    public static Player getOrCreate(@NotNull UUID uuid) {
        Player user = uuid2User.computeIfAbsent(uuid, Player::new);
        user.playerCalled();

        return user;
    }

    @Nullable
    @Contract("null -> null")
    public static Player getUser(@Nullable UUID uuid) {
        if (uuid == null) return null;

        Player user = uuid2User.get(uuid);
        if (user != null) user.playerCalled();

        return user;
    }
}
