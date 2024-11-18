package com.jeka8833.tntserver.requester;

import com.jeka8833.tntserver.requester.balancer.RequestBalancer;
import com.jeka8833.tntserver.requester.ratelimiter.HypixelRateLimiter;
import com.jeka8833.tntserver.requester.ratelimiter.strategy.TNTServerStrategyRefill;
import com.jeka8833.tntserver.util.Util;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

class HypixelCacheTest {
    private static final HypixelRateLimiter RATE_LIMITER = new HypixelRateLimiter(
            TimeUnit.SECONDS.toNanos(10),
            TimeUnit.MILLISECONDS.toNanos(100),
            new TNTServerStrategyRefill(120));
    @Test
    void get() throws InterruptedException {
        UUID[] dataset = generateDataset(10_000);

        LocalNodeDecoy localNodeDecoy =
                new LocalNodeDecoy(RATE_LIMITER, UUID.randomUUID(), Util.HTTP_CLIENT, 0);

        RequestBalancer.addNode(localNodeDecoy);

        for (int i = 0; i < 50; i++) {
            new Thread(() -> {
                while (true) {
                    Set<UUID> uuids = select(dataset, 24);

                    HypixelCache.get(UUID.randomUUID(), uuids, map -> {
                    }, () -> {
                    });

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }

        Thread.currentThread().join();
    }

    private static UUID[] generateDataset(int count) {
        UUID[] uuids = new UUID[count];
        for (int i = 0; i < count; i++) {
            uuids[i] = UUID.randomUUID();
        }

        return uuids;
    }

    private static Set<UUID> select(UUID[] uuids, int count) {
        Set<UUID> result = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            int index = ThreadLocalRandom.current().nextInt(uuids.length);

            result.add(uuids[index]);
        }

        return result;
    }
}