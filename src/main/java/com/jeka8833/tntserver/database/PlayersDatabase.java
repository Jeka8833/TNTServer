package com.jeka8833.tntserver.database;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayersDatabase {
    public static final UUID settingUser = UUID.fromString("00000000-0000-4000-0000-000000000000");

    public static final Map<UUID, User> uuid2User = new ConcurrentHashMap<>();

    public static void clearInactivePeople() {
        uuid2User.values().removeIf(User::isInactive);
    }

    public static boolean isUser(@NotNull UUID uuid) {
        if (settingUser.equals(uuid)) return true;

        return uuid.version() == 4 && uuid.variant() == 2;
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
    public static Collection<Bot> getBotsWithPrivilege(@NotNull String privilege) {
        List<Bot> bots = new ArrayList<>();
        for (User user : uuid2User.values()) {
            if (!user.isInactive() && user instanceof Bot bot && bot.hasPrivilege(privilege)) {
                bots.add(bot);
            }
        }
        return bots;
    }
}
