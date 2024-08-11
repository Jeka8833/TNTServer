package com.jeka8833.tntserver.requester.ratelimiter.strategy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

public final class HalfRefill implements RefillStrategy {
    private final int maxAvailable;
    private @Nullable Future<?> task;

    public HalfRefill(int maxAvailable) {
        this.maxAvailable = maxAvailable;
    }

    @Override
    public int refill(int remaining, long durationNanos, @NotNull IntConsumer addAvailableCounts,
                      @NotNull ScheduledExecutorService executorService) {
        if (remaining < 1) return remaining;

        int half = remaining / 2;

        task = executorService.schedule(() -> addAvailableCounts.accept(remaining - half),
                durationNanos / 2, TimeUnit.NANOSECONDS);

        return half;
    }

    @Override
    public int atFirstRequest(int approximateWaitingThreadCount) {
        return Math.max(0, maxAvailable / 4 - approximateWaitingThreadCount);
    }

    @Override
    public void stopAll() {
        if (task != null) {
            task.cancel(true);
        }
    }
}
