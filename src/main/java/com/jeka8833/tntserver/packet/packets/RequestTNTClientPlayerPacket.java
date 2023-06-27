package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.managers.TNTClientDBManager;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RequestTNTClientPlayerPacket implements Packet {

    private final List<UUID> users = new ArrayList<>();

    @Override
    public void write(PacketOutputStream stream) {
        throw new NullPointerException("Fail write packet");
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        final byte b = stream.readByte();
        for (int i = 0; i < b; i++)
            users.add(stream.readUUID());
    }

    @Override
    public void serverProcess(WebSocket socket, Player user) {
        if (user == null) {
            socket.close();
            return;
        }

        TNTClientDBManager.readOrCashUsers(users,
                tntUsers -> Main.serverSend(socket, new ReceiveTNTClientPlayerPacket(tntUsers,
                        user.tntPlayerInfo != null && user.tntPlayerInfo.donate > 50)),
                true);
    }
}
