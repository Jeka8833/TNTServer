package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.dataBase.TNTClientDBManager;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RequestPlayerStatusPacket implements Packet {

    private final List<UUID> users = new ArrayList<>();

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        throw new NullPointerException("Fail write packet");
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        final byte b = stream.readByte();
        for (int i = 0; i < b; i++)
            users.add(stream.readUUID());
    }

    @Override
    public void serverProcess(WebSocket socket, TNTUser user) {
        TNTClientDBManager.readOrCashUser(users, tntUsers ->
                Main.serverSend(socket, new SendPlayerStatusPacket(tntUsers, user.donate > 50)), true);
    }
}
