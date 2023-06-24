package com.Jeka8833.TNTServer.util;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.time.Duration;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class SingleThreadHypixelRateLimiter {

    public static final int ONE_GROUP = Integer.MAX_VALUE;
    public static final int INFINITY_GROUPS = 1;

    private @Range(from = 1, to = Integer.MAX_VALUE) int refreshDuration;
    private @Range(from = INFINITY_GROUPS, to = ONE_GROUP) int groupSize;
    private @Range(from = 0, to = Long.MAX_VALUE) long sleepAfterFail;

    private @Nullable ServerResponse response;

    public SingleThreadHypixelRateLimiter(@NotNull Duration refreshDuration) {
        this(refreshDuration, INFINITY_GROUPS);
    }

    public SingleThreadHypixelRateLimiter(@NotNull Duration refreshDuration,
                                          @Range(from = INFINITY_GROUPS, to = ONE_GROUP) int groupSize) {
        this(refreshDuration, groupSize, Duration.ofSeconds(10));
    }

    public SingleThreadHypixelRateLimiter(@NotNull Duration refreshDuration,
                                          @Range(from = INFINITY_GROUPS, to = ONE_GROUP) int groupSize,
                                          @NotNull Duration sleepAfterFail) {
        setRefreshDuration(refreshDuration);
        setGroupSize(groupSize);
        setSleepAfterFail(sleepAfterFail);
    }

    @Blocking
    public void receiveAndLock(@NotNull ServerResponse response) throws InterruptedException {
        if (response.isFail()) {
            if (sleepAfterFail > 0) Thread.sleep(sleepAfterFail);
            return;
        }

        this.response = response;

        // 2000 - 2 seconds makes up for the mathematical rounding.
        if (response.getRemaining() >= (2000 * response.getLimit()) / refreshDuration) {
            int groupRemaining = response.getLimit() -
                    (response.getLimit() - response.getRemaining()) / groupSize * groupSize;   // Math round
            int sleepTime = response.getToReset() - (refreshDuration * groupRemaining) / response.getLimit();

            if (sleepTime > 1) Thread.sleep(sleepTime);
        } else if (response.getRemaining() == 0) {
            int sleepTime = response.getToReset();

            if (sleepTime > 1) Thread.sleep(sleepTime);
        }
    }

    public void setRefreshDuration(@NotNull Duration refreshDuration) {
        long time = refreshDuration.toMillis();
        if (time <= 0) throw new IllegalArgumentException("RefreshDuration is <= 0");

        this.refreshDuration = Math.toIntExact(time);
    }

    public void setSleepAfterFail(@NotNull Duration sleepAfterFail) {
        if (sleepAfterFail.isNegative()) throw new IllegalArgumentException("Sleep duration is negative");

        this.sleepAfterFail = sleepAfterFail.toMillis();
    }

    public void setGroupSize(int groupSize) {
        if (groupSize < INFINITY_GROUPS) throw new IllegalArgumentException("Group size must be > 0");

        this.groupSize = groupSize;
    }

    public boolean isFail() {
        return response == null || response.isFail();
    }

    public OptionalInt getLimit() {
        if (isFail()) return OptionalInt.empty();

        //noinspection DataFlowIssue
        return OptionalInt.of(response.getLimit());
    }

    public OptionalInt getUsed() {
        if (isFail()) return OptionalInt.empty();

        //noinspection DataFlowIssue
        return OptionalInt.of(response.getLimit() - response.getRemaining());
    }

    public OptionalInt getToReset() {
        if (isFail()) return OptionalInt.empty();

        //noinspection DataFlowIssue
        return OptionalInt.of(Math.max(0, response.getToReset()));
    }

    public static class ServerResponse {

        private @Range(from = 0, to = Integer.MAX_VALUE) int limit;
        private @Range(from = 0, to = Integer.MAX_VALUE) int remaining;
        private @Range(from = 0, to = Long.MAX_VALUE) long reset;

        public void set(@Range(from = 0, to = Integer.MAX_VALUE) int reset,
                        @Range(from = 1, to = Integer.MAX_VALUE) int limit,
                        @Range(from = 0, to = Integer.MAX_VALUE) int remaining) {
            this.reset = System.currentTimeMillis() + (reset + 1) * 1000L; // +1 compensates for mathematical rounding, ceil value
            this.limit = limit;
            this.remaining = remaining;
        }

        public void set(@Nullable String reset, @Nullable String limit, @Nullable String remaining) {
            if (reset == null || limit == null || remaining == null ||
                    reset.isEmpty() || limit.isEmpty() || remaining.isEmpty()) return;

            try {
                set(Integer.parseUnsignedInt(reset),
                        Integer.parseUnsignedInt(limit), Integer.parseUnsignedInt(remaining));
            } catch (Exception ignored) {
            }
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        public void set(@NotNull OptionalLong reset, @NotNull OptionalLong limit, @NotNull OptionalLong remaining) {
            //noinspection SimplifyOptionalCallChains
            if (!reset.isPresent() || !limit.isPresent() || !remaining.isPresent()) return;

            try {
                set(Math.toIntExact(reset.getAsLong()),
                        Math.toIntExact(limit.getAsLong()), Math.toIntExact(remaining.getAsLong()));
            } catch (Exception ignored) {
            }
        }

        @Range(from = 0, to = Integer.MAX_VALUE)
        public int getLimit() {
            return limit;
        }

        @Range(from = 0, to = Integer.MAX_VALUE)
        public int getRemaining() {
            return remaining;
        }

        public int getToReset() {
            return Math.toIntExact(reset - System.currentTimeMillis());
        }

        public boolean isFail() {
            return limit == 0;
        }

        @Override
        public String toString() {
            return "ServerResponse{" +
                    "limit=" + limit +
                    ", remaining=" + remaining +
                    ", reset=" + reset +
                    '}';
        }
    }
}
