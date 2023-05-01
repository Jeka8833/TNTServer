package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.BotsManager;
import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TokenPacket implements Packet {
    private static final Logger logger = LogManager.getLogger(TokenPacket.class);

    private final UUID user;
    private final UUID key;

    private boolean unregister;

    public TokenPacket() {
        this(null, null);
    }

    public TokenPacket(UUID user, UUID key) {
        this.user = user;
        this.key = key;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeUUID(user);
        stream.writeUUID(key);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        unregister = stream.readBoolean();
    }

    @Override
    public void serverProcess(WebSocket socket, TNTUser user) {
        if (user == null) {
            socket.close();
            return;
        }

        List<Map.Entry<UUID, BotsManager.BotUser>> bots = BotsManager.getBots("SERVER_TOKEN");
        if (bots.isEmpty()) {
            Main.serverSend(socket, new TokenPacket(user.uuid, new UUID(0, 0)));
        } else {
            TokenPacket packet = new TokenPacket(user.uuid,
                    unregister ? new UUID(0, 0) : UUID.randomUUID());

            try {
                for (Map.Entry<UUID, BotsManager.BotUser> entry : bots) {
                    Main.serverSend(entry.getValue().connection(), packet);
                }
                Main.serverSend(socket, packet);
            } catch (Exception e) {
                logger.warn("Fail send to bot", e);

                Main.serverSend(socket, new TokenPacket(user.uuid, new UUID(0, 0)));
            }
        }
    }
}
