package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.balancer.HypixelBalancer;
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
        if (BotsManager.checkPrivilege(socket, "SERVER_HYPIXEL_API")) {
            HypixelBalancer.TNT_BOT_REQUESTER.setAvailable(socket, count);
        } else {
            HypixelBalancer.TNT_USER_REQUESTER.setAvailable(socket, count);

        }
    }
}
