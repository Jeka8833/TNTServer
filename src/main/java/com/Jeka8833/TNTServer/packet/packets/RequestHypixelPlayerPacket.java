package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.database.Player;
import com.Jeka8833.TNTServer.database.managers.HypixelDBManager;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class RequestHypixelPlayerPacket implements Packet {

    private @Nullable Collection<UUID> userList;
    private int timeout = 0;

    public RequestHypixelPlayerPacket() {
    }

    public RequestHypixelPlayerPacket(@NotNull Collection<UUID> userList, int timeout) {
        this.userList = userList;
        this.timeout = timeout;
    }


    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (userList == null || userList.isEmpty()) throw new NullPointerException("User list is empty");

        stream.writeByte(timeout);
        stream.writeByte(userList.size());
        for (UUID user : userList) {
            stream.writeUUID(user);
        }
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        int size = stream.readUnsignedByte();
        userList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            userList.add(stream.readUUID());
        }
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable Player user) {
        if (userList == null || userList.isEmpty()) throw new NullPointerException("User list is empty");

        HypixelDBManager.requestPlayers(userList.toArray(UUID[]::new), playersReady -> {
            Collection<ReceiveHypixelPlayerPacket.ReceivePlayer> userList = new ArrayList<>(playersReady.ready().size());
            for (Player player : playersReady.ready()) {
                userList.add(new ReceiveHypixelPlayerPacket.ReceivePlayer(player.uuid, player.hypixelPlayerInfo));
            }

            Main.serverSend(socket, new ReceiveHypixelPlayerPacket(userList, playersReady.lastPacket()));
        }, 5_000, 3);
    }
}
