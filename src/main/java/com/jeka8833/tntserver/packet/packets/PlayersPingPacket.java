package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.TNTPlayerPingStorage;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.user.UserBase;
import com.jeka8833.tntserver.user.player.Player;
import com.jeka8833.tntserver.user.player.PlayerPing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayersPingPacket implements Packet {
    private final List<UUID> requestedPlayers = new ArrayList<>();
    private @Nullable TNTPlayerPingStorage playerPing;

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) throws IOException {
        Player[] playerPings = requestedPlayers.stream()
                .map(PlayersDatabase::getUser)
                .filter(tntUser -> tntUser instanceof Player player &&
                        player.tntPlayerInfo != null && player.tntPlayerInfo.playerPing != null)
                .map(tntUser -> (Player) tntUser)
                .toArray(Player[]::new);

        stream.writeByte(playerPings.length);
        for (Player player : playerPings) {
            stream.writeUUID(player.uuid);

            if (player.tntPlayerInfo != null && player.tntPlayerInfo.playerPing != null) {
                player.tntPlayerInfo.playerPing.writeStream(stream);
            } else {
                new TNTPlayerPingStorage().writeStream(stream);
            }
        }
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        playerPing = new TNTPlayerPingStorage(stream);

        int b = stream.readUnsignedByte();
        for (int i = 0; i < b; i++) requestedPlayers.add(stream.readUUID());
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        if (user instanceof Player player) {
            player.setPlayerPing(playerPing);
            if (player.tntPlayerInfo != null) player.tntPlayerInfo.playerPing = playerPing;
            if (!requestedPlayers.isEmpty()) TNTServer.serverSend(socket, this);
        } else {
            socket.close();
        }
    }

    private static PlayerPing read()
}
