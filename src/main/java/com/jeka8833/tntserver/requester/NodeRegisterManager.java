package com.jeka8833.tntserver.requester;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.mojang.MojangAPI;
import com.jeka8833.tntserver.packet.packets.RequestHypixelPlayerPacket;
import com.jeka8833.tntserver.requester.node.LocalNode;
import com.jeka8833.tntserver.requester.node.RemoteNode;
import com.jeka8833.tntserver.requester.node.RequesterNode;
import com.jeka8833.tntserver.requester.ratelimiter.HypixelRateLimiter;
import com.jeka8833.tntserver.requester.ratelimiter.strategy.TNTServerStrategyRefill;
import com.jeka8833.tntserver.util.Util;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class NodeRegisterManager {
    private static final HypixelRateLimiter RATE_LIMITER = new HypixelRateLimiter(
            TimeUnit.SECONDS.toNanos(10),
            TimeUnit.MILLISECONDS.toNanos(100),
            new TNTServerStrategyRefill(300));

    private static final int OVERLOAD_LOCAL_REQUESTS = 0;

    private static final Map<WebSocket, RemoteNode> NODES = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeRegisterManager.class);

    public static void init() {
        if (Main.INSTANCE.hypixelApiKey.isEmpty()) {
            LOGGER.error("Hypixel API key is not set. Local node will not be added.");

            return;
        }

        RequestBalancer.addNode(new LocalNode(RATE_LIMITER, Main.INSTANCE.hypixelApiKey.get(),
                Util.HTTP_CLIENT, OVERLOAD_LOCAL_REQUESTS));
    }

    public static void connect(WebSocket node) {
        if (NODES.containsKey(node)) return;

        UUID user = node.getAttachment();
        boolean isPlayer = MojangAPI.isPlayer(user);

        var remoteNode = new RemoteNode(TimeUnit.SECONDS.toNanos(isPlayer ? 10 : 20),
                isPlayer ? RequesterNode.PRIORITY_PLAYER : RequesterNode.PRIORITY_SUPPORT_SERVER,
                (uuid) -> TNTServer.serverSend(node, new RequestHypixelPlayerPacket(Collections.singletonList(uuid))),
                0);

        NODES.put(node, remoteNode);

        RequestBalancer.addNode(remoteNode);
    }

    public static void disconnect(WebSocket node) {
        RemoteNode nodeData = NODES.remove(node);
        if (nodeData == null) return;

        RequestBalancer.removeNode(nodeData);
        nodeData.cancelAll();
    }

    public static void setAvailableCount(WebSocket node, int count) {
        RemoteNode nodeData = NODES.get(node);
        if (nodeData == null) return;

        nodeData.setAvailable(count);
    }
}
