package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
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
    public void serverProcess(WebSocket socket, final User user) {
        if (user instanceof Player player) {
            if (player.tntPlayerInfo != null) player.tntPlayerInfo.activeModules = activeModules;
        } else {
            socket.close();
        }
    }
}
