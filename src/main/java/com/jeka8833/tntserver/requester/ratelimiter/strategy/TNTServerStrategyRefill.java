package com.jeka8833.tntserver.requester.ratelimiter.strategy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.IntConsumer;

public final class TNTServerStrategyRefill implements RefillStrategy {
    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private final int maxAvailable;

    public TNTServerStrategyRefill(@Range(from = 0, to = Integer.MAX_VALUE) int maxAvailable) {
        this.maxAvailable = maxAvailable;
    }

    @Override
    public int refill(@Range(from = 0, to = Integer.MAX_VALUE) int remaining, long durationNanos,
                      @NotNull IntConsumer addAvailableCounts, @NotNull ScheduledExecutorService executorService) {
        return remaining;
    }

    @Override
    public int atFirstRequest(int approximateWaitingThreadCount) {
        return Math.max(0, maxAvailable - AVAILABLE_PROCESSORS - approximateWaitingThreadCount);
    }

    @Override
    public void stopAll() {
    }
}
