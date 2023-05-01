package com.Jeka8833.TNTServer;

import com.google.gson.reflect.TypeToken;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BotsManager {
    private static final String BOT_LABEL = "TNTCLIENT_SERVER";

    private static final Map<UUID, BotUser> BOTS = new ConcurrentHashMap<>();

    private static final Type setType = new TypeToken<HashSet<String>>() {
    }.getType();

    @Nullable
    @Contract("null->null")
    public static Set<String> validateBotAndCutPrivilege(@Nullable String privileges) {
        if (privileges == null) return null;

        Set<String> privilegesArray = Main.GSON.fromJson(privileges, setType);
        if (!privilegesArray.remove(BOT_LABEL)) return null;

        return Collections.unmodifiableSet(privilegesArray);
    }

    public static void addBot(@NotNull UUID user, @NotNull Set<String> privileges, @NotNull WebSocket connection) {
        BOTS.put(user, new BotUser(privileges, connection));
    }

    public static boolean checkPrivilege(@NotNull WebSocket socket, @NotNull String privilege) {
        UUID botUUID = socket.getAttachment();
        if (botUUID == null) return false;

        BotUser botUser = BOTS.get(botUUID);
        if (botUser == null) return false;

        return botUser.privileges.contains(privilege);
    }

    @NotNull
    @Contract("_->new")
    public static List<Map.Entry<UUID, BotUser>> getBots(@NotNull String privilege) {
        return BOTS.entrySet().stream()
                .filter(entry -> entry.getValue().privileges.contains(privilege))
                .toList();
    }

    public static void clearDisconnectedBots() {
        BOTS.values().removeIf(botUser -> !botUser.connection.isOpen());
    }

    public record BotUser(@NotNull Set<String> privileges, @NotNull WebSocket connection) {
    }
}
