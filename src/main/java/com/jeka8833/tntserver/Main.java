package com.jeka8833.tntserver;

import com.jeka8833.tntserver.balancer.HypixelAPIRequest;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.managers.DatabaseManager;
import com.jeka8833.tntserver.database.managers.TNTClientDBManager;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.packets.*;
import com.jeka8833.tntserver.packet.packets.authorization.AuthClientPacket;
import com.jeka8833.tntserver.packet.packets.authorization.AuthWebPacket;
import com.jeka8833.tntserver.packet.packets.web.ModulesStatusPacket;
import com.jeka8833.tntserver.util.BiMap;
import com.jeka8833.tntserver.util.Util;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Main extends WebSocketServer {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static final BiMap<Byte, Class<? extends Packet>> packetsList = new BiMap<>();

    public static Main server;

    static {
        packetsList.put((byte) 1, ActiveModulesPacket.class);
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
        packetsList.put((byte) 254, ModulesStatusPacket.class);
        packetsList.put((byte) 255, AuthWebPacket.class);
    }

    public Main(final InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("Current online: " + Main.server.getConnections().size());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("Current online: " + Main.server.getConnections().size());

        BotsManager.clearDisconnectedBots();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        try (PacketInputStream stream = new PacketInputStream(message)) {
            UUID playerUUID = conn.getAttachment();

            if (stream.packet instanceof AuthClientPacket || stream.packet instanceof AuthWebPacket) {
                stream.packet.read(stream);
                stream.packet.serverProcess(conn, null);
            } else if (playerUUID == null) {
                conn.close();
            } else {
                Player user = PlayersDatabase.getUser(playerUUID);
                if (user == null && playerUUID.variant() == 2) {
                    conn.close(); // The player doesn't exist in the cache, disconnecting...
                } else {
                    try {
                        stream.packet.read(stream);
                        stream.packet.serverProcess(conn, user);
                    } catch (Exception e) {
                        logger.error("Fail parse packet", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Fail parse packet", e);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("Server have a error:", ex);
    }

    @Override
    public void onStart() {
        // Ping every 15 second and wait answer 23 second
        setConnectionLostTimeout(15);
    }

    public static void serverSend(final WebSocket socket, final Packet packet) {
        try (final PacketOutputStream stream = new PacketOutputStream()) {
            packet.write(stream);
            if (socket.isOpen())
                socket.send(stream.getByteBuffer(packet.getClass()));
        } catch (Exception e) {
            logger.error("Fail send packet:", e);
        }
    }

    public static void serverBroadcast(final Packet packet) {
        try (final PacketOutputStream stream = new PacketOutputStream()) {
            packet.write(stream);
            ByteBuffer send = stream.getByteBuffer(packet.getClass());

            for (WebSocket client : server.getConnections()) {
                try {
                    UUID userID = client.getAttachment();
                    if (userID == null || userID.version() != 4) continue;

                    if (client.isOpen()) client.send(send);
                } catch (Exception e) {
                    logger.error("Fail send packet:", e);
                }
            }
        } catch (Exception e) {
            logger.error("Fail generate packet:", e);
        }
    }

    public static void main(String[] args) {
        System.setOut(
                IoBuilder.forLogger(LogManager.getLogger("system.out")).setLevel(Level.INFO).buildPrintStream());
        System.setErr(
                IoBuilder.forLogger(LogManager.getLogger("system.err")).setLevel(Level.ERROR).buildPrintStream());

        try {
            HypixelAPIRequest.init(Util.getParam(args, "-hypixel_key"));
            DatabaseManager.initConnect(Util.getParam(args, "-db_ip"), Util.getParam(args, "-db_user"),
                    Util.getParam(args, "-db_password"));

            server = new Main(new InetSocketAddress(Integer.parseInt(Util.getParam(args, "-server_port"))));
            server.start();
            TNTClientDBManager.init();
            BlockModulesPacket.readAndSetGlobalBlock();
        } finally {
            TNTClientDBManager.forceWrite();
        }
    }
}
