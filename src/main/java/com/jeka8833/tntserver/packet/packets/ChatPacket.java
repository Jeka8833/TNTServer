package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.RemoteDB;
import com.jeka8833.tntserver.gamechat.ChatFilter;
import com.jeka8833.tntserver.gamechat.CommandManager;
import com.jeka8833.tntserver.gamechat.GameChatManager;
import com.jeka8833.tntserver.gamechat.MuteDto;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.user.UserBase;
import com.jeka8833.tntserver.user.player.Player;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public void write(PacketOutputStream stream, int protocolVersion) throws IOException {
        stream.writeUUID(user);
        stream.writeUTF(text);
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        text = stream.readUTF();
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        ChatFilter.clearOld();

        if (user instanceof Player player) {
            if (CommandManager.executeCommand(player, text)) return;

            RemoteDB.readMute(player.getUuid(), muteDtoOptional -> {
                if (muteDtoOptional.isPresent() && muteDtoOptional.get().isMuted()) {
                    MuteDto muteDto = muteDtoOptional.get();

                    log.info("Player {} tried to send \"{}\", but he is banned. Unban after: {}",
                            player.getUuid(), text, muteDto.unmuteTime());

                    Duration period = Duration.between(Instant.now(), muteDto.unmuteTime());

                    String muteText = ("§cYou have been muted for: " + muteDto.reason() +
                            ". You will be able to write in " + period.toDaysPart() + " days " + period.toHoursPart() +
                            ":" + period.toMinutesPart() + ":" + period.toSecondsPart() + ".")
                            .replaceAll(" ", " §c");

                    player.sendPacket(new ChatPacket(player.getUuid(), muteText));
                } else {
                    String filteredText = ChatFilter.filter(player.getUuid(), text);
                    if (filteredText == null) {
                        String message = ("§cYou are sending messages too fast, your message has not been delivered. " +
                                "Only you can see this message.").replaceAll(" ", " §c");

                        player.sendPacket(new ChatPacket(player.getUuid(), message));
                    } else {
                        GameChatManager.sendGlobalMessage(player.getUuid(), player.getGameServer(), filteredText,
                                false);
                    }
                }
            });
        } else {
            user.disconnect();
        }
    }
}
