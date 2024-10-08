package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.gamechat.ChatFilter;
import com.jeka8833.tntserver.gamechat.CommandManager;
import com.jeka8833.tntserver.gamechat.GameChatManager;
import com.jeka8833.tntserver.gamechat.PlayerMute;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;

public class ChatPacket implements Packet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatPacket.class);

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
            if (CommandManager.executeCommand(player, text)) {
                return;
            }

            PlayerMute playerMute = PlayerMute.getPlayer(player.uuid);
            if (playerMute != null && playerMute.isMuted()) {
                LOGGER.info("Player {} tried to send \"{}\", but he is banned. Unban after: {}",
                        player.uuid, text, playerMute.unbanAt());

                Duration period = Duration.between(ZonedDateTime.now(), playerMute.unbanAt());

                String muteText = ("§cYou have been muted for: " + playerMute.reason() +
                        ". You will be able to write in " + period.toDaysPart() + " days " + period.toHoursPart() +
                        ":" + period.toMinutesPart() + ":" + period.toSecondsPart() + ".")
                        .replaceAll(" ", " §c");

                TNTServer.serverSend(socket, new ChatPacket(player.uuid, muteText));
                return;
            }

            String filteredText = ChatFilter.filter(player.uuid, text);
            if (filteredText == null) {
                String message = ("§cYou are sending messages too fast, your message has not been delivered. " +
                        "Only you can see this message.").replaceAll(" ", " §c");

                TNTServer.serverSend(socket, new ChatPacket(player.uuid, message));
                return;
            }

            GameChatManager.sendGlobalMessage(player.uuid, player.serverType, filteredText);
        } else {
            socket.close();
        }
    }
}
