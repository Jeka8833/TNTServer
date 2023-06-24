package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.database.Player;
import com.Jeka8833.TNTServer.database.PlayersDatabase;
import com.Jeka8833.TNTServer.database.storage.TNTPlayerPingStorage;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayersPingPacket implements Packet {

    private @Nullable TNTPlayerPingStorage playerPing;
    private final List<UUID> requestedPlayers = new ArrayList<>();

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        Player[] playerPings = requestedPlayers.stream()
                .map(PlayersDatabase::getUser)
                .filter(tntUser -> tntUser != null && tntUser.tntPlayerInfo != null &&
                        tntUser.tntPlayerInfo.playerPing != null)
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
    public void read(PacketInputStream stream) throws IOException {
        playerPing = new TNTPlayerPingStorage();
        playerPing.readStream(stream);

        int b = stream.readUnsignedByte();
        for (int i = 0; i < b; i++) requestedPlayers.add(stream.readUUID());
    }

    @Override
    public void serverProcess(WebSocket socket, Player user) {
        if (user == null) {
            socket.close();
            return;
        }

        if (user.tntPlayerInfo != null) user.tntPlayerInfo.playerPing = playerPing;
        if (!requestedPlayers.isEmpty()) Main.serverSend(socket, this);
    }
}
