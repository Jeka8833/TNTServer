package com.jeka8833.tntserver.requester;

import com.jeka8833.tntserver.requester.node.RequesterNode;
import com.jeka8833.tntserver.requester.storage.HypixelCompactStorage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RequestBalancer {
    private static final List<RequesterNode> NODES = new CopyOnWriteArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestBalancer.class);

    private static final int MAX_REQUESTS = 3;

    @NotNull
    public static HypixelCompactStorage get(@NotNull UUID requestedPlayer) throws Exception {
        if (Thread.interrupted()) throw new InterruptedException();

        Collection<RequesterNode> usedNodes = new ArrayList<>(1);    // ArrayList because contains method is faster than HashSet, when the collection is small

        for (int i = 0; i < MAX_REQUESTS; i++) {
            if (Thread.interrupted()) throw new InterruptedException();

            RequesterNode node = null;
            try {
                node = getFreeNode(usedNodes);
                if (node == null) break;

                usedNodes.add(node);

                return node.get(requestedPlayer);
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.warn("Failed to load data from node", e);
            } finally {
                if (node != null) node.release();
            }
        }

        throw new InterruptedException("All nodes are busy");
    }

    public static void addNode(@NotNull RequesterNode node) {
        NODES.add(node);
        NODES.sort(null);
    }

    public static void removeNode(@NotNull RequesterNode node) {
        NODES.remove(node);

        node.cancelAll();
    }

    public static RequesterNode[] getNodes() {
        return NODES.toArray(new RequesterNode[0]);
    }

    private static RequesterNode getFreeNode(@NotNull Collection<RequesterNode> exclude) {
        // Expensive operations, but IDK how to do it better
        RequesterNode[] nodes = NODES.toArray(new RequesterNode[0]);
        Arrays.sort(nodes);

        for (RequesterNode node : nodes) {
            // Due to thread concurrency, getAvailable can not return 0, but node can be busy, but it's fine
            if (exclude.contains(node) || !node.tryTake()) continue;

            return node;
        }
        return null;
    }
}
