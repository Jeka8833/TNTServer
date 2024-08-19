package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.requester.node.RemoteNode;
import com.jeka8833.tntserver.requester.storage.HypixelCompactStorage;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class ReceiveHypixelPlayerV2Packet implements Packet {
    public static final int RECEIVE_FAIL = 0;
    public static final int RECEIVE_GOOD = 1;
    public static final int RECEIVE_GOOD_NOTHING = 2;

    private @Nullable Map<UUID, HypixelCompactStorage> storage;
    private boolean lastPacket = false;

    public ReceiveHypixelPlayerV2Packet() {
    }

    public ReceiveHypixelPlayerV2Packet(@NotNull Map<UUID, HypixelCompactStorage> storage, boolean lastPacket) {
        this.storage = storage;
        this.lastPacket = lastPacket;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (storage == null) throw new NullPointerException("User list is empty");

        stream.writeBoolean(lastPacket);
        stream.writeByte(storage.size());
        for (Map.Entry<UUID, HypixelCompactStorage> player : storage.entrySet()) {
            stream.writeByte(RECEIVE_GOOD);
            stream.writeUUID(player.getKey());
            player.getValue().writeStreamV2(stream);
        }
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        int size = stream.readUnsignedByte();
        for (int i = 0; i < size; i++) {
            int status = stream.readUnsignedByte();
            UUID playerUUID = stream.readUUID();
            if ((status & RECEIVE_GOOD) == RECEIVE_GOOD) {
                RemoteNode.put(playerUUID, HypixelCompactStorage.readStreamV2(stream));
            }
        }
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
    }
}
