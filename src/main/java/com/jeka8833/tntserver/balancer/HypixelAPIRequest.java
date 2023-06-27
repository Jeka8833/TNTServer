package com.jeka8833.tntserver.balancer;

import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.HypixelPlayer;
import com.jeka8833.tntserver.database.storage.HypixelPlayerError;
import com.jeka8833.tntserver.database.storage.HypixelPlayerLoading;
import com.jeka8833.tntserver.database.storage.HypixelPlayerStorage;
import com.jeka8833.tntserver.util.HypixelRateLimiter;
import com.jeka8833.tntserver.util.Util;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class HypixelAPIRequest implements Balancer<UUID, HypixelPlayer> {

    private static final int MAX_LINE_SIZE = 8;

    private static volatile boolean isSleep = true;
    private static final Logger logger = LogManager.getLogger(HypixelAPIRequest.class);
    private static final BlockingQueue<Request> QUEUE = new LinkedBlockingQueue<>();
    private static final HypixelRateLimiter RATE_LIMITER = new HypixelRateLimiter(Duration.ofMinutes(5), 4);

    private static final Object MUTEX = new Object();

    @SerializedName("player")
    public HypixelPlayerStorage player = new HypixelPlayerStorage();

    @Override
    public boolean requestInfo(@NotNull UUID key, @NotNull Consumer<HypixelPlayer> data) {
        Player player = PlayersDatabase.getOrCreate(key);

        synchronized (MUTEX) {  // Bad but Hypixel API much slower
            if (player.hypixelPlayerInfo == null ||
                    (player.hypixelPlayerInfo instanceof HypixelPlayerStorage playerStorage &&
                            playerStorage.invalidateAt < System.currentTimeMillis())) {
                Queue<Consumer<HypixelPlayer>> tasks = new ConcurrentLinkedQueue<>();
                tasks.add(data);
                player.hypixelPlayerInfo = new HypixelPlayerLoading(tasks);

                QUEUE.add(new Request(key, (HypixelPlayerLoading) player.hypixelPlayerInfo));
            } else if (player.hypixelPlayerInfo instanceof HypixelPlayerLoading loading) {
                loading.listeners().add(data);
            }
        }
        return true;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int getFree() {
        int available = RATE_LIMITER.getFreeAtMoment().orElse(1 - MAX_LINE_SIZE) + MAX_LINE_SIZE;
        int using = QUEUE.size() + (isSleep ? 0 : 1);
        return Math.max(0, available - using);
    }


    public static void init(String key) {
        final UUID secretKey = UUID.fromString(key);

        var thread = new Thread(() -> {
            while (true) {
                try {
                    Request request = QUEUE.take();
                    isSleep = false;

                    HypixelPlayer storage = readPlayer(request.player(), secretKey);
                    if (storage == null) storage = new HypixelPlayerError();

                    Player player = PlayersDatabase.getOrCreate(request.player());
                    player.hypixelPlayerInfo = storage;

                    Consumer<HypixelPlayer> listener;
                    while ((listener = request.callback().listeners().poll()) != null) {
                        listener.accept(storage);
                    }
                } catch (InterruptedException interruptedException) {
                    return;
                } catch (Exception e) {
                    logger.error("Hypixel Consumer", e);
                } finally {
                    isSleep = true;
                }
            }
        }, "Hypixel Manager");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    @Nullable
    public static HypixelPlayerStorage readPlayer(@NotNull UUID playerUUID, @NotNull UUID key)
            throws InterruptedException {
        HypixelRateLimiter.ServerResponse response = null;
        try {
            HttpRequest request = HttpRequest
                    .newBuilder(URI.create("https://api.hypixel.net/player?uuid=" + playerUUID))
                    .header("API-Key", key.toString()).build();

            HttpResponse<InputStream> serverResponse = Util.client.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            response = HypixelRateLimiter.ServerResponse.create(
                    serverResponse.headers().firstValueAsLong("RateLimit-Reset"),
                    serverResponse.headers().firstValueAsLong("RateLimit-Limit"),
                    serverResponse.headers().firstValueAsLong("RateLimit-Remaining"));

            if (serverResponse.statusCode() == 200) {
                HypixelAPIRequest structure = Util.GSON.fromJson(
                        new InputStreamReader(serverResponse.body(), StandardCharsets.UTF_8), HypixelAPIRequest.class);
                if (structure == null || structure.player == null) return null;

                return structure.player;
            }
        } catch (InterruptedException interruptedException) {
            throw interruptedException;
        } catch (Exception ignored) {
            // Ignore
        } finally {
            RATE_LIMITER.receiveAndLock(response);
        }
        return null;
    }

    private record Request(UUID player, HypixelPlayerLoading callback) {
    }
}
