package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.database.RemoteDB;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RequestTNTClientPlayerPacket implements Packet {
    private final Set<UUID> users = new HashSet<>();

    @Override
    public void write(PacketOutputStream stream) {
        throw new NullPointerException("Fail write packet");
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        byte b = stream.readByte();
        for (int i = 0; i < b; i++) {
            users.add(stream.readUUID());
        }
    }

    @Override
    public void serverProcess(WebSocket socket, User user) {
        if (user instanceof Player || !BotsManager.isAbsent(user, "TNT_PLAYER_REQUEST")) {
            RemoteDB.readUsers(users, resultUsers -> {
                Player[] players = resultUsers.stream()
                        .filter(player -> player instanceof Player)
                        .map(player -> (Player) player)
                        .toArray(Player[]::new);

                TNTServer.serverSend(socket, new ReceiveTNTClientPlayerPacket(players, false));
            });
        } else {
            socket.close();
        }
    }
}
