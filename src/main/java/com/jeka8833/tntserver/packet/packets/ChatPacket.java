package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.RemoteDB;
import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.gamechat.ChatFilter;
import com.jeka8833.tntserver.gamechat.CommandManager;
import com.jeka8833.tntserver.gamechat.GameChatManager;
import com.jeka8833.tntserver.gamechat.MuteDto;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class ChatPacket implements Packet {
    private @NotNull UUID user;
    private @NotNull String text;

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
            if (CommandManager.executeCommand(player, text)) return;

            RemoteDB.readMute(player.uuid, muteDtoOptional -> {
                if (muteDtoOptional.isPresent() && muteDtoOptional.get().isMuted()) {
                    MuteDto muteDto = muteDtoOptional.get();

                    log.info("Player {} tried to send \"{}\", but he is banned. Unban after: {}",
                            player.uuid, text, muteDto.unmuteTime());

                    Duration period = Duration.between(Instant.now(), muteDto.unmuteTime());

                    String muteText = ("§cYou have been muted for: " + muteDto.reason() +
                            ". You will be able to write in " + period.toDaysPart() + " days " + period.toHoursPart() +
                            ":" + period.toMinutesPart() + ":" + period.toSecondsPart() + ".")
                            .replaceAll(" ", " §c");

                    TNTServer.serverSend(socket, new ChatPacket(player.uuid, muteText));
                } else {
                    String filteredText = ChatFilter.filter(player.uuid, text);
                    if (filteredText == null) {
                        String message = ("§cYou are sending messages too fast, your message has not been delivered. " +
                                "Only you can see this message.").replaceAll(" ", " §c");

                        TNTServer.serverSend(socket, new ChatPacket(player.uuid, message));
                    } else {
                        GameChatManager.sendGlobalMessage(player.uuid, player.serverType, filteredText,
                                false);
                    }
                }
            });
        } else {
            socket.close();
        }
    }
}
