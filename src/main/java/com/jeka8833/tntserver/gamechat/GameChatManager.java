package com.jeka8833.tntserver.gamechat;

import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.Bot;
import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.packets.ChatPacket;
import com.jeka8833.tntserver.packet.packets.discordbot.ChatHookPacket;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

public class GameChatManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameChatManager.class);

    public static void sendDirectMessage(@NotNull UUID sender, @NotNull UUID receiver, @NotNull String message) {
        User user = PlayersDatabase.getUser(receiver);
        if (user instanceof Player player) {
            try {
                WebSocket webSocket = player.getSocket();

                if (webSocket != null) TNTServer.serverSend(webSocket, new ChatPacket(sender, message));
            } catch (Exception e) {
                LOGGER.error("Fail send packet:", e);
            }

            sendToHook(sender, receiver, player.serverType, message);
        }
    }

    public static void sendGlobalMessage(@NotNull UUID sender,
                                         @NotNull ServerType destination, @NotNull String message) {
        var packet = new ChatPacket(sender, message);

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
                    LOGGER.error("Fail send packet:", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Fail generate packet:", e);
        }

        sendToHook(sender, null, destination, message);
    }

    public static void sendToHook(@NotNull UUID sender, @Nullable UUID receiver,
                                  @NotNull ServerType server, @NotNull String text) {
        LOGGER.info("Chat: {} -> {} {}: {}", sender, receiver, server, text);

        Iterable<Bot> bots = PlayersDatabase.getBotsWithPrivilege("SERVER_CHAT");

        var packet = new ChatHookPacket(sender, receiver, server, text);

        for (Bot bot : bots) {
            WebSocket webSocket = bot.getSocket();
            if (webSocket == null) continue;

            TNTServer.serverSend(webSocket, packet);
        }
    }
}
