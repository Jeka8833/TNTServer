package com.jeka8833.tntserver.gamechat;

import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.Bot;
import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.packets.ChatPacket;
import com.jeka8833.tntserver.packet.packets.webendpoints.ChatHookPacket;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class GameChatManager {
    private static final UUID EMPTY_UUID = new UUID(0, 0);

    public static void sendDirectMessage(@Nullable UUID sender, @Nullable UUID receiver, @NotNull String message,
                                         boolean isSystemMessage) {
        User user = PlayersDatabase.getUser(receiver);
        if (user instanceof Player player) {
            try {
                WebSocket webSocket = player.getSocket();

                if (webSocket != null) {
                    TNTServer.serverSend(webSocket,
                            new ChatPacket(Objects.requireNonNullElse(sender, EMPTY_UUID), message));
                }
            } catch (Exception e) {
                log.error("Fail send packet:", e);
            }

            if (isSystemMessage) return;
            sendToHook(sender, receiver, player.serverType, message);
        }
    }

    public static void sendGlobalMessage(@Nullable UUID sender, @NotNull ServerType destination, @NotNull String message,
                                         boolean isSystemMessage) {
        var packet = new ChatPacket(Objects.requireNonNullElse(sender, EMPTY_UUID), message);

        try (final PacketOutputStream stream = new PacketOutputStream()) {
            packet.write(stream);
            ByteBuffer send = stream.getByteBuffer(packet.getClass());

            for (WebSocket client : TNTServer.server.getConnections()) {
                try {
                    User user = PlayersDatabase.getUser(client.getAttachment());
                    if (user instanceof Player toPlayer &&
                            (ServerType.UNKNOWN.equals(destination) ||
                                    destination.equals(toPlayer.serverType))) {
                        if (client.isOpen()) client.send(send);
                    }
                } catch (Exception e) {
                    log.error("Fail send packet:", e);
                }
            }
        } catch (Exception e) {
            log.error("Fail generate packet:", e);
        }

        if (isSystemMessage) return;
        sendToHook(sender, null, destination, message);
    }

    public static void sendToHook(@Nullable UUID sender, @Nullable UUID receiver,
                                  @NotNull ServerType server, @NotNull String text) {
        log.info("Chat: {} -> {} {}: {}", sender, receiver, server, text);

        Iterable<Bot> bots = PlayersDatabase.getBotsWithPrivilege("SERVER_CHAT");

        var packet = new ChatHookPacket(sender, receiver, server, text);

        for (Bot bot : bots) {
            WebSocket webSocket = bot.getSocket();
            if (webSocket == null) continue;

            TNTServer.serverSend(webSocket, packet);
        }
    }
}
