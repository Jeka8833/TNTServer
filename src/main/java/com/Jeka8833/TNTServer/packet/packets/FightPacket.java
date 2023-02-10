package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class FightPacket implements Packet {

    private int playerFight = 0;

    private final Collection<WebSocket> activeConnection;

    public FightPacket() {
        this(null);
    }

    public FightPacket(Collection<WebSocket> activeConnection) {
        this.activeConnection = activeConnection;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        final List<TNTUser> users = activeConnection.stream()
                .map(webSocket -> {
                    UUID user = webSocket.getAttachment();
                    if(user == null) return null;
                    return TNTUser.uuid2User.get(user);
                })
                .filter(tntUser -> tntUser != null && tntUser.fight != 0).toList();

        stream.writeByte(users.size());
        for (TNTUser user : users) {
            stream.writeUUID(user.user);
            stream.writeInt(user.fight);
        }
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        playerFight = stream.readInt();
    }

    @Override
    public void serverProcess(WebSocket socket, TNTUser user) {
        user.fight = playerFight;
        Main.serverSend(socket, new FightPacket(Main.server.getConnections()));
    }
}
