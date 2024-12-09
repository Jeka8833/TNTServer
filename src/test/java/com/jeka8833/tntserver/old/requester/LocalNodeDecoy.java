package com.jeka8833.tntserver.old.requester;

import com.alibaba.fastjson2.JSON;
import com.jeka8833.tntserver.old.requester.balancer.node.BalancerNode;
import com.jeka8833.tntserver.old.requester.ratelimiter.HypixelRateLimiter;
import com.jeka8833.tntserver.old.requester.storage.HypixelCompactStructure;
import com.jeka8833.tntserver.old.requester.storage.HypixelJSONStructure;
import lombok.Locked;
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
public class LocalNodeDecoy implements BalancerNode {
    private final Collection<Thread> threadList = ConcurrentHashMap.newKeySet();
    private final Collection<Thread> blockedThreadsCountsNew = ConcurrentHashMap.newKeySet();

    private final HypixelRateLimiter rateLimiter;
    private final UUID key;
    private final OkHttpClient httpClient;
    private final int overloadLimit;

    public LocalNodeDecoy(HypixelRateLimiter rateLimiter, UUID key, OkHttpClient httpClient, int overloadLimit) {
        this.rateLimiter = rateLimiter;
        this.key = key;
        this.httpClient = httpClient;
        this.overloadLimit = overloadLimit;
    }

    @NotNull
    @Override
    public HypixelCompactStructure get(@NotNull UUID requestedPlayer) throws Exception {
        threadList.add(Thread.currentThread());

        try (HypixelRateLimiter.Status status = rateLimiter.newRequest()) {
            releaseReserve();

            Request request = new Request.Builder()
                    .url("https://2fab0cf2-3add-4943-97ba-be882387b452.mock.pstmn.io/test?uuid=" + requestedPlayer)
                    .header("API-Key", key.toString())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                status.connectionInfo(response.code(),
                        response.header("X-RateLimit-Remaining"),
                        "60");

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
        boolean isAvailable = getAvailable() - blockedThreadsCountsNew.size() + overloadLimit > 0;
        if (isAvailable) {
            blockedThreadsCountsNew.add(Thread.currentThread());
        }

        return isAvailable;
    }

    @Override
    public void releaseReserve() {
        blockedThreadsCountsNew.remove(Thread.currentThread());
    }

    @Override
    public void cancelAll() {
        for (Thread thread : threadList) {
            thread.interrupt();
        }

        for (Thread thread : blockedThreadsCountsNew) {
            thread.interrupt();
        }
    }
}
