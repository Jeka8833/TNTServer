package com.jeka8833.tntserver.requester.node;

import com.jeka8833.tntserver.requester.storage.HypixelCompactStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Comparator;
import java.util.UUID;

public interface RequesterNode extends Comparable<RequesterNode> {
    int PRIORITY_MAIN_SERVER = 30;
    int PRIORITY_SUPPORT_SERVER = 20;
    int PRIORITY_PLAYER = 10;

    @NotNull
    HypixelCompactStorage get(@NotNull UUID requestedPlayer) throws Exception;

    int getPriority();

    @Range(from = 0, to = Integer.MAX_VALUE)
    int getAvailable();

    boolean tryTake();

    void release();

    void cancelAll();

    @Override
    default int compareTo(@NotNull RequesterNode o) {
        return Comparator.comparingInt(RequesterNode::getPriority)
                .thenComparingInt(RequesterNode::getAvailable)
                .compare(o, this);
    }
}
