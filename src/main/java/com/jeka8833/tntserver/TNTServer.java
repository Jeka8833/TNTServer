package com.jeka8833.tntserver;

import com.jeka8833.tntserver.database.storage.Bot;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.database.analytics.AnalyticManager;
import com.jeka8833.tntserver.database.RemoteDB;
import com.jeka8833.tntserver.gamechat.ChatFilter;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.packets.*;
import com.jeka8833.tntserver.packet.packets.authorization.AuthClientPacket;
import com.jeka8833.tntserver.packet.packets.authorization.AuthWebPacket;
import com.jeka8833.tntserver.packet.packets.discordbot.ChatHookPacket;
import com.jeka8833.tntserver.packet.packets.discordbot.LinkCodePacket;
import com.jeka8833.tntserver.packet.packets.discordbot.MutePacket;
import com.jeka8833.tntserver.packet.packets.odyssey.DonatePacket;
import com.jeka8833.tntserver.packet.packets.web.ModulesStatusPacket;
import com.jeka8833.tntserver.packet.packets.web.RolePacket;
import com.jeka8833.tntserver.packet.packets.web.TokenGeneratorPacket;
import com.jeka8833.tntserver.requester.NodeRegisterManager;
import com.jeka8833.tntserver.util.BiMap;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public class TNTServer extends WebSocketServer {
    public static final BiMap<Byte, Class<? extends Packet>> packetsList = new BiMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(TNTServer.class);
    @Nullable
    public static AnalyticManager analyticManager;

    public static TNTServer server;

    static {
        packetsList.put((byte) 1, ActiveModulesPacket.class);
        packetsList.put((byte) 3, PingPacket.class);
        packetsList.put((byte) 4, RequestTNTClientPlayerPacket.class);
        packetsList.put((byte) 5, ReceiveTNTClientPlayerPacket.class);
        packetsList.put((byte) 6, ChatPacket.class);
        packetsList.put((byte) 7, BlockModulesPacket.class);
        packetsList.put((byte) 8, GameInfoPacket.class);
        packetsList.put((byte) 9, FightPacket.class);
        packetsList.put((byte) 10, AuthClientPacket.class);
        packetsList.put((byte) 11, PlayersPingPacket.class);
        packetsList.put((byte) 12, TokenPacket.class);
        packetsList.put((byte) 13, ReceiveHypixelPlayerPacket.class);
        packetsList.put((byte) 14, RequestHypixelPlayerPacket.class);
        packetsList.put((byte) 15, UpdateFreeRequestsPacket.class);
        packetsList.put((byte) 16, AnalyticPacket.class);
        packetsList.put((byte) 17, ReceiveHypixelPlayerV2Packet.class);
        packetsList.put((byte) 18, RequestHypixelPlayerV2Packet.class);
        packetsList.put((byte) 248, ChatHookPacket.class);
        packetsList.put((byte) 249, MutePacket.class);
        packetsList.put((byte) 250, LinkCodePacket.class);
        packetsList.put((byte) 251, RolePacket.class);
        packetsList.put((byte) 252, DonatePacket.class);
        packetsList.put((byte) 253, TokenGeneratorPacket.class);
        packetsList.put((byte) 254, ModulesStatusPacket.class);
        packetsList.put((byte) 255, AuthWebPacket.class);
    }

    public TNTServer(final InetSocketAddress address) {
        super(address);
    }

    public static void serverSend(@NotNull WebSocket socket, @NotNull Packet packet) {
        try (final PacketOutputStream stream = new PacketOutputStream()) {
            packet.write(stream);
            if (socket.isOpen()) {
                socket.send(stream.getByteBuffer(packet.getClass()));
            }
        } catch (Exception e) {
            LOGGER.error("Fail send packet:", e);
        }
    }

    public static void loadServer() {
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
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        NodeRegisterManager.disconnect(conn);

        UUID userUUID = conn.getAttachment();
        conn.close();

        User user = PlayersDatabase.getUser(userUUID);
        if (user != null) user.disconnect();

        if (userUUID == null) {
            LOGGER.info("Unknown user is logged out (Error code: {}; Message: {})", code, reason);
        } else if (user instanceof Bot) {
            LOGGER.info("Bot {} is logged out (Error code: {}; Message: {})", userUUID, code, reason);
        } else {
            LOGGER.info("Player {} is logged out (Error code: {}; Message: {})", userUUID, code, reason);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        UUID userUUID = conn.getAttachment();
        User user = PlayersDatabase.getUser(userUUID);

        try (PacketInputStream stream = new PacketInputStream(message)) {
            if ((user == null || user.isInactive()) &&
                    !(stream.packet instanceof AuthClientPacket || stream.packet instanceof AuthWebPacket)) {
                conn.close(); // The player doesn't exist in the cache, disconnecting...
                return;
            }

            stream.packet.read(stream);
            stream.packet.serverProcess(conn, user);
        } catch (Exception e) {
            LOGGER.error("Fail parse packet", e);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.error("Server have a error:", ex);
    }

    @Override
    public void onStart() {
    }
}
