package com.jeka8833.tntserver.requester.balancer.node;

import com.jeka8833.tntserver.requester.storage.HypixelCompactStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Comparator;
import java.util.UUID;

public interface BalancerNode extends Comparable<BalancerNode> {
    int PRIORITY_MAIN_SERVER = 30;
    int PRIORITY_SUPPORT_SERVER = 20;
    int PRIORITY_PLAYER = 10;

    @NotNull
    HypixelCompactStructure get(@NotNull UUID requestedPlayer) throws Exception;

    long refreshTimeNanos(boolean isTNTClientUser, int wins);

    UUID getNodeUUID();

    int getPriority();

    @Range(from = 0, to = Integer.MAX_VALUE)
    int getAvailable();

    boolean canReserve();

    void releaseReserve();

    void cancelAll();

    @Override
    default int compareTo(@NotNull BalancerNode o) {
        return Comparator.comparingInt(BalancerNode::getPriority)
                .thenComparingInt(BalancerNode::getAvailable)
                .compare(o, this);
    }
}
