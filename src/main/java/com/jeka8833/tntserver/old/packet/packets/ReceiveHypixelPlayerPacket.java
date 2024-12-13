package com.jeka8833.tntserver.old.packet.packets;

import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.old.requester.balancer.node.RemoteNode;
import com.jeka8833.tntserver.old.requester.storage.HypixelCompactStructure;
import com.jeka8833.tntserver.services.tntclient.user.UserBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReceiveHypixelPlayerPacket implements Packet {
    public static final int RECEIVE_FAIL = 0;
    public static final int RECEIVE_GOOD = 1;
    public static final int RECEIVE_GOOD_NOTHING = 2;

    private @Nullable Map<UUID, HypixelCompactStructure> storage;
    private boolean lastPacket = false;

    public ReceiveHypixelPlayerPacket() {
    }

    public ReceiveHypixelPlayerPacket(@NotNull Map<UUID, HypixelCompactStructure> storage, boolean lastPacket) {
        this.storage = storage;
        this.lastPacket = lastPacket;
    }

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) throws IOException {
        if (storage == null) throw new NullPointerException("User list is empty");

        stream.writeBoolean(lastPacket);
        stream.writeByte(storage.size());
        for (Map.Entry<UUID, HypixelCompactStructure> player : storage.entrySet()) {
            stream.writeByte(RECEIVE_GOOD);
            stream.writeUUID(player.getKey());
            player.getValue().writeStream(stream);
        }
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        int size = stream.readUnsignedByte();
        storage = new HashMap<>(size);

        for (int i = 0; i < size; i++) {
            int status = stream.readUnsignedByte();
            UUID playerUUID = stream.readUUID();
            if ((status & RECEIVE_GOOD) == RECEIVE_GOOD) {
                storage.put(playerUUID, HypixelCompactStructure.readStream(stream));
            } else if ((status & RECEIVE_GOOD_NOTHING) == RECEIVE_GOOD_NOTHING) {
                storage.put(playerUUID, HypixelCompactStructure.EMPTY_INSTANCE);
            }
        }
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        if (storage == null) return;

        for (Map.Entry<UUID, HypixelCompactStructure> player : storage.entrySet()) {
            RemoteNode.put(player.getKey(), player.getValue());
        }
    }
}
