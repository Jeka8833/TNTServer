package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
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
    public void serverProcess(WebSocket socket, TNTUser user) {
        user.gameInfo = gameInfo;
    }
}
