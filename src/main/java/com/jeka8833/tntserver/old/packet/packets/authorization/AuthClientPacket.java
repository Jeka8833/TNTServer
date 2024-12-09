package com.jeka8833.tntserver.old.packet.packets.authorization;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import com.jeka8833.tntserver.old.AuthManager;
import com.jeka8833.tntserver.old.user.UserBase;
import com.jeka8833.tntserver.old.user.player.GameServer;
import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.database.PlayersDatabase;
import com.jeka8833.tntserver.old.database.RemoteDB;
import com.jeka8833.tntserver.old.database.storage.Player;
import com.jeka8833.tntserver.old.database.storage.TNTPlayerStorage;
import com.jeka8833.tntserver.old.database.storage.User;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.old.packet.packets.BlockModulesPacket;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@NoArgsConstructor
public class AuthClientPacket implements Packet {
    private String playerUsername;
    private String serverKey;
    private String version;
    private byte[] customParameters;

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) {
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        playerUsername = stream.readUTF();
        serverKey = stream.readUTF();
        version = stream.readUTF();

        customParameters = stream.readAllBytes();
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        AuthManager.authMojang(playerUsername, serverKey, new AuthManager.AuthResponse() {
            @Override
            public void good(@NotNull UUID user, @Nullable Set<String> privileges) {
                RemoteDB.readUser(user, ignore -> {
                    User newUser = PlayersDatabase.getOrCreate(user);
                    if (newUser instanceof Player player) {
                        for (WebSocket oldSocket : TNTServer.server.getConnections()) {
                            UUID oldUser = oldSocket.getAttachment();
                            if (user.equals(oldUser)) {
                                oldSocket.close();
                            }
                        }

                        socket.setAttachment(user);

                        Parameters parameters = null;
                        try {
                            String fixed = new String(customParameters, StandardCharsets.US_ASCII)
                                    .replace("{\"serverBrand\":Hypixel}", "{\"serverBrand\":\"Hypixel\"}")
                                    .replace("{\"serverBrand\":Odyssey}", "{\"serverBrand\":\"Odyssey\"}");

                            parameters = JSON.parseObject(fixed, Parameters.class);
                        } catch (Exception e) {
                            log.warn("Failed to parse custom parameters", e);
                        }

                        player.serverType = parameters != null && parameters.serverBrand.isPresent() ?
                                GameServer.findByServerBrand(parameters.serverBrand.get()) : GameServer.HYPIXEL;

                        if (player.tntPlayerInfo == null) player.tntPlayerInfo = new TNTPlayerStorage();

                        player.tntPlayerInfo.version = version;
                        player.tntPlayerInfo.status = TNTPlayerStorage.STATUS_ONLINE;

                        RemoteDB.writeUser(newUser);

                        TNTServer.serverSend(socket, new BlockModulesPacket(
                                player.tntPlayerInfo.forceBlock, player.tntPlayerInfo.forceActive));

                        log.info("Player {} logged in.", playerUsername);
                    } else {
                        socket.close();
                    }
                });
            }

            @Override
            public void bad(int errorCode) {
                socket.close();
            }
        });
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Parameters {
        public @JSONField(name = "serverBrand") Optional<String> serverBrand = Optional.empty();
    }
}
