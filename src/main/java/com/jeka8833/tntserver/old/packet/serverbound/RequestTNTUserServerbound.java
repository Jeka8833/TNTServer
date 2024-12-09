package com.jeka8833.tntserver.old.packet.serverbound;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record RequestTNTUserServerbound(@NotNull Set<@NotNull UUID> players) implements PacketBase {
    public RequestTNTUserServerbound(@NotNull PacketInputSerializer serializer, int protocolVersion) {
        int size = serializer.readByte() & 0xFF;
        Set<UUID> players = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            players.add(serializer.readUUID());
        }

        this(players);
    }

    @Override
    public void write(@NotNull PacketOutputSerializer serializer, int protocolVersion) {
        serializer.writeByte((byte) players.size());

        for (UUID player : players) {
            serializer.writeUUID(player);
        }
    }
}
