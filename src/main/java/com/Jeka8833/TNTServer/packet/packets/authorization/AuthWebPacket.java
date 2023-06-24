package com.Jeka8833.TNTServer.packet.packets.authorization;

import com.Jeka8833.TNTServer.AuthManager;
import com.Jeka8833.TNTServer.BotsManager;
import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.database.Player;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import com.Jeka8833.TNTServer.packet.packets.TokenPacket;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class AuthWebPacket implements Packet {
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

                Main.serverSend(socket,
                        new TokenPacket(UUID.fromString("6bd6e833-a80a-430e-9029-4786368811f9"),
                                UUID.fromString("6bd6e833-a80a-430e-9029-4786368811f9")));
            }

            @Override
            public void bad(int errorCode) {
                socket.close();
            }
        });
    }
}
