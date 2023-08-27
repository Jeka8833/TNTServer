package com.jeka8833.tntserver.packet.packets.authorization;

import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

public class AuthClientDeprecatedPacket implements Packet {
    @Override
    public void write(PacketOutputStream stream) {
    }

    @Override
    public void read(PacketInputStream stream) {
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
    }
}
