package com.jeka8833.tntserver.balancer;

import com.google.gson.annotations.SerializedName;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.HypixelPlayer;
import com.jeka8833.tntserver.database.storage.HypixelPlayerError;
import com.jeka8833.tntserver.database.storage.HypixelPlayerStorage;
import com.jeka8833.tntserver.util.HypixelRateLimiter;
import com.jeka8833.tntserver.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class HypixelAPIRequest implements Balancer<UUID, HypixelPlayer> {

    private static final int MAX_LINE_SIZE = 8;
    private static final HypixelRateLimiter RATE_LIMITER = new HypixelRateLimiter(Duration.ofMinutes(5), 4);

    private static final Logger logger = LogManager.getLogger(HypixelAPIRequest.class);
    private static final BlockingQueue<UUID> QUEUE = new LinkedBlockingQueue<>();
    private static volatile boolean isSleep = true;

    @SerializedName("player")
    public HypixelPlayerStorage player = new HypixelPlayerStorage();

    @Override
    public boolean requestInfo(@NotNull UUID key, @NotNull Consumer<HypixelPlayer> data) {
        Player player = PlayersDatabase.getOrCreate(key);

        return player.tryAddToLoadingQueue(loading -> QUEUE.offer(key), data);
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
                    UUID requestedPlayer = QUEUE.take();
                    isSleep = false;

                    HypixelPlayer storage = readPlayer(requestedPlayer, secretKey);
                    if (storage == null) storage = new HypixelPlayerError();

                    Player player = PlayersDatabase.getOrCreate(requestedPlayer);
                    player.setHypixelStorage(storage);
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

            // Answer size around 100-1000 KByte
            HttpResponse<InputStream> serverResponse =
                    Util.client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            response = HypixelRateLimiter.ServerResponse.create(
                    serverResponse.headers().firstValueAsLong("RateLimit-Reset"),
                    serverResponse.headers().firstValueAsLong("RateLimit-Limit"),
                    serverResponse.headers().firstValueAsLong("RateLimit-Remaining"));

            if (serverResponse.statusCode() == 200) {
                HypixelAPIRequest structure = Util.GSON.fromJson(
                        new BufferedReader(
                                new InputStreamReader(serverResponse.body(), StandardCharsets.UTF_8), 64 * 1024),
                        HypixelAPIRequest.class);
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
}
