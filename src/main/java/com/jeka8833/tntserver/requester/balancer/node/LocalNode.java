package com.jeka8833.tntserver.requester.balancer.node;

import com.alibaba.fastjson2.JSON;
import com.jeka8833.tntserver.requester.ratelimiter.HypixelRateLimiter;
import com.jeka8833.tntserver.requester.storage.HypixelCompactStructure;
import com.jeka8833.tntserver.requester.storage.HypixelJSONStructure;
import lombok.Locked;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public final class LocalNode implements BalancerNode {
    private final HypixelRateLimiter rateLimiter;
    private final UUID key;
    private final OkHttpClient httpClient;
    private final int overloadLimit;

    private final Collection<Thread> threadList = ConcurrentHashMap.newKeySet();
    private final Collection<Thread> blockedThreadsCounts = ConcurrentHashMap.newKeySet();

    @NotNull
    @Override
    public HypixelCompactStructure get(@NotNull UUID requestedPlayer) throws Exception {
        threadList.add(Thread.currentThread());

        try (HypixelRateLimiter.Status status = rateLimiter.newRequest()) {
            releaseReserve();

            Request request = new Request.Builder()
                    .url("https://api.hypixel.net/v2/player?uuid=" + requestedPlayer)
                    .header("API-Key", key.toString())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                status.connectionInfo(response.code(),
                        response.header("RateLimit-Remaining"),
                        response.header("RateLimit-Reset"));

                if (!response.isSuccessful()) {
                    status.setError(true);

                    throw new IOException("Hypixel API request returned: " + response.code());
                }

                status.setError(false);

                try (ResponseBody body = response.body(); InputStream reader = body.byteStream()) {
                    HypixelJSONStructure object = JSON.parseObject(reader, HypixelJSONStructure.class);
                    if (object == null) {
                        throw new IOException("Hypixel API request returned empty response");
                    }
                    return object.toCompactStructure();
                }
            }
        } finally {
            threadList.remove(Thread.currentThread());

            releaseReserve();
        }
    }

    @Override
    public long refreshTimeNanos(boolean isTNTClientUser, int wins) {
        return TimeUnit.HOURS.toNanos(3);
    }

    @Override
    public UUID getNodeUUID() {
        return key;
    }

    @Override
    public int getPriority() {
        return PRIORITY_MAIN_SERVER;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int getAvailable() {
        return rateLimiter.getRemaining();
    }

    @Locked
    @Override
    public boolean canReserve() {
        boolean isAvailable = getAvailable() - blockedThreadsCounts.size() + overloadLimit > 0;
        if (isAvailable) {
            blockedThreadsCounts.add(Thread.currentThread());
        }

        return isAvailable;
    }

    @Override
    public void releaseReserve() {
        blockedThreadsCounts.remove(Thread.currentThread());
    }

    @Override
    public void cancelAll() {
        for (Thread thread : threadList) {
            thread.interrupt();
        }

        for (Thread thread : blockedThreadsCounts) {
            thread.interrupt();
        }
    }
}
