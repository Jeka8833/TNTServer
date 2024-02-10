package com.jeka8833.tntserver;

import com.jeka8833.tntserver.balancer.HypixelAPIRequest;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.database.analytics.AnalyticManager;
import com.jeka8833.tntserver.database.managers.DatabaseManager;
import com.jeka8833.tntserver.database.managers.TNTClientDBManager;
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
import com.jeka8833.tntserver.util.BiMap;
import com.jeka8833.tntserver.util.Util;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.UUID;

public class Main extends WebSocketServer {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static final BiMap<Byte, Class<? extends Packet>> packetsList = new BiMap<>();
    public static AnalyticManager analyticManager;

    public static Main server;

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
        packetsList.put((byte) 248, ChatHookPacket.class);
        packetsList.put((byte) 249, MutePacket.class);
        packetsList.put((byte) 250, LinkCodePacket.class);
        packetsList.put((byte) 251, RolePacket.class);
        packetsList.put((byte) 252, DonatePacket.class);
        packetsList.put((byte) 253, TokenGeneratorPacket.class);
        packetsList.put((byte) 254, ModulesStatusPacket.class);
        packetsList.put((byte) 255, AuthWebPacket.class);
    }

    public Main(final InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        User user = PlayersDatabase.getUser(conn.getAttachment());
        if (user != null) user.disconnect();

        final String type;
        if (user == null) {
            type = "Player(Bot) ";
        } else if (user instanceof Player) {
            type = "Player ";
        } else {
            type = "Bot ";
        }
        LOGGER.info(type + conn.getAttachment() + " is logged out(Error code: " + code +
                "; Message: " + reason + "). Current online: " + Main.server.getConnections().size());
        conn.close();
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

    public static void main(String[] args) {
        System.setOut(IoBuilder.forLogger(LogManager.getLogger("system.out"))
                .setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(LogManager.getLogger("system.err"))
                .setLevel(Level.ERROR).buildPrintStream());

        try {
            ChatFilter.loadDictionaries(Util.getParam(args, "-dictionary_path"));
        } catch (Exception e) {
            LOGGER.error("Fail load dictionary", e);
        }

        try {
            analyticManager = new AnalyticManager(Path.of(Util.getParam(args, "-analytic_path")),
                    Long.parseLong(Util.getParam(args, "-analytic_max_size")));

            analyticManager.start();
        } catch (Exception ignored) {
            LOGGER.error("Fail start analytic thread");
        }

        try {
            AuthManager.authURLTNTClient = Util.getParam(args, "-auth_url_tntclient");
            HypixelAPIRequest.init(Util.getParam(args, "-hypixel_key"));
            DatabaseManager.initConnect(Util.getParam(args, "-db_ip"),
                    Util.getParam(args, "-db_user"), Util.getParam(args, "-db_password"));

            server = new Main(new InetSocketAddress(Integer.parseInt(Util.getParam(args, "-server_port"))));
            server.setConnectionLostTimeout(15);
            server.setReuseAddr(true);
            server.start();
            TNTClientDBManager.init();
            BlockModulesPacket.readAndSetGlobalBlock();
        } finally {
            TNTClientDBManager.forceWrite();
        }
    }
}
