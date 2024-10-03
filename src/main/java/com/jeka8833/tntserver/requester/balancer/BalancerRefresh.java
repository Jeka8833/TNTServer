package com.jeka8833.tntserver.requester.balancer;

import com.jeka8833.tntserver.requester.balancer.node.BalancerNode;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface BalancerRefresh {
    int getMaxWins();

    @NotNull
    Map<UUID, Long> getRequestTimeMap();

    default OptionalLong getElapsedFromLastRequest() {
        return getRequestTimeMap().values().stream()
                .mapToLong(time -> System.nanoTime() - time)
                .min();
    }

    default long getMinDelayBetweenRequests(boolean isTNTClientUser, @NotNull Collection<BalancerNode> nodes) {
        Map.Entry<UUID, Long> minRefreshTime = nodes.stream()
                .map(node -> new AbstractMap.SimpleEntry<>(
                        node.getNodeUUID(),
                        node.refreshTimeNanos(isTNTClientUser, getMaxWins())))
                .sorted(Map.Entry.comparingByKey())
                .min(Map.Entry.comparingByValue())
                .orElseThrow(() -> new RuntimeException("Nodes are empty, how?"));

        Long minRefreshNodeLastTime = getRequestTimeMap().get(minRefreshTime.getKey());
        if (minRefreshNodeLastTime == null) return minRefreshTime.getValue() / nodes.size();

        long endZoneTime = minRefreshNodeLastTime + minRefreshTime.getValue();
        if (System.nanoTime() - endZoneTime >= 0) return 0;

        long finalEndZoneTime = endZoneTime - minRefreshTime.getValue() / nodes.size();

        long count = nodes.stream()
                .filter(node -> canRequestAt(finalEndZoneTime, isTNTClientUser, node))
                .count();

        return minRefreshTime.getValue() / (count + 1);
    }

    default boolean canRequestAt(long time, boolean isTNTClientUser, @NotNull BalancerNode node) {
        Long lastRequestTime = getRequestTimeMap().get(node.getNodeUUID());
        if (lastRequestTime == null) return true;

        long nextAllowTime = lastRequestTime + node.refreshTimeNanos(isTNTClientUser, getMaxWins());

        return time - nextAllowTime >= 0;
    }
}
