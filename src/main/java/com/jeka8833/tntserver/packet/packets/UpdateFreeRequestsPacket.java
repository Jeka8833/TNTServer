package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.balancer.HypixelTNTRequest;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class UpdateFreeRequestsPacket implements Packet {

    private int count;

    @Override
    public void write(PacketOutputStream stream) throws IOException {
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        count = stream.readInt();
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable Player user) {
        HypixelTNTRequest.setAvailable(socket, count);
    }
}
