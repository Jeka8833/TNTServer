package com.jeka8833.tntserver.database;

import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.database.storage.Bot;
import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.storage.TNTPlayerStorage;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.mojang.MojangAPI;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayersDatabase {
    public static final UUID SETTING_USER = UUID.fromString("00000000-0000-4000-0000-000000000000");
    private static final Map<UUID, User> USER_MAP = new ConcurrentHashMap<>();

    public static void clearInactivePeople() {
        USER_MAP.values().removeIf(User::isInactive);
    }

    public static boolean isPlayer(@NotNull UUID uuid) {
        if (SETTING_USER.equals(uuid)) return true;

        return MojangAPI.isPlayer(uuid);
    }

    @NotNull
    public static User getOrCreate(@NotNull UUID uuid) {
        User user = USER_MAP.computeIfAbsent(uuid,
                userUUID -> isPlayer(userUUID) ? new Player(userUUID) : new Bot(userUUID));

        if (user instanceof Player player) player.playerCalled();

        return user;
    }

    @Nullable
    @Contract("null -> null")
    public static User getUser(@Nullable UUID uuid) {
        if (uuid == null) return null;

        User user = USER_MAP.get(uuid);
        if (user instanceof Player player) player.playerCalled();

        return user;
    }

    public static void deleteUser(@NotNull UUID uuid) {
        USER_MAP.remove(uuid);
        clearInactivePeople();
    }

    @Nullable
    public static Bot getBotWithPrivilege(@NotNull String privilege) {
        for (User user : USER_MAP.values()) {
            if (!user.isInactive() && user instanceof Bot bot && bot.hasPrivilege(privilege)) {
                return bot;
            }
        }
        return null;
    }

    @NotNull
    public static ArrayList<Bot> getBotsWithPrivilege(@NotNull String privilege) {
        ArrayList<Bot> bots = new ArrayList<>();
        for (User user : USER_MAP.values()) {
            if (!user.isInactive() && user instanceof Bot bot && bot.hasPrivilege(privilege)) {
                bots.add(bot);
            }
        }
        return bots;
    }

    public static boolean isUsesTNTClient(@Nullable UUID uuid) {
        User user = PlayersDatabase.getUser(uuid);
        if (user instanceof Player player) {
            if (player.tntPlayerInfo != null) {
                return player.tntPlayerInfo.status != TNTPlayerStorage.STATUS_OFFLINE;
            }
        }

        return false;
    }

    @Nullable
    public static String getGameInfo(UUID uuid) {
        User user = getUser(uuid);
        if (user instanceof Player player) {
            if (player.serverType == ServerType.TNT_COMMUNITY) return "{\"ServerName\":\"Odyssey\"}";
            if (player.tntPlayerInfo != null) return player.tntPlayerInfo.gameInfo;
        }

        return null;
    }
}
