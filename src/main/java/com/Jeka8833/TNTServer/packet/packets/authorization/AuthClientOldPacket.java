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

@Deprecated
public class AuthClientOldPacket implements Packet {
    private UUID user;
    private UUID key;
    private String version;

    private int closeCode;
    private String closeMessage;

    public AuthClientOldPacket() {
    }

    public AuthClientOldPacket(int closeCode, String closeMessage) {
        this.closeCode = closeCode;
        this.closeMessage = closeMessage;
    }

    @Override
    public void write(final PacketOutputStream stream) throws IOException {
        stream.write(closeCode);
        stream.writeUTF(closeMessage);
    }

    @Override
    public void read(final PacketInputStream stream) throws IOException {
        user = stream.readUUID();
        key = stream.readUUID();
        version = stream.readUTF();
    }

    @Override
    public void serverProcess(WebSocket socket, TNTUser user) {
        AuthManager.authHypixel(this.user, key, new AuthManager.AuthResponse() {
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
                switch (errorCode) {
                    case AuthManager.ERROR_MANAGER_BUSY ->
                            Main.serverSend(socket, new AuthClientOldPacket(AuthManager.ERROR_MANAGER_BUSY,
                                    "This server is overloading"));
                    case AuthManager.ERROR_SERVER_THROTTLING ->
                            Main.serverSend(socket, new AuthClientOldPacket(AuthManager.ERROR_SERVER_THROTTLING,
                                    "Key throttling"));
                    case AuthManager.ERROR_INTERNAL_SERVER ->
                            Main.serverSend(socket, new AuthClientOldPacket(AuthManager.ERROR_INTERNAL_SERVER,
                                    "Internal server error"));
                    case AuthManager.ERROR_LOGIN_FAIL ->
                            Main.serverSend(socket, new AuthClientOldPacket(AuthManager.ERROR_LOGIN_FAIL,
                                    "Fail authentication, incorrect user"));
                }
                socket.close();
            }
        });
    }
}
