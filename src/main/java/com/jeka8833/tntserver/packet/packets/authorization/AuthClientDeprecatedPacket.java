package com.jeka8833.tntserver.packet.packets.authorization;

import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class AuthClientDeprecatedPacket implements Packet {
    @Override
    public void write(PacketOutputStream stream) throws IOException {
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable Player user) {
        socket.close(); // Authorisation is not supported
    }
}
