package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TokenPacket implements Packet {
    private static final Logger logger = LogManager.getLogger(TokenPacket.class);
    private static final UUID nullUUID = new UUID(0, 0);

    private final UUID user;
    private final UUID key;

    private boolean unregister;

    @SuppressWarnings("unused")
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
    public void serverProcess(WebSocket socket, Player user) {
        if (user == null) {
            socket.close();
            return;
        }

        List<Map.Entry<UUID, BotsManager.BotUser>> bots = BotsManager.getBots("SERVER_TOKEN");
        if (!bots.isEmpty()) {
            var packet = new TokenPacket(user.uuid, unregister ? nullUUID : UUID.randomUUID());

            try {
                for (Map.Entry<UUID, BotsManager.BotUser> entry : bots)
                    Main.serverSend(entry.getValue().connection(), packet);

                Main.serverSend(socket, packet);
                return;
            } catch (Exception e) {
                logger.warn("Fail send to bot", e);
            }
        }

        Main.serverSend(socket, new TokenPacket(nullUUID, nullUUID));
    }
}
