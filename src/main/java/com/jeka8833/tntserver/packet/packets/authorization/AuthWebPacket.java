package com.jeka8833.tntserver.packet.packets.authorization;

import com.jeka8833.tntserver.AuthManager;
import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Bot;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
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
    private static final Logger LOGGER = LogManager.getLogger(AuthWebPacket.class);

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
    public void serverProcess(WebSocket socket, User user) {
        AuthManager.authTNTClient(this.user, key, new AuthManager.AuthResponse() {
            @Override
            public void good(@NotNull UUID user, @Nullable Set<String> privileges) {
                if (privileges == null) {
                    socket.close();
                    return;
                }

                User newUser = PlayersDatabase.getOrCreate(user);
                if (newUser instanceof Bot bot) {
                    bot.addPrivileges(privileges);

                    socket.setAttachment(user);

                    Main.serverSend(socket, new AuthWebPacket());

                    LOGGER.info("Bot " + user + " logged in. Current online: " + Main.server.getConnections().size());
                } else {
                    socket.close();
                }
            }

            @Override
            public void bad(int errorCode) {
                socket.close();
            }
        });
    }
}
