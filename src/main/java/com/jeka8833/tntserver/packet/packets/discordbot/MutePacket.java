package com.jeka8833.tntserver.packet.packets.discordbot;

import com.jeka8833.tntserver.database.Bot;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.gamechat.PlayerMute;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

public class MutePacket implements Packet {
    private PlayerMute[] players;

    @SuppressWarnings("unused")
    public MutePacket() {
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {

    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        int i = stream.readInt();
        players = new PlayerMute[i];
        for (int j = 0; j < i; j++) {
            UUID user = stream.readUUID();
            String description = stream.readUTF();
            String unmuteTime = stream.readUTF();

            players[j] = new PlayerMute(user, ZonedDateTime.parse(unmuteTime), description);
        }
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        if (user instanceof Bot bot && bot.hasPrivilege("SERVER_CHAT_MUTE")) {
            PlayerMute.setMuteList(players);
        } else {
            socket.close();
        }
    }
}
