package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.storage.TNTPlayerStorage;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.Objects;

public record ReceiveTNTClientPlayerPacket(Player[] users) implements Packet {
    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeByte(users.length);
        for (Player user : users) {
            stream.writeUUID(user.uuid);

            boolean inTNTDatabase = user.tntPlayerInfo != null;
            stream.writeBoolean(inTNTDatabase);
            if (inTNTDatabase) {
                boolean isOffline = user.tntPlayerInfo.status == TNTPlayerStorage.STATUS_OFFLINE ||
                        user.tntPlayerInfo.status == TNTPlayerStorage.STATUS_INVISIBLE;

                long isDJFix = user.tntPlayerInfo.activeModules & (1L << 6L | 1L << 29L | 1L << 30L);

                stream.writeByte(user.tntPlayerInfo.donate);

                stream.writeByte(isOffline ? TNTPlayerStorage.STATUS_OFFLINE : user.tntPlayerInfo.status);

                stream.writeLong(isDJFix);

                stream.writeUTF(Objects.requireNonNullElse(user.tntPlayerInfo.version, ""));

                stream.writeUTF(Objects.requireNonNullElse(
                        isOffline ? null : user.tntPlayerInfo.gameInfo, ""));
            }
        }
    }

    @Override
    public void read(PacketInputStream stream) {
    }

    @Override
    public void serverProcess(WebSocket socket, User user) {
        socket.close();
    }
}
