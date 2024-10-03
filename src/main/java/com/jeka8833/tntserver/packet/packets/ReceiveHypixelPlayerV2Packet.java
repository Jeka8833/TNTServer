package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.requester.balancer.node.RemoteNode;
import com.jeka8833.tntserver.requester.storage.HypixelCompactStructure;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReceiveHypixelPlayerV2Packet implements Packet {
    public static final int RECEIVE_FAIL = 0;
    public static final int RECEIVE_GOOD = 1;
    public static final int RECEIVE_GOOD_NOTHING = 2;

    private @Nullable Map<UUID, HypixelCompactStructure> storage;
    private boolean lastPacket = false;

    @SuppressWarnings("unused")
    public ReceiveHypixelPlayerV2Packet() {
    }

    public ReceiveHypixelPlayerV2Packet(@NotNull Map<UUID, HypixelCompactStructure> storage, boolean lastPacket) {
        this.storage = storage;
        this.lastPacket = lastPacket;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (storage == null) throw new NullPointerException("User list is empty");

        stream.writeBoolean(lastPacket);
        stream.writeByte(storage.size());
        for (Map.Entry<UUID, HypixelCompactStructure> player : storage.entrySet()) {
            stream.writeByte(RECEIVE_GOOD);
            stream.writeUUID(player.getKey());
            player.getValue().writeStreamV2(stream);
        }
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        int size = stream.readUnsignedByte();
        storage = new HashMap<>(size);

        for (int i = 0; i < size; i++) {
            int status = stream.readUnsignedByte();
            UUID playerUUID = stream.readUUID();
            if ((status & RECEIVE_GOOD) == RECEIVE_GOOD) {
                storage.put(playerUUID, HypixelCompactStructure.readStreamV2(stream));
            } else if ((status & RECEIVE_GOOD_NOTHING) == RECEIVE_GOOD_NOTHING) {
                storage.put(playerUUID, HypixelCompactStructure.EMPTY_INSTANCE);
            }
        }
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        if (user == null) {
            socket.close();

            return;
        }

        if (storage == null) return;

        for (Map.Entry<UUID, HypixelCompactStructure> player : storage.entrySet()) {
            RemoteNode.put(player.getKey(), player.getValue());
        }
    }
}
