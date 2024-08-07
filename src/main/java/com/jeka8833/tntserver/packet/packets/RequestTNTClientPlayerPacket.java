package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.User;
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
    public void serverProcess(WebSocket socket, User user) {
        if (user instanceof Player || !BotsManager.isAbsent(user, "TNT_PLAYER_REQUEST")) {
            boolean isAdmin = user instanceof Player player &&
                    player.tntPlayerInfo != null && player.tntPlayerInfo.donate > 50;

            TNTClientDBManager.readOrCashUsers(users,
                    tntUsers -> TNTServer.serverSend(socket, new ReceiveTNTClientPlayerPacket(tntUsers, isAdmin)),
                    true);
        } else {
            socket.close();
        }
    }
}
