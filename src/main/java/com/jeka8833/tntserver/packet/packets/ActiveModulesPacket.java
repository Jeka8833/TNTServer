package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.util.Util;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ActiveModulesPacket implements Packet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveModulesPacket.class);

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
            if (player.tntPlayerInfo != null) {
                if (Util.isDJFix(player.tntPlayerInfo.activeModules) != Util.isDJFix(activeModules)) {
                    LOGGER.info("Player {} change DJFix: {}", player.uuid, Util.isDJFix(activeModules));
                }

                player.tntPlayerInfo.activeModules = activeModules;
            }
        } else {
            socket.close();
        }
    }
}
