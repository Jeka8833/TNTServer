package com.jeka8833.tntserver.old.packet.serverbound;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@RequiredArgsConstructor
public class RequestHypixelUserServerbound implements PacketBase {
    private final @NotNull Set<@NotNull UUID> players;

    public RequestHypixelUserServerbound(@NotNull PacketInputSerializer serializer, int protocolVersion) {
        int size = serializer.readByte() & 0xFF;

        this.players = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            this.players.add(serializer.readUUID());
        }
    }

    @Override
    public void write(@NotNull PacketOutputSerializer serializer, int protocolVersion) {
        serializer.writeByte((byte) players.size());

        for (UUID player : players) {
            serializer.writeUUID(player);
        }
    }
}
