package com.jeka8833.tntserver.packet.packets.authorization;

import com.jeka8833.tntserver.AuthManager;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.Bot;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.user.UserBase;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@NoArgsConstructor
public class AuthBotPacket implements Packet {
    private UUID user;
    private UUID key;

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) {
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        user = stream.readUUID();
        key = stream.readUUID();
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
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

                    for (WebSocket oldSocket : TNTServer.server.getConnections()) {
                        UUID oldUser = oldSocket.getAttachment();
                        if (user.equals(oldUser)) {
                            oldSocket.close();
                        }
                    }

                    socket.setAttachment(user);

                    TNTServer.serverSend(socket, new AuthBotPacket());

                    log.info("Bot {} logged in.", user);
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
