package com.jeka8833.tntserver.old.packet.serverbound;

import com.jeka8833.tntserver.old.user.player.PlayerPing;
import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record PlayerPingServerbound(@NotNull PlayerPing playerPing,
                                    @NotNull Set<@NotNull UUID> requestedPlayers) implements PacketBase {
    public PlayerPingServerbound(@NotNull PacketInputSerializer serializer, int protocolVersion) {
        PlayerPing playerPing = readPlayerPing(serializer, protocolVersion);

        int size = serializer.readByte() & 0xFF;
        Set<UUID> requestedPlayers = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            requestedPlayers.add(serializer.readUUID());
        }

        this(playerPing, requestedPlayers);
    }

    @Override
    public void write(@NotNull PacketOutputSerializer serializer, int protocolVersion) {
        writePlayerPing(serializer, protocolVersion, playerPing);

        serializer.writeByte((byte) requestedPlayers.size());
        for (UUID uuid : requestedPlayers) {
            serializer.writeUUID(uuid);
        }
    }

    @NotNull
    private static PlayerPing readPlayerPing(@NotNull PacketInputSerializer serializer, int protocolVersion) {
        short playerPing = serializer.readShort();
        short serverPing = serializer.readShort();
        serializer.skip(8);   // Download and upload speed
        short jumpPing = serializer.readShort();
        short blockReactionTime = serializer.readShort();

        return new PlayerPing(playerPing, serverPing, jumpPing, blockReactionTime);
    }

    private static void writePlayerPing(@NotNull PacketOutputSerializer serializer, int protocolVersion,
                                        @NotNull PlayerPing playerPing) {
        serializer.writeShort(playerPing.playerPing());
        serializer.writeShort(playerPing.serverPing());
        serializer.writeInt(0);
        serializer.writeInt(0);
        serializer.writeShort(playerPing.jumpPing());
        serializer.writeShort(playerPing.blockReactionTime());
    }
}
