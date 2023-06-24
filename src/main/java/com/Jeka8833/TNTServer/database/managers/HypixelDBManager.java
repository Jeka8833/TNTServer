package com.Jeka8833.TNTServer.database.managers;

import com.Jeka8833.TNTServer.AuthManager;
import com.Jeka8833.TNTServer.database.Player;
import com.Jeka8833.TNTServer.database.PlayersDatabase;
import com.Jeka8833.TNTServer.database.storage.HypixelPlayer;
import com.Jeka8833.TNTServer.database.storage.HypixelPlayerError;
import com.Jeka8833.TNTServer.database.storage.HypixelPlayerLoading;
import com.Jeka8833.TNTServer.database.storage.HypixelPlayerStorage;
import com.Jeka8833.TNTServer.util.SingleThreadHypixelRateLimiter;
import com.Jeka8833.TNTServer.util.Util;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class HypixelDBManager {
    public static final SingleThreadHypixelRateLimiter RATE_LIMITER =
            new SingleThreadHypixelRateLimiter(Duration.ofMinutes(5), SingleThreadHypixelRateLimiter.ONE_GROUP);
    private static final CopyOnWriteArrayList<PlayerRequest> REQUEST_QUEUE = new CopyOnWriteArrayList<>();
    private static final BlockingQueue<SendTimeout> SEND_TIMEOUTS = new DelayQueue<>();
    private static final Logger logger = LogManager.getLogger(AuthManager.class);
    private static final Random RANDOM = new Random();

    private static String key;

    public static void init(String key) {
        HypixelDBManager.key = key;

        var thread = new Thread(() -> {
            while (true) {
                try {
                    SendTimeout sendTimeout;
                    while ((sendTimeout = SEND_TIMEOUTS.poll()) != null) sendTimeout.event.run();

                    int size = REQUEST_QUEUE.size();
                    if (size == 0) {
                        //noinspection BusyWait
                        Thread.sleep(500);
                        continue;
                    }

                    // Not perfect entropy, but it's something
                    PlayerRequest playerRequest = REQUEST_QUEUE.remove(RANDOM.nextInt(size));

                    HypixelPlayer storage = readPlayer(playerRequest.uuid);
                    if (storage == null) storage = new HypixelPlayerError();

                    Player player = PlayersDatabase.getOrCreate(playerRequest.uuid);
                    player.hypixelPlayerInfo = storage;

                    for (Consumer<HypixelPlayer> listener : playerRequest.listener) {
                        listener.accept(storage);
                    }
                } catch (InterruptedException interruptedException) {
                    return;
                } catch (Exception e) {
                    logger.error("Hypixel Consumer", e);
                }
            }
        }, "Hypixel Manager");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Not thread safe if same time call one player
     */
    public static void requestPlayer(@NotNull UUID uuid, @Nullable Consumer<@NotNull HypixelPlayer> listener) {
        Player player = PlayersDatabase.getOrCreate(uuid);

        if (player.hypixelPlayerInfo == null) {
            var playerRequest = new PlayerRequest(uuid);
            if (listener != null) playerRequest.listener.add(listener);

            player.hypixelPlayerInfo = new HypixelPlayerLoading(playerRequest.listener);

            REQUEST_QUEUE.add(playerRequest);
        } else if (player.hypixelPlayerInfo instanceof HypixelPlayerLoading) {
            if (listener != null) {
                ((HypixelPlayerLoading) player.hypixelPlayerInfo)
                        .listeners().add(listener);
            }
        } else {
            if (listener != null) listener.accept(player.hypixelPlayerInfo);
        }
    }

    public static void requestPlayers(@NotNull UUID[] uuids,
                                      @Nullable Consumer<@NotNull PlayersReady> listener,
                                      long timeSend, int maxSendCount) {
        if (listener == null) {
            for (UUID uuid : uuids) requestPlayer(uuid, null);
        } else {
            AtomicInteger receivedCount = new AtomicInteger(uuids.length);
            AtomicInteger sendCount = new AtomicInteger(maxSendCount);

            Collection<Player> received = new ArrayList<>();    // Not thread concurrent

            for (int i = 1; i <= maxSendCount; i++) {
                SEND_TIMEOUTS.add(new SendTimeout(() -> {
                    if (receivedCount.get() == 0) return;

                    int count = sendCount.decrementAndGet();

                    listener.accept(new PlayersReady(received, count == 0));
                    received.clear();
                }, timeSend * i));
            }

            for (UUID uuid : uuids) {
                requestPlayer(uuid, hypixelPlayer -> {
                    if (sendCount.get() == 0) return;

                    received.add(PlayersDatabase.getOrCreate(uuid));

                    int count = receivedCount.decrementAndGet();
                    if (count == 0) {
                        listener.accept(new PlayersReady(received, true));
                        received.clear();
                    }
                });
            }
        }
    }

    @Nullable
    public static HypixelPlayerStorage readPlayer(@NotNull UUID playerUUID) throws InterruptedException {
        SingleThreadHypixelRateLimiter.ServerResponse response = new SingleThreadHypixelRateLimiter.ServerResponse();
        try {
            HttpRequest request = HttpRequest
                    .newBuilder(URI.create("https://api.hypixel.net/player?uuid=" + playerUUID))
                    .header("API-Key", key).build();

            HttpResponse<String> serverResponse = Util.client.send(request, HttpResponse.BodyHandlers.ofString());
            response.set(
                    serverResponse.headers().firstValueAsLong("RateLimit-Reset"),
                    serverResponse.headers().firstValueAsLong("RateLimit-Limit"),
                    serverResponse.headers().firstValueAsLong("RateLimit-Remaining"));

            if (serverResponse.statusCode() == 200) {
                HypixelDBManager structure = Util.GSON.fromJson(serverResponse.body(), HypixelDBManager.class);
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

    @SerializedName("player")
    public HypixelPlayerStorage player = new HypixelPlayerStorage();


    public record PlayersReady(@NotNull Collection<@NotNull Player> ready, boolean lastPacket) {
    }

    private record SendTimeout(@NotNull Runnable event, long timeoutTime) implements Delayed {
        private SendTimeout(@NotNull Runnable event, long timeoutTime) {
            this.event = event;
            this.timeoutTime = System.currentTimeMillis() + timeoutTime;
        }

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            long diff = timeoutTime - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@NotNull Delayed o) {
            return Long.compare(timeoutTime, ((SendTimeout) o).timeoutTime);
        }
    }

    private static class PlayerRequest {
        private final @NotNull UUID uuid;
        private final Collection<Consumer<HypixelPlayer>> listener = new CopyOnWriteArrayList<>();

        public PlayerRequest(@NotNull UUID uuid) {
            this.uuid = uuid;
        }
    }
}
