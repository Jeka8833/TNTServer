package com.jeka8833.tntserver.packet.packets.authorization;

import com.jeka8833.tntserver.AuthManager;
import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.managers.TNTClientDBManager;
import com.jeka8833.tntserver.database.storage.TNTPlayerStorage;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.packets.BlockModulesPacket;
import com.jeka8833.tntserver.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class AuthClientPacket implements Packet {
    private static final Logger logger = LogManager.getLogger(AuthClientPacket.class);

    private String playerUsername;
    private String serverKey;
    private String version;
    private byte[] customParameters;

    @Override
    public void write(PacketOutputStream stream) {
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        playerUsername = stream.readUTF();
        serverKey = stream.readUTF();
        version = stream.readUTF();

        customParameters = stream.readAllBytes();
    }

    @Override
    public void serverProcess(WebSocket socket, Player user) {
        Parameters parameters = Util.GSON.fromJson(
                new String(customParameters, StandardCharsets.US_ASCII), Parameters.class);

        AuthManager.authMojang(playerUsername, serverKey, new AuthManager.AuthResponse() {
            @Override
            public void good(@NotNull UUID user, @Nullable Set<String> privileges) {
                socket.setAttachment(user);

                TNTClientDBManager.readUser(user, ignore -> {
                    Player player = PlayersDatabase.getOrCreate(user);
                    player.serverType = parameters == null ?
                            ServerType.HYPIXEL : ServerType.getServer(parameters.serverBrand);

                    if (player.tntPlayerInfo == null) player.tntPlayerInfo = new TNTPlayerStorage();

                    player.tntPlayerInfo.version = version;
                    player.tntPlayerInfo.status = TNTPlayerStorage.STATUS_ONLINE;

                    TNTClientDBManager.writeUser(user, null);

                    Main.serverSend(socket,
                            new BlockModulesPacket(player.tntPlayerInfo.forceBlock, player.tntPlayerInfo.forceActive));

                    logger.info("User " + playerUsername + " logged in. Current online: " +
                            Main.server.getConnections().size());
                });
            }

            @Override
            public void bad(int errorCode) {
                socket.close();
            }
        });
    }

    private static class Parameters {
        @SuppressWarnings("FieldMayBeFinal")
        private String serverBrand = "";
    }
}
