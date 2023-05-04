package com.Jeka8833.TNTServer;

import com.Jeka8833.TNTServer.dataBase.DatabaseManager;
import com.Jeka8833.TNTServer.dataBase.TNTClientDBManager;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import com.Jeka8833.TNTServer.packet.packets.*;
import com.Jeka8833.TNTServer.packet.packets.authorization.AuthClientOldPacket;
import com.Jeka8833.TNTServer.packet.packets.authorization.AuthClientPacket;
import com.Jeka8833.TNTServer.packet.packets.authorization.AuthWebPacket;
import com.Jeka8833.TNTServer.packet.packets.web.ModulesStatusPacket;
import com.Jeka8833.TNTServer.util.BiMap;
import com.Jeka8833.TNTServer.util.Util;
import com.google.gson.Gson;
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
    public static final Gson GSON = new Gson();

    public static final BiMap<Byte, Class<? extends Packet>> packetsList = new BiMap<>();

    public static Main server;

    static {
        packetsList.put((byte) 1, ActiveModulesPacket.class);
        packetsList.put((byte) 2, AuthClientOldPacket.class);
        packetsList.put((byte) 3, PingPacket.class);
        packetsList.put((byte) 4, RequestPlayerStatusPacket.class);
        packetsList.put((byte) 5, SendPlayerStatusPacket.class);
        packetsList.put((byte) 6, ChatPacket.class);
        packetsList.put((byte) 7, BlockModulesPacket.class);
        packetsList.put((byte) 8, GameInfoPacket.class);
        packetsList.put((byte) 9, FightPacket.class);
        packetsList.put((byte) 10, AuthClientPacket.class);
        packetsList.put((byte) 11, PlayersPingPacket.class);
        packetsList.put((byte) 12, TokenPacket.class);
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

            if (stream.packet instanceof AuthClientPacket || stream.packet instanceof AuthWebPacket ||
                    stream.packet instanceof AuthClientOldPacket) {
                stream.packet.read(stream);
                stream.packet.serverProcess(conn, null);
            } else if (playerUUID == null) {
                conn.close();
            } else {
                TNTUser user = TNTClientDBManager.getUser(playerUUID);
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
            logger.info("TNTServer start");
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
