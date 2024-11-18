package com.jeka8833.tntserver.requester.balancer;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.mojang.MojangAPI;
import com.jeka8833.tntserver.packet.packets.RequestHypixelPlayerPacket;
import com.jeka8833.tntserver.requester.balancer.node.BalancerNode;
import com.jeka8833.tntserver.requester.balancer.node.LocalNode;
import com.jeka8833.tntserver.requester.balancer.node.RemoteNode;
import com.jeka8833.tntserver.requester.ratelimiter.HypixelRateLimiter;
import com.jeka8833.tntserver.requester.ratelimiter.strategy.TNTServerStrategyRefill;
import com.jeka8833.tntserver.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class NodeRegisterManager {
    private static final HypixelRateLimiter RATE_LIMITER = new HypixelRateLimiter(
            TimeUnit.SECONDS.toNanos(10),
            TimeUnit.MILLISECONDS.toNanos(100),
            new TNTServerStrategyRefill(300));

    private static final int OVERLOAD_LOCAL_REQUESTS = 0;

    private static final Map<WebSocket, RemoteNode> NODES = new ConcurrentHashMap<>();

    public static void init() {
        Optional<UUID> hypixelApiKey = Main.INSTANCE.hypixelApiKey;

        if (hypixelApiKey.isEmpty()) {
            log.error("Hypixel API key is not set. Local node will not be added.");

            return;
        }

        RequestBalancer.addNode(
                new LocalNode(RATE_LIMITER, hypixelApiKey.get(), Util.HTTP_CLIENT, OVERLOAD_LOCAL_REQUESTS));
    }

    public static void connect(WebSocket node) {
        if (NODES.containsKey(node)) return;

        UUID user = node.getAttachment();
        boolean isPlayer = MojangAPI.isPlayer(user);

        var remoteNode = new RemoteNode(TimeUnit.SECONDS.toNanos(isPlayer ? 10 : 20),
                isPlayer ? BalancerNode.PRIORITY_PLAYER : BalancerNode.PRIORITY_SUPPORT_SERVER,
                (uuid) -> TNTServer.serverSend(node, new RequestHypixelPlayerPacket(Collections.singleton(uuid))),
                0, user);

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
