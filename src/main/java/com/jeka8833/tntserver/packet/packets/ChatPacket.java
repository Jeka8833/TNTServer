package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Bot;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.gamechat.ChatFilter;
import com.jeka8833.tntserver.gamechat.PlayerMute;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.UUID;

public class ChatPacket implements Packet {
    private static final Logger logger = LogManager.getLogger(ChatPacket.class);

    private UUID user;
    private String text;

    @SuppressWarnings("unused")
    public ChatPacket() {
    }

    public ChatPacket(UUID user, String text) {
        this.user = user;
        this.text = text;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeUUID(user);
        stream.writeUTF(text);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        text = stream.readUTF();
    }

    @Override
    public void serverProcess(WebSocket socket, User user) {
        ChatFilter.clearOld();

        if (user instanceof Player player) {
            PlayerMute playerMute = PlayerMute.getPlayer(player.uuid);
            if (playerMute != null && playerMute.isMuted()) {
                logger.info("Player " + player.uuid + " tried to send message, but he is banned. Unban after: " +
                        playerMute.unbanAt());

                Duration period = Duration.between(ZonedDateTime.now(), playerMute.unbanAt());

                String muteText = ("§cYou have been muted for: " + playerMute.reason() +
                        ". You will be able to write in " + period.toDaysPart() + " days " + period.toHoursPart() +
                        ":" + period.toMinutesPart() + ":" + period.toSecondsPart() + ".")
                        .replaceAll(" ", " §c");

                Main.serverSend(socket, new ChatPacket(player.uuid, muteText));
                return;
            }

            String filteredText = ChatFilter.filter(player.uuid, text);
            if (filteredText == null) {
                String message = ("§cYou are sending messages too fast, your message has not been delivered. " +
                        "Only you can see this message.").replaceAll(" ", " §c");

                Main.serverSend(socket, new ChatPacket(player.uuid, message));
                return;
            }

            sendMessage(player, filteredText);

            Collection<Bot> bots = PlayersDatabase.getBotsWithPrivilege("SERVER_CHAT");

            var packet = new ChatPacket(player.uuid, filteredText);

            for (Bot bot : bots) {
                WebSocket webSocket = bot.getSocket();
                if (webSocket == null) continue;

                Main.serverSend(webSocket, packet);
            }
        } else {
            socket.close();
        }
    }

    private static void sendMessage(Player player, String message) {
        var packet = new ChatPacket(player.uuid, message);

        try (final PacketOutputStream stream = new PacketOutputStream()) {
            packet.write(stream);
            ByteBuffer send = stream.getByteBuffer(packet.getClass());

            for (WebSocket client : Main.server.getConnections()) {
                try {
                    User user = PlayersDatabase.getUser(client.getAttachment());
                    if (user instanceof Player toPlayer && player.serverType.equals(toPlayer.serverType)) {
                        if (client.isOpen()) client.send(send);
                    }
                } catch (Exception e) {
                    logger.error("Fail send packet:", e);
                }
            }
        } catch (Exception e) {
            logger.error("Fail generate packet:", e);
        }
    }
}
