package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.requester.NodeRegisterManager;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class UpdateFreeRequestsPacket implements Packet {
    private int count;

    @Override
    public void write(PacketOutputStream stream) {
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        count = stream.readInt();
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        NodeRegisterManager.connect(socket);
        NodeRegisterManager.setAvailableCount(socket, count);
    }
}
