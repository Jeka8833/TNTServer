package com.jeka8833.tntserver.requester.balancer.node;

import com.jeka8833.tntserver.requester.storage.HypixelCompactStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class RemoteNode implements BalancerNode {
    private static final Map<UUID, CompletableFuture<HypixelCompactStructure>> WAITING = new ConcurrentHashMap<>();

    private final AtomicInteger available = new AtomicInteger(1);

    private final long timeoutNanos;
    private final int priority;
    private final @NotNull Consumer<@NotNull UUID> sendRequest;
    private final int overloadLimit;
    private final UUID processorUUID;

    public RemoteNode(long timeoutNanos, int priority, @NotNull Consumer<@NotNull UUID> sendRequest, int overloadLimit,
                      UUID processorUUID) {
        this.timeoutNanos = timeoutNanos;
        this.priority = priority;
        this.sendRequest = sendRequest;
        this.overloadLimit = overloadLimit;
        this.processorUUID = processorUUID;
    }

    public static void put(@NotNull UUID requestedPlayer, @NotNull HypixelCompactStructure storage) {
        CompletableFuture<HypixelCompactStructure> future = WAITING.remove(requestedPlayer);
        if (future != null) {
            future.complete(storage);
        }

        clearTimeouts();
    }

    private static void clearTimeouts() {
        WAITING.values().removeIf(CompletableFuture::isDone);
    }

    public void setAvailable(int available) {
        this.available.set(available);
    }

    @Override
    public @NotNull HypixelCompactStructure get(@NotNull UUID requestedPlayer) throws Exception {
        CompletableFuture<HypixelCompactStructure> completableFuture = WAITING.computeIfAbsent(requestedPlayer,
                k -> new CompletableFuture<HypixelCompactStructure>().orTimeout(timeoutNanos, TimeUnit.NANOSECONDS));

        sendRequest.accept(requestedPlayer);

        return completableFuture.get();
    }

    @Override
    public long refreshTimeNanos(boolean isTNTClientUser, int wins) {
        return TimeUnit.HOURS.toNanos(1);
    }

    @Override
    public UUID getNodeUUID() {
        return processorUUID;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int getAvailable() {
        return Math.max(0, available.get());
    }

    @Override
    public boolean canReserve() {
        return available.getAndUpdate(value -> value + overloadLimit > 0 ? value - 1 : value) > -overloadLimit;
    }

    @Override
    public void releaseReserve() {
    }

    @Override
    public void cancelAll() {
        Iterator<CompletableFuture<HypixelCompactStructure>> futures = WAITING.values().iterator();
        while (futures.hasNext()) {
            futures.next().cancel(true);

            futures.remove();
        }
    }
}
