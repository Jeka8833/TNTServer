package com.jeka8833.tntserver.packet.packets.authorization;

import com.jeka8833.tntserver.AuthManager;
import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class AuthWebPacket implements Packet {
    private static final Logger logger = LogManager.getLogger(AuthWebPacket.class);

    private UUID user;
    private UUID key;

    @Override
    public void write(PacketOutputStream stream) {
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        user = stream.readUUID();
        key = stream.readUUID();
    }

    @Override
    public void serverProcess(WebSocket socket, Player user) {
        AuthManager.authTNTClient(this.user, key, new AuthManager.AuthResponse() {
            @Override
            public void good(@NotNull UUID user, @Nullable Set<String> privileges) {
                if (privileges == null) {
                    socket.close();
                    return;
                }

                socket.setAttachment(user);

                BotsManager.addBot(user, privileges, socket);
                Main.serverSend(socket, new AuthWebPacket());

/*                Main.serverSend(socket,
                        new TokenPacket(UUID.fromString("6bd6e833-a80a-430e-9029-4786368811f9"),
                                UUID.fromString("6bd6e833-a80a-430e-9029-4786368811f9")));*/

                logger.info("Bot " + user + " logged in. Current online: " + Main.server.getConnections().size());
            }

            @Override
            public void bad(int errorCode) {
                socket.close();
            }
        });
    }
}
