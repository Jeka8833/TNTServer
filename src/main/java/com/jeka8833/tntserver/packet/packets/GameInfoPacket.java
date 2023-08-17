package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;

public class GameInfoPacket implements Packet {

    private String gameInfo;

    @Override
    public void write(PacketOutputStream stream) {
        throw new NullPointerException("Fail read packet");
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        gameInfo = stream.readUTF();
    }

    @Override
    public void serverProcess(WebSocket socket, User user) {
        if (user instanceof Player player) {
            if (player.tntPlayerInfo != null) player.tntPlayerInfo.gameInfo = gameInfo;
        } else {
            socket.close();
        }
    }
}
