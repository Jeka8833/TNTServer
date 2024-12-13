package com.jeka8833.tntserver.services.tntclient;

import com.jeka8833.tntserver.services.tntclient.packet.ClientBoundPacket;
import com.jeka8833.tntserver.services.tntclient.packet.PacketRegister;
import com.jeka8833.tntserver.services.tntclient.packet.ServerBoundPacket;
import com.jeka8833.tntserver.services.tntclient.user.UserBase;
import com.jeka8833.toprotocol.core.register.ServerBoundRegistry;
import com.jeka8833.toprotocol.core.serializer.ArrayInputSerializer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.protocols.Protocol;
import org.java_websocket.server.WebSocketServer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
@Service
public class TNTServerWebSocket extends WebSocketServer {
    @Getter
    private final ServerBoundRegistry<Byte, ClientBoundPacket, ServerBoundPacket, Integer> registry =
            PacketRegister.getPacketRegistry().buildForServer();

    @Getter
    private final int maxFrameSize;

    private final Map<Class<? extends ServerBoundPacket>, Collection<Consumer<ServerBoundPacket>>> listenersMap =
            new ConcurrentHashMap<>();

    public TNTServerWebSocket(@Value("${tntclient.websocket.port:8833}") int port,
                              @Value("${tntclient.websocket.maxframesize:2KB}") DataSize dataSize) {
        super(new InetSocketAddress(port), Collections.singletonList(new Draft_6455(
                Collections.emptyList(),
                Collections.singletonList(new Protocol("")),
                Math.toIntExact(dataSize.toBytes()))));

        this.maxFrameSize = Math.toIntExact(dataSize.toBytes());
    }

    public <T extends ServerBoundPacket> void registerListener(@NotNull Class<T> packetClass,
                                                               @NotNull Consumer<T> listener) {
        Collection<Consumer<ServerBoundPacket>> list = listenersMap.computeIfAbsent(packetClass,
                _ -> new CopyOnWriteArrayList<>());

        //noinspection unchecked
        list.add((Consumer<ServerBoundPacket>) listener);
    }

    public <T extends ServerBoundPacket> void unregisterListener(@NotNull Class<T> packetClass,
                                                                 @NotNull Consumer<T> listener) {
        Collection<Consumer<ServerBoundPacket>> list = listenersMap.get(packetClass);
        if (list != null) {
            list.remove(listener);
        }
    }

    @Override
    public void onStart() {

    }

    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer
            (WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
        return super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (conn.getAttachment() instanceof UserBase userBase) {
            userBase.onDisconnect();
        }

        log.info("WebSocket closed, attachment: {}, code: {}, reason: {}, remote: {}",
                conn.getAttachment(), code, reason, remote);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        log.error("WebSocket error, attachment: {}", webSocket.getAttachment(), e);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        webSocket.close();  // I think this user something did wrong
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        super.onMessage(conn, message);

        if (conn.getAttachment() instanceof UserBase userBase) {
            ArrayInputSerializer serializer = new ArrayInputSerializer(message.array());

            try {
                byte packetId = serializer.readByte();

                ServerBoundPacket packet = registry.createServerBoundPacket(packetId, serializer, userBase.getProtocolVersion());
                if (packet == null) {
                    log.warn("Unknown packet: {}, user: {}", packetId, userBase);
                    conn.close();
                    return;
                }

                Collection<Consumer<ServerBoundPacket>> listeners = listenersMap.get(packet.getClass());
                if (listeners != null) {
                    for (Consumer<ServerBoundPacket> listener : listeners) {
                        try {
                            listener.accept(packet);
                        } catch (Exception e) {
                            log.warn("Error while running service logic for packet: {}, user: {}", packet, userBase, e);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error while parsing packet, user: {}", userBase, e);
            }
        } else {
            log.error("Unknown attachment: " + conn.getAttachment());
            conn.close();
        }
    }
}
