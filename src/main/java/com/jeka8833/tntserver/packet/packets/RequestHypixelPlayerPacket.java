package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.requester.HypixelCache;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RequestHypixelPlayerPacket implements Packet {
    private @Nullable Set<UUID> userList;

    @SuppressWarnings("unused")
    public RequestHypixelPlayerPacket() {
    }

    public RequestHypixelPlayerPacket(@NotNull Set<UUID> userList) {
        this.userList = userList;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (userList == null || userList.isEmpty()) throw new NullPointerException("User list is empty");

        UUID[] array = userList.toArray(UUID[]::new);
        stream.writeByte(array.length);
        for (UUID user : array) {
            stream.writeUUID(user);
        }
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        int size = stream.readUnsignedByte();
        userList = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            userList.add(stream.readUUID());
        }
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        if (user instanceof Player senderPlayer) {
            if (userList == null || userList.isEmpty()) throw new NullPointerException("User list is empty");

            HypixelCache.get(senderPlayer.uuid, userList,
                    playersReady -> TNTServer.serverSend(socket,
                            new ReceiveHypixelPlayerPacket(playersReady, false)),
                    () -> TNTServer.serverSend(socket,
                            new ReceiveHypixelPlayerPacket(Collections.emptyMap(), true)));
        } else {
            socket.close();
        }
    }
}
