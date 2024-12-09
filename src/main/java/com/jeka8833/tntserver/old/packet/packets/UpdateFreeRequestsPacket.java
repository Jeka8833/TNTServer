package com.jeka8833.tntserver.old.packet.packets;

import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.old.requester.balancer.NodeRegisterManager;
import com.jeka8833.tntserver.old.user.UserBase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class UpdateFreeRequestsPacket implements Packet {
    private int count;

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) {
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        count = stream.readInt();
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        NodeRegisterManager.connect(user.getSocket());
        NodeRegisterManager.setAvailableCount(user.getSocket(), count);
    }
}
