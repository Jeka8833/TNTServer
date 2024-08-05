package com.jeka8833.tntserver.database;

import com.jeka8833.tntserver.mojang.MojangAPI;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayersDatabase {
    public static final UUID SETTING_USER = UUID.fromString("00000000-0000-4000-0000-000000000000");
    public static final Map<UUID, User> uuid2User = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayersDatabase.class);

    public static void clearInactivePeople() {
        boolean isSomethingDeleted = uuid2User.values().removeIf(User::isInactive);
        if (isSomethingDeleted)
            LOGGER.debug("Clear inactive people, size: {}", uuid2User.size());
    }

    public static boolean isUser(@NotNull UUID uuid) {
        if (SETTING_USER.equals(uuid)) return true;

        return MojangAPI.isPlayer(uuid);
    }

    @NotNull
    public static User getOrCreate(@NotNull UUID uuid) {
        User user = uuid2User.computeIfAbsent(uuid,
                userUUID -> isUser(userUUID) ? new Player(userUUID) : new Bot(userUUID));

        if (user instanceof Player player) player.playerCalled();

        return user;
    }

    @Nullable
    @Contract("null -> null")
    public static User getUser(@Nullable UUID uuid) {
        if (uuid == null) return null;

        User user = uuid2User.get(uuid);
        if (user instanceof Player player) player.playerCalled();

        return user;
    }

    public static void deleteUser(UUID uuid) {
        uuid2User.remove(uuid);
    }

    @Nullable
    public static Bot getBotWithPrivilege(@NotNull String privilege) {
        for (User user : uuid2User.values()) {
            if (!user.isInactive() && user instanceof Bot bot && bot.hasPrivilege(privilege)) {
                return bot;
            }
        }
        return null;
    }

    @NotNull
    public static ArrayList<Bot> getBotsWithPrivilege(@NotNull String privilege) {
        ArrayList<Bot> bots = new ArrayList<>();
        for (User user : uuid2User.values()) {
            if (!user.isInactive() && user instanceof Bot bot && bot.hasPrivilege(privilege)) {
                bots.add(bot);
            }
        }
        return bots;
    }
}
