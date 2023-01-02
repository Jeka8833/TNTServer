package com.Jeka8833.TNTServer;

import com.Jeka8833.TNTServer.dataBase.DatabaseManager;
import com.Jeka8833.TNTServer.dataBase.TNTClientDBManager;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import com.Jeka8833.TNTServer.packet.packets.*;
import com.Jeka8833.TNTServer.util.BiMap;
import com.Jeka8833.TNTServer.util.Util;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
        packetsList.put((byte) 2, AuthPacket.class);
        packetsList.put((byte) 3, PingPacket.class);
        packetsList.put((byte) 4, RequestPlayerStatusPacket.class);
        packetsList.put((byte) 5, SendPlayerStatusPacket.class);
        packetsList.put((byte) 6, ChatPacket.class);
        packetsList.put((byte) 7, BlockModulesPacket.class);
        packetsList.put((byte) 8, GameInfoPacket.class);
        packetsList.put((byte) 9, FightPacket.class);

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
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        try (final PacketInputStream stream = new PacketInputStream(message)) {
            final UUID key = conn.getAttachment();
            final TNTUser user = TNTUser.keyUserList.get(key);
            if (user == null && !(stream.packet instanceof AuthPacket)) {
                conn.close();
            } else {
                stream.packet.read(stream);
                stream.packet.serverProcess(conn, user);
                if (user != null) user.heartBeat();
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
            server.broadcast(stream.getByteBuffer(packet.getClass()));
        } catch (Exception e) {
            logger.error("Fail send packet:", e);
        }
    }

    public static void main(String[] args) {
        try {
            logger.info("TNTServer start");
            DatabaseManager.initConnect(Util.getParam(args, "-db_ip"), Util.getParam(args, "-db_user"),
                    Util.getParam(args, "-db_password"));

            server = new Main(new InetSocketAddress(Integer.parseInt(Util.getParam(args, "-server_port"))));
            server.start();
            TNTClientDBManager.init();
        } finally {
            TNTClientDBManager.writeUsers(TNTUser.keyUserList.values().stream()
                    .filter(tntUser -> tntUser.key != null && tntUser.version != null)
                    .map(tntUser -> tntUser.user).toList(), null);
            TNTClientDBManager.forceWrite();
        }
    }
}
