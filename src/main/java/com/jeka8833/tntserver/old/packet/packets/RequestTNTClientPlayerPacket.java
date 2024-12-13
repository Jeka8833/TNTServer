package com.jeka8833.tntserver.old.packet.packets;

import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.database.RemoteDB;
import com.jeka8833.tntserver.old.database.storage.Player;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.services.tntclient.user.UserBase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RequestTNTClientPlayerPacket implements Packet {
    private final Set<UUID> users = new HashSet<>();

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) {
        throw new NullPointerException("Fail write packet");
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        byte b = stream.readByte();
        for (int i = 0; i < b; i++) {
            users.add(stream.readUUID());
        }
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        if (user instanceof com.jeka8833.tntserver.services.tntclient.user.player.Player player){
            RemoteDB.readUsers(users, resultUsers -> {
                Player[] players = resultUsers.stream()
                        .filter(player -> player instanceof Player)
                        .map(player -> (Player) player)
                        .toArray(Player[]::new);

                user.sendPacket(new ReceiveTNTClientPlayerPacket(players));
            });
        } else {
            user.disconnect();
        }
    }
}
