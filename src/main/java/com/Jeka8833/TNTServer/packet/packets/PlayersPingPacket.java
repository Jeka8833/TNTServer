package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.dataBase.TNTClientDBManager;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import com.Jeka8833.TNTServer.util.PlayerPing;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayersPingPacket implements Packet {

    private @Nullable PlayerPing playerPing;
    private final List<UUID> requestedPlayers = new ArrayList<>();

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        TNTUser[] playerPings = requestedPlayers.stream()
                .map(TNTClientDBManager::getUser)
                .filter(tntUser -> tntUser != null && tntUser.ping != null)
                .toArray(TNTUser[]::new);

        stream.writeByte(playerPings.length);
        for (TNTUser player : playerPings) {
            stream.writeUUID(player.uuid);
            if (player.ping == null) {
                stream.writeShort(0);
                stream.writeShort(0);
                stream.writeInt(0);
                stream.writeInt(0);
                stream.writeShort(0);
                stream.writeShort(0);
            } else {
                stream.writeShort(player.ping.userPing);
                stream.writeShort(player.ping.serverPing);
                stream.writeInt(player.ping.serverDownloadSpeed);
                stream.writeInt(player.ping.serverUploadSpeed);
                stream.writeShort(player.ping.jumpPing);
                stream.writeShort(player.ping.blockReactionTime);
            }
        }
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        int userPing = stream.readUnsignedShort();
        int serverPing = stream.readUnsignedShort();
        int serverDownloadSpeed = stream.readInt();
        int serverUploadSpeed = stream.readInt();
        int jumpPing = stream.readUnsignedShort();
        int blockReactionTime = stream.readUnsignedShort();

        final byte b = stream.readByte();
        for (int i = 0; i < b; i++) requestedPlayers.add(stream.readUUID());
        playerPing = new PlayerPing(userPing, serverPing, serverDownloadSpeed, serverUploadSpeed, jumpPing, blockReactionTime);
    }

    @Override
    public void serverProcess(WebSocket socket, TNTUser user) {
        if (user == null) {
            socket.close();
            return;
        }

        user.ping = playerPing;

        if (!requestedPlayers.isEmpty()) Main.serverSend(socket, this);
    }
}
