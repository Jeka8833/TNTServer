package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;

public class ActiveModulesPacket implements Packet {

    private long activeModules = 0;

    @Override
    public void write(PacketOutputStream stream) {
        throw new NullPointerException("Fail write packet");
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        activeModules = stream.readLong();
    }

    @Override
    public void serverProcess(WebSocket socket, final TNTUser user) {
        if (user == null) {
            socket.close();
            return;
        }

        user.activeModules = activeModules;
    }
}
