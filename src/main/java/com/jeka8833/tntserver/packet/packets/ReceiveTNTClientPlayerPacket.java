package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.database.storage.TNTPlayerStorage;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;

public record ReceiveTNTClientPlayerPacket(Player[] users, boolean isAdmin) implements Packet {
    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeByte(users.length);
        for (Player user : users) {
            stream.writeUUID(user.uuid);
            boolean inTNTDatabase = user.tntPlayerInfo != null;
            stream.writeBoolean(inTNTDatabase);
            if (inTNTDatabase) {
                stream.writeByte(user.tntPlayerInfo.donate);
                stream.writeByte(switch (user.tntPlayerInfo.status) {
                    case TNTPlayerStorage.STATUS_ONLINE -> 3;
                    case TNTPlayerStorage.STATUS_AFK -> 2;
                    default -> 0;
                });
                final long isDJFix = user.tntPlayerInfo.activeModules & 64L;
                stream.writeLong(isAdmin ? user.tntPlayerInfo.activeModules : isDJFix);
                stream.writeUTF(user.tntPlayerInfo.version == null ? "" : user.tntPlayerInfo.version);
                if (user.tntPlayerInfo.gameInfo == null)
                    stream.writeUTF("");
                else
                    stream.writeUTF((isAdmin || user.tntPlayerInfo.status > 1) ? user.tntPlayerInfo.gameInfo : "");
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
