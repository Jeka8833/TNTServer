package com.jeka8833.tntserver.requester.node;

import com.alibaba.fastjson2.JSON;
import com.jeka8833.tntserver.requester.ratelimiter.AsyncHypixelRateLimiter;
import com.jeka8833.tntserver.requester.ratelimiter.HypixelResponse;
import com.jeka8833.tntserver.requester.storage.HypixelCompactStorage;
import com.jeka8833.tntserver.requester.storage.HypixelJSONStructure;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalNode implements RequesterNode {
    private final Collection<Thread> threadList = ConcurrentHashMap.newKeySet();
    private final Collection<Thread> blockedThreadsCounts = new HashSet<>();

    private final AsyncHypixelRateLimiter rateLimiter;
    private final String key;
    private final OkHttpClient httpClient;
    private final int overloadLimit;

    public LocalNode(AsyncHypixelRateLimiter rateLimiter, UUID key, OkHttpClient client,
                     int overloadLimit) {
        this.rateLimiter = rateLimiter;
        this.key = key.toString();
        httpClient = client;
        this.overloadLimit = overloadLimit;
    }

    @Override
    public @NotNull HypixelCompactStorage get(@NotNull UUID requestedPlayer) throws Exception {
        threadList.add(Thread.currentThread());

        try (HypixelResponse serverStatus = new HypixelResponse(rateLimiter)) {
            release();

            Request request = new Request.Builder()
                    .url("https://api.hypixel.net/v2/player?uuid=" + requestedPlayer)
                    .header("API-Key", key)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                serverStatus.setHeaders(response.code(),
                        response.header("RateLimit-Reset"),
                        response.header("RateLimit-Limit"),
                        response.header("RateLimit-Remaining"));

                if (!response.isSuccessful()) throw new IOException("Hypixel API request returned: " + response.code());

                try (ResponseBody body = response.body(); InputStream reader = body.byteStream()) {
                    HypixelJSONStructure object = JSON.parseObject(reader, HypixelJSONStructure.class);
                    if (object == null) {
                        throw new IOException("Hypixel API request returned empty response");
                    }

                    return HypixelCompactStorage.compress(object);
                }
            }
        } finally {
            threadList.remove(Thread.currentThread());
            release();
        }
    }

    @Override
    public int getPriority() {
        return PRIORITY_MAIN_SERVER;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int getAvailable() {
        return rateLimiter.getFreeAtMoment();
    }

    @Override
    public boolean tryTake() {
        synchronized (blockedThreadsCounts) {
            boolean isAvailable = getAvailable() - blockedThreadsCounts.size() + overloadLimit > 0;
            if (isAvailable) {
                blockedThreadsCounts.add(Thread.currentThread());
            }

            return isAvailable;
        }
    }

    @Override
    public void release() {
        synchronized (blockedThreadsCounts) {
            blockedThreadsCounts.remove(Thread.currentThread());
        }
    }

    @Override
    public void cancelAll() {
        for (Thread thread : threadList) {
            thread.interrupt();
        }

        synchronized (blockedThreadsCounts) {
            for (Thread thread : blockedThreadsCounts) {
                thread.interrupt();
            }
        }
    }
}
