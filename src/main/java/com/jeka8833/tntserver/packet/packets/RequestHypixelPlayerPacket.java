package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.balancer.HypixelBalancer;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class RequestHypixelPlayerPacket implements Packet {

    private @Nullable Collection<UUID> userList;

    public RequestHypixelPlayerPacket() {
    }

    public RequestHypixelPlayerPacket(@NotNull Collection<UUID> userList) {
        this.userList = userList;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (userList == null || userList.isEmpty()) throw new NullPointerException("User list is empty");

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
        if (user == null) {
            socket.close();
            return;
        }

        if (userList == null || userList.isEmpty()) throw new NullPointerException("User list is empty");

        HypixelBalancer.tryGet(user.uuid, userList, playersReady -> {
            Collection<ReceiveHypixelPlayerPacket.ReceivePlayer> userList = new ArrayList<>(playersReady.ready().size());
            for (Player player : playersReady.ready()) {
                userList.add(new ReceiveHypixelPlayerPacket.ReceivePlayer(player.uuid, player.hypixelPlayerInfo));
            }
            Main.serverSend(socket, new ReceiveHypixelPlayerPacket(userList, playersReady.lastPacket()));
        }, 5, 3);
    }
}
