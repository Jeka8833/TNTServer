package com.jeka8833.tntserver.services.tntclient.packet.serverbound;

import com.jeka8833.tntserver.services.tntclient.packet.ServerBoundPacket;
import com.jeka8833.tntserver.services.tntclient.user.player.PlayerPing;
import com.jeka8833.toprotocol.addons.serializer.UUIDSerialize;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerPingServerbound implements ServerBoundPacket {
    private final @NotNull PlayerPing playerPing;
    private final @NotNull Set<@NotNull UUID> requestedPlayers;

    public PlayerPingServerbound(@NotNull PlayerPing playerPing,
                                 @NotNull Set<@NotNull UUID> requestedPlayers) {
        this.playerPing = playerPing;
        this.requestedPlayers = requestedPlayers;
    }

    public PlayerPingServerbound(@NotNull InputByteArray serializer, Integer protocolVersion) {
        this.playerPing = readPlayerPing(serializer, protocolVersion);

        int size = serializer.readByte() & 0xFF;
        this.requestedPlayers = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            this.requestedPlayers.add(UUIDSerialize.readUUID(serializer));
        }
    }

    @Override
    public void write(@NotNull OutputByteArray serializer, Integer protocolVersion) {
        writePlayerPing(serializer, protocolVersion, playerPing);

        serializer.writeByte((byte) requestedPlayers.size());
        for (UUID uuid : requestedPlayers) {
            UUIDSerialize.writeUUID(serializer, uuid);
        }
    }

    @NotNull
    private static PlayerPing readPlayerPing(@NotNull InputByteArray serializer, int protocolVersion) {
        short playerPing = serializer.readShort();
        short serverPing = serializer.readShort();
        serializer.skip(8);   // Download and upload speed
        short jumpPing = serializer.readShort();
        short blockReactionTime = serializer.readShort();

        return new PlayerPing(playerPing, serverPing, jumpPing, blockReactionTime);
    }

    private static void writePlayerPing(@NotNull OutputByteArray serializer, int protocolVersion,
                                        @NotNull PlayerPing playerPing) {
        serializer.writeShort(playerPing.playerPing());
        serializer.writeShort(playerPing.serverPing());
        serializer.writeInt(0);
        serializer.writeInt(0);
        serializer.writeShort(playerPing.jumpPing());
        serializer.writeShort(playerPing.blockReactionTime());
    }

    public @NotNull PlayerPing getPlayerPing() {
        return playerPing;
    }

    public @NotNull Set<@NotNull UUID> getRequestedPlayers() {
        return requestedPlayers;
    }
}
