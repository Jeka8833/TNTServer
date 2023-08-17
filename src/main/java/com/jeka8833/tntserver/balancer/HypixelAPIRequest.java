package com.jeka8833.tntserver.balancer;

import com.google.gson.annotations.SerializedName;
import com.jeka8833.tntserver.balancer.ratelimiter.AsyncHypixelRateLimiter;
import com.jeka8833.tntserver.balancer.ratelimiter.HypixelResponse;
import com.jeka8833.tntserver.balancer.ratelimiter.ResetManager;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.database.storage.HypixelPlayer;
import com.jeka8833.tntserver.database.storage.HypixelPlayerError;
import com.jeka8833.tntserver.database.storage.HypixelPlayerStorage;
import com.jeka8833.tntserver.util.Util;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class HypixelAPIRequest implements Balancer<UUID, HypixelPlayer> {
    private static final int THREAD_COUNT = 10;
    private static final AsyncHypixelRateLimiter RATE_LIMITER = new AsyncHypixelRateLimiter(
            new ResetManager(Duration.ofMinutes(5)),
            4, Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofSeconds(10));

    private static final Logger logger = LogManager.getLogger(HypixelAPIRequest.class);
    private static final BlockingQueue<UUID> QUEUE = new LinkedBlockingQueue<>();

    @SerializedName("player")
    public HypixelPlayerStorage player = new HypixelPlayerStorage();

    @Override
    public boolean requestInfo(@NotNull UUID key, @NotNull Consumer<HypixelPlayer> data) {
        User user = PlayersDatabase.getOrCreate(key);
        if (user instanceof Player requestedPlayer) {
            return requestedPlayer.tryAddToLoadingQueue(loading -> QUEUE.offer(key), data);
        }
        return false;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int getFree() {
        int available = RATE_LIMITER.isFail() ? 1 : RATE_LIMITER.getFreeAtMoment();
        return Math.max(0, available - QUEUE.size());
    }

    public static void init(String key) {
        final UUID secretKey = UUID.fromString(key);
        for (int i = 0; i < THREAD_COUNT; i++) {
            var thread = new Thread(() -> {
                while (true) {
                    UUID requestedPlayer = null;
                    try {
                        requestedPlayer = QUEUE.take();

                        HypixelPlayer storage = readPlayer(requestedPlayer, secretKey);
                        if (storage == null) storage = new HypixelPlayerError();

                        User user = PlayersDatabase.getOrCreate(requestedPlayer);
                        if (user instanceof Player player) player.setHypixelStorage(storage);
                    } catch (InterruptedException interruptedException) {
                        logger.warn("Force stop request: " + requestedPlayer);

                        if (requestedPlayer != null) {
                            //noinspection ResultOfMethodCallIgnored
                            QUEUE.offer(requestedPlayer);
                        }
                    } catch (Exception e) {
                        logger.error("Hypixel Requester", e);
                    }
                }
            }, "Hypixel Requester " + i);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Nullable
    public static HypixelPlayerStorage readPlayer(@NotNull UUID playerUUID, @NotNull UUID key)
            throws InterruptedException {
        try (HypixelResponse serverStatus = new HypixelResponse(RATE_LIMITER)) {
            Request request = new Request.Builder()
                    .url("https://api.hypixel.net/player?uuid=" + playerUUID)
                    .header("API-Key", key.toString())
                    .build();
            try (Response response = Util.clientOk.newCall(request).execute()) {
                serverStatus.setHeaders(response.code(),
                        response.header("RateLimit-Reset"),
                        response.header("RateLimit-Limit"),
                        response.header("RateLimit-Remaining"));

                if (response.isSuccessful()) {
                    //noinspection DataFlowIssue
                    try (ResponseBody body = response.body(); InputStream reader = body.byteStream()) {
                        HypixelAPIRequest structure = Util.GSON.fromJson(
                                new BufferedReader(new InputStreamReader(reader, StandardCharsets.UTF_8), 64 * 1024),
                                HypixelAPIRequest.class);
                        if (structure == null || structure.player == null) return null;

                        return structure.player;
                    }
                } else {
                    logger.warn("Hypixel API request returned: " + response.code());
                }
            }
        } catch (Exception e) {
            logger.warn("Hypixel API request", e);
        }
        return null;
    }
}
