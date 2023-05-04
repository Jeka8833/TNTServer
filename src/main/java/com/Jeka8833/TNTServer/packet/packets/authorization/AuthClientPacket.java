package com.Jeka8833.TNTServer.packet.packets.authorization;

import com.Jeka8833.TNTServer.AuthManager;
import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.dataBase.TNTClientDBManager;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import com.Jeka8833.TNTServer.packet.packets.BlockModulesPacket;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class AuthClientPacket implements Packet {
    private String playerUsername;
    private String serverKey;
    private String version;

    @Override
    public void write(PacketOutputStream stream) {
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        playerUsername = stream.readUTF();
        serverKey = stream.readUTF();
        version = stream.readUTF();
    }

    @Override
    public void serverProcess(WebSocket socket, TNTUser user) {
        AuthManager.authMojang(playerUsername, serverKey, new AuthManager.AuthResponse() {
            @Override
            public void good(@NotNull UUID user, @Nullable Set<String> privileges) {
                socket.setAttachment(user);

                TNTClientDBManager.readOrCashUser(user, ignore -> {
                    TNTUser account = TNTClientDBManager.getOrCreate(user);
                    account.version = version;
                    account.status = TNTUser.STATUS_ONLINE;
                    account.timeLogin = System.currentTimeMillis();

                    TNTClientDBManager.writeUser(user, null);

                    Main.serverSend(socket, new BlockModulesPacket(account.forceBlock, account.forceActive));
                });
            }

            @Override
            public void bad(int errorCode) {
                socket.close();
            }
        });
    }
}
