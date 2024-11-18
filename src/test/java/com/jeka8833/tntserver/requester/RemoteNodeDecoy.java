package com.jeka8833.tntserver.requester;

import com.jeka8833.tntserver.requester.balancer.node.BalancerNode;
import com.jeka8833.tntserver.requester.storage.HypixelCompactStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.UUID;

public class RemoteNodeDecoy implements BalancerNode {
    @Override
    public @NotNull HypixelCompactStructure get(@NotNull UUID requestedPlayer) throws Exception {
        return null;
    }

    @Override
    public long refreshTimeNanos(boolean isTNTClientUser, int wins) {
        return 0;
    }

    @Override
    public UUID getNodeUUID() {
        return null;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int getAvailable() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canReserve() {
        return false;
    }

    @Override
    public void releaseReserve() {

    }

    @Override
    public void cancelAll() {

    }
}
