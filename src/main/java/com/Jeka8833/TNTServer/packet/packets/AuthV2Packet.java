package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.dataBase.TNTClientDBManager;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import com.Jeka8833.TNTServer.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class AuthV2Packet implements Packet {

    private static final Logger logger = LogManager.getLogger(AuthV2Packet.class);

    private String playerUsername;
    private String serverKey;
    private String version;

    public AuthV2Packet() {
    }

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
        if (AuthPacket.executor.getQueue().size() == AuthPacket.executor.getMaximumPoolSize()) {
            socket.close();
        } else {
            AuthPacket.executor.execute(() -> {
                UUID userUUID = readUUID();
                if (userUUID == null) {
                    socket.close();
                    return;
                }

                socket.setAttachment(userUUID);

                TNTClientDBManager.readOrCashUser(userUUID, tntUser -> {
                    final TNTUser account = tntUser == null ? new TNTUser(userUUID, version) : tntUser;
                    account.version = version;
                    account.timeLogin = System.currentTimeMillis();
                    account.heartBeat();
                    TNTUser.addUser(account);

                    TNTClientDBManager.writeUser(userUUID, null);

                    Main.serverSend(socket, new BlockModulesPacket(account.forceBlock, account.forceActive));
                });
            });
        }
    }

    @Nullable
    public UUID readUUID() {
        if (!playerUsername.matches("^[a-zA-Z0-9_]{2,16}$")) return null;
        if (serverKey.length() > 41 || serverKey.matches("[^0-9a-f-]")) return null;

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/hasJoined?serverId=" +
                        serverKey + "&username=" + playerUsername)).GET().build();
        try {
            final HttpResponse<String> response = Util.client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ResponseID responseID = Main.GSON.fromJson(response.body(), ResponseID.class);

                return responseID == null ? null : responseID.getUuid();
            }
        } catch (Exception ex) {
            logger.warn("Mojang API is down", ex);
        }
        return null;
    }

    private static class ResponseID {
        private @Nullable String id;

        @Nullable
        public UUID getUuid() {
            if (id == null) return null;

            return fromString(id);
        }

        @NotNull
        public static UUID fromString(@NotNull String input) {
            return UUID.fromString(
                    input.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
        }
    }
}
