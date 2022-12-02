package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.List;

public record SendPlayerStatusPacket(List<TNTUser> users, boolean isAdmin) implements Packet {

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeByte(users.size());
        for (TNTUser user : users) {
            stream.writeUUID(user.user);
            stream.writeBoolean(user.isClient());
            if (user.isClient()) {
                stream.writeByte(user.donate);
                stream.writeByte(switch (user.status) {
                    case TNTUser.STATUS_ONLINE -> 3;
                    case TNTUser.STATUS_AFK -> 2;
                    default -> 0;
                });
                final long isDJFix = user.activeModules & 64L;
                stream.writeLong(isAdmin ? user.activeModules : isDJFix);
                stream.writeUTF(user.version == null ? "" : user.version);
                if (user.gameInfo == null)
                    stream.writeUTF("");
                else
                    stream.writeUTF((isAdmin || user.status > 1) ? user.gameInfo : "");
            }
        }
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        throw new NullPointerException("Fail read packet");
    }

    @Override
    public void serverProcess(WebSocket socket, TNTUser user) {
        throw new NullPointerException("Fail process packet");
    }
}
