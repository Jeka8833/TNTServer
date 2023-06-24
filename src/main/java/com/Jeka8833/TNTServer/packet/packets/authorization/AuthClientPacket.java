package com.Jeka8833.TNTServer.packet.packets.authorization;

import com.Jeka8833.TNTServer.AuthManager;
import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.database.Player;
import com.Jeka8833.TNTServer.database.PlayersDatabase;
import com.Jeka8833.TNTServer.database.managers.TNTClientDBManager;
import com.Jeka8833.TNTServer.database.storage.TNTPlayerStorage;
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
    public void serverProcess(WebSocket socket, Player user) {
        AuthManager.authMojang(playerUsername, serverKey, new AuthManager.AuthResponse() {
            @Override
            public void good(@NotNull UUID user, @Nullable Set<String> privileges) {
                socket.setAttachment(user);

                TNTClientDBManager.readOrCashUser(user, ignore -> {
                    Player player = PlayersDatabase.getOrCreate(user);

                    player.tntPlayerInfo = new TNTPlayerStorage();
                    player.tntPlayerInfo.version = version;
                    player.tntPlayerInfo.status = TNTPlayerStorage.STATUS_ONLINE;
                    player.tntPlayerInfo.timeLogin = System.currentTimeMillis();

                    TNTClientDBManager.writeUser(user, null);

                    Main.serverSend(socket,
                            new BlockModulesPacket(player.tntPlayerInfo.forceBlock, player.tntPlayerInfo.forceActive));
                });
            }

            @Override
            public void bad(int errorCode) {
                socket.close();
            }
        });
    }
}
