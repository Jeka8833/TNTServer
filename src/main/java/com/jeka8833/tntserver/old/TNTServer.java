package com.jeka8833.tntserver.old;

import com.jeka8833.tntserver.old.database.PlayersDatabase;
import com.jeka8833.tntserver.old.database.RemoteDB;
import com.jeka8833.tntserver.old.database.analytics.AnalyticManager;
import com.jeka8833.tntserver.old.database.storage.Bot;
import com.jeka8833.tntserver.old.database.storage.User;
import com.jeka8833.tntserver.old.gamechat.ChatFilter;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.old.packet.packets.*;
import com.jeka8833.tntserver.old.packet.packets.authorization.AuthBotPacket;
import com.jeka8833.tntserver.old.packet.packets.authorization.AuthClientPacket;
import com.jeka8833.tntserver.old.packet.packets.webendpoints.ChatHookPacket;
import com.jeka8833.tntserver.old.packet.packets.webendpoints.DiscordTokenEndpointSidePacket;
import com.jeka8833.tntserver.old.packet.packets.webendpoints.WebTokenEndpointSidePacket;
import com.jeka8833.tntserver.old.requester.HypixelCache;
import com.jeka8833.tntserver.old.requester.balancer.NodeRegisterManager;
import com.jeka8833.tntserver.old.util.BiMap;
import com.jeka8833.tntserver.services.tntclient.user.Anonymous;
import com.jeka8833.tntserver.services.tntclient.user.UserBase;
import com.jeka8833.tntserver.services.tntclient.user.UserDatabase;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.protocols.Protocol;
import org.java_websocket.server.WebSocketServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TNTServer extends WebSocketServer {
    public static final BiMap<Byte, Class<? extends Packet>> PACKETS_LIST = new BiMap<>();

    @Nullable
    public static AnalyticManager analyticManager;
    public static TNTServer server;

    static {
        PACKETS_LIST.put((byte) 1, ActiveModulesPacket.class);
        PACKETS_LIST.put((byte) 4, RequestTNTClientPlayerPacket.class);
        PACKETS_LIST.put((byte) 5, ReceiveTNTClientPlayerPacket.class);
        PACKETS_LIST.put((byte) 6, ChatPacket.class);
        PACKETS_LIST.put((byte) 7, BlockModulesPacket.class);
        PACKETS_LIST.put((byte) 8, GameInfoPacket.class);
        PACKETS_LIST.put((byte) 9, FightPacket.class);
        PACKETS_LIST.put((byte) 10, AuthClientPacket.class);
        PACKETS_LIST.put((byte) 11, PlayersPingPacket.class);
        PACKETS_LIST.put((byte) 12, WebTokenUseSidePacket.class);
        PACKETS_LIST.put((byte) 13, ReceiveHypixelPlayerPacket.class);
        PACKETS_LIST.put((byte) 14, RequestHypixelPlayerPacket.class);
        PACKETS_LIST.put((byte) 15, UpdateFreeRequestsPacket.class);
        PACKETS_LIST.put((byte) 16, AnalyticPacket.class);
        PACKETS_LIST.put((byte) 17, ReceiveHypixelPlayerV2Packet.class);
        PACKETS_LIST.put((byte) 18, RequestHypixelPlayerV2Packet.class);
        PACKETS_LIST.put((byte) 248, ChatHookPacket.class);
        PACKETS_LIST.put((byte) 250, DiscordTokenUserSidePacket.class);
        PACKETS_LIST.put((byte) 253, WebTokenEndpointSidePacket.class);
        PACKETS_LIST.put((byte) 254, DiscordTokenEndpointSidePacket.class);
        PACKETS_LIST.put((byte) 255, AuthBotPacket.class);
    }

    @Getter
    private final UserDatabase userDatabase = new UserDatabase(this);

    public TNTServer(final InetSocketAddress address) {
        super(address, Collections.singletonList(new Draft_6455(
                Collections.emptyList(),
                Collections.singletonList(new Protocol("")),
                2 * 1024)));
    }

    public static void serverSend(@NotNull WebSocket socket, @NotNull Packet packet) {
        try (final PacketOutputStream stream = new PacketOutputStream()) {
            packet.write(stream);
            if (socket.isOpen()) {
                socket.send(stream.getByteBuffer(packet.getClass()));
            }
        } catch (Exception e) {
            log.error("Fail send packet:", e);
        }
    }

    public static void loadServer() {
        HypixelCache.loadFromFile(Main.INSTANCE.cacheFile);

        ChatFilter.loadDictionaries();
        analyticManager = AnalyticManager.createAndStart();

        RemoteDB.openConnection();

        server = new TNTServer(new InetSocketAddress(Main.INSTANCE.serverPort));
        server.setConnectionLostTimeout(15);
        server.setReuseAddr(true);
        server.start();

        BlockModulesPacket.readAndSetGlobalBlock();
        NodeRegisterManager.init();
    }

    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer
            (WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
        try {
            ServerHandshakeBuilder builder = super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);

            String protocolVersionHeader = request.getFieldValue("TntServer-Protocol-Version");
            int protocolVersion = protocolVersionHeader.isEmpty() ? 0 : Integer.parseInt(protocolVersionHeader);

            String authorizationHeader = request.getFieldValue("Authorization");
            if (authorizationHeader.isEmpty()) {
                conn.setAttachment(new Anonymous(conn, protocolVersion, 1, TimeUnit.MINUTES));

                return builder;    // Outdated handshake
            }

            if (!authorizationHeader.toLowerCase().startsWith("basic ")) {
                throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid authorization header");
            }

            String base64Credentials = authorizationHeader.substring("basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            String[] values = credentials.split(":", 2);

            String username = values[0];
            String password = values[1];
            //To your checks and throw an InvalidDataException to indicate that you reject this handshake.

            for (WebSocket user : server.getConnections()) {
                if (user.equals(conn)) continue;

                if (Objects.equals(user.getAttachment(), conn.getAttachment())) {
                    user.close();
                }
            }

            return builder;
        } catch (Exception e) {
            log.error("Error in handshake:", e);

            throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Internal server error");
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        NodeRegisterManager.disconnect(conn);

        UUID userUUID = conn.getAttachment();
        conn.close();

        PlayersDatabase.clearInactivePeople();
        User user = PlayersDatabase.getUser(userUUID);
        if (user != null) user.disconnect();

        if (userUUID == null) {
            log.info("Unknown user is logged out (Error code: {}; Message: {})", code, reason);
        } else if (user instanceof Bot) {
            log.info("Bot {} is logged out (Error code: {}; Message: {})", userUUID, code, reason);
        } else {
            log.info("Player {} is logged out (Error code: {}; Message: {})", userUUID, code, reason);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        UserBase userBase = conn.getAttachment();
        if (userBase == null) {
            conn.close();

            log.error("Attachment is null, bug.");
            return;
        }

        try (PacketInputStream stream = new PacketInputStream(message)) {
            if (userBase instanceof Anonymous &&
                    !(stream.packet instanceof AuthClientPacket || stream.packet instanceof AuthBotPacket)) {
                conn.close();
                return;
            }

            stream.packet.read(stream);
            stream.packet.serverProcess(userBase, this);
        } catch (Exception e) {
            log.error("Fail parse packet", e);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("Server have a error:", ex);
    }

    @Override
    public void onStart() {
    }
}
