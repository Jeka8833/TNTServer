package com.jeka8833.tntserver.services.tntclient.packet.serverbound;

import com.jeka8833.tntserver.services.tntclient.packet.ServerBoundPacket;
import com.jeka8833.toprotocol.addons.serializer.UUIDSerialize;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class RequestTNTUserServerbound implements ServerBoundPacket {
    private final @NotNull Set<@NotNull UUID> players;

    public RequestTNTUserServerbound(@NotNull Set<@NotNull UUID> players) {
        this.players = players;
    }

    public RequestTNTUserServerbound(@NotNull InputByteArray serializer, Integer protocolVersion) {
        int size = serializer.readByte() & 0xFF;
        this.players = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            this.players.add(UUIDSerialize.readUUID(serializer));
        }
    }

    @Override
    public void write(@NotNull OutputByteArray serializer, Integer protocolVersion) {
        serializer.writeByte((byte) players.size());

        for (UUID player : players) {
            UUIDSerialize.writeUUID(serializer, player);
        }
    }

    public @NotNull Set<@NotNull UUID> getPlayers() {
        return players;
    }
}
