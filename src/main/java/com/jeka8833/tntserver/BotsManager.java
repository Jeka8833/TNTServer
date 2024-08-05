package com.jeka8833.tntserver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.jeka8833.tntserver.database.Bot;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.packet.callback.CallbackManager;
import com.jeka8833.tntserver.packet.packets.web.RolePacket;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BotsManager {
    private static final String BOT_LABEL = "TNTCLIENT_SERVER";

    private static final TypeReference<HashSet<String>> HASH_SET_TYPE = new TypeReference<>() {
    };

    @Nullable
    @UnmodifiableView
    @Contract("null->null")
    public static Set<String> validateBotAndCutPrivilege(@Nullable String privileges) {
        if (privileges == null) return null;

        Set<String> privilegesArray = JSON.parseObject(privileges, HASH_SET_TYPE);
        if (!privilegesArray.contains(BOT_LABEL)) return null;

        return Collections.unmodifiableSet(privilegesArray);
    }

    @Contract(value = "null, _ -> true; _, null -> true", pure = true)
    public static boolean isAbsent(@Nullable User user, @Nullable String privilege) {
        if (user instanceof Bot bot) {
            return !bot.hasPrivilege(privilege);
        }
        return true;
    }

    public static void requestUserPrivileges(@NotNull UUID user, @NotNull Consumer<Optional<Set<String>>> callback) {
        Bot roleServer = PlayersDatabase.getBotWithPrivilege(BOT_LABEL);
        if (roleServer == null) {
            callback.accept(Optional.empty());
            return;
        }

        WebSocket roleServerSocket = roleServer.getSocket();
        if (roleServerSocket == null) {
            callback.accept(Optional.empty());

            return;
        }

        CallbackManager.sendAndGetResult(roleServerSocket, new RolePacket(user), packet -> {
            if (packet == null) {
                callback.accept(Optional.empty());
            } else {
                if (packet.getRoles() == null) {
                    callback.accept(Optional.empty());
                    return;
                }

                callback.accept(Optional.of(Arrays.stream(packet.getRoles().split(",")).collect(Collectors.toSet())));
            }
        });
    }
}
