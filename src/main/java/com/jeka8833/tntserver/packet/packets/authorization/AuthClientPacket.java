package com.jeka8833.tntserver.packet.packets.authorization;

import com.jeka8833.tntserver.AuthManager;
import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.managers.TNTClientDBManager;
import com.jeka8833.tntserver.database.storage.TNTPlayerStorage;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.packets.BlockModulesPacket;
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

                TNTClientDBManager.readUser(user, ignore -> {
                    Player player = PlayersDatabase.getOrCreate(user);

                    if (player.tntPlayerInfo == null) player.tntPlayerInfo = new TNTPlayerStorage();

                    player.tntPlayerInfo.version = version;
                    player.tntPlayerInfo.status = TNTPlayerStorage.STATUS_ONLINE;

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
