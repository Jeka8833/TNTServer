package com.jeka8833.tntserver.balancer;

import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.HypixelPlayer;
import com.jeka8833.tntserver.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class HypixelBalancer {
    private static final long TIME_TO_LEAVE_FROM_QUEUE = TimeUnit.MINUTES.toMillis(1);
    @SuppressWarnings("unchecked")
    private static final Balancer<UUID, HypixelPlayer>[] SOURCES =
            new Balancer[]{new HypixelAPIRequest(), new HypixelTNTRequest()};
    @SuppressWarnings("unchecked")
    private static final Balancer<UUID, HypixelPlayer>[] SOURCES_CACHE = new Balancer[]{new HypixelCache()};

    private static final Logger logger = LogManager.getLogger(HypixelAPIRequest.class);
    private static final Map<UUID, Long> PLAYER_TIMEOUTS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService TIMEOUT_THREAD = Executors.newScheduledThreadPool(1,
            Util.getThreadFactory("Scheduled Hypixel Answer", Thread.MIN_PRIORITY, true));

    private static boolean tryGet(@NotNull UUID player, @NotNull Consumer<HypixelPlayer> listener) {
        try {
            for (Balancer<UUID, HypixelPlayer> balancer : SOURCES) {
                if (balancer.getFree() > 0 && balancer.requestInfo(player, listener)) return true;
            }
        } catch (Exception e) {
            logger.error("Hypixel Balancer", e);
        }
        return false;
    }

    private static boolean tryGetCache(@NotNull UUID player, @NotNull Consumer<HypixelPlayer> listener) {
        try {
            for (Balancer<UUID, HypixelPlayer> balancer : SOURCES_CACHE) {
                if (balancer.requestInfo(player, listener)) return true;
            }
        } catch (Exception e) {
            logger.error("Hypixel Balancer", e);
        }
        return false;
    }

    // TODO: Need rewrite
    public static void tryGet(@NotNull UUID sender, @NotNull Collection<@NotNull UUID> players,
                              @NotNull Consumer<PlayersReady> listener, long timeSend, int maxSendCount) {
        PLAYER_TIMEOUTS.put(sender, System.currentTimeMillis() + TIME_TO_LEAVE_FROM_QUEUE);

        Queue<Player> received = new ConcurrentLinkedQueue<>();
        AtomicInteger sendCount = new AtomicInteger(maxSendCount);
        AtomicInteger receivedCount = new AtomicInteger();
        AtomicInteger currentRequests = new AtomicInteger();

        ScheduledFuture<?> timer = TIMEOUT_THREAD.scheduleAtFixedRate(() -> {
            int count = sendCount.decrementAndGet();
            if (count < 0) throw new NullPointerException("Timer stop");

            if (count == 0) {
                listener.accept(new PlayersReady(received, true));
                throw new NullPointerException("Timer stop");
            } else {
                int c = receivedCount.get();
                if (c >= currentRequests.get()) {
                    if (c == currentRequests.get()) {
                        listener.accept(new PlayersReady(received, true));
                    }

                    throw new NullPointerException("Timer stop");
                } else {
                    Collection<Player> collection = Util.pollAll(received);
                    if (!collection.isEmpty()) listener.accept(new PlayersReady(collection, false));
                }
            }
        }, timeSend, timeSend, TimeUnit.SECONDS);

        int maxRequests = getFreeAtMoment() / getCountOfRequesters();
        for (UUID player : players) {
            boolean onCache = tryGetCache(player, hypixelPlayer -> received.add(PlayersDatabase.getOrCreate(player)));
            if (onCache || currentRequests.get() >= maxRequests) continue;

            boolean isTake = tryGet(player, hypixelPlayer -> {
                received.add(PlayersDatabase.getOrCreate(player));
                int count = receivedCount.incrementAndGet();
                if (count >= currentRequests.get()) {
                    timer.cancel(true);
                    sendCount.getAndDecrement();

                    if (count == currentRequests.get()) {
                        listener.accept(new PlayersReady(received, true));
                    }
                }
            });
            if (isTake) currentRequests.getAndIncrement();
        }

        int count = receivedCount.get();
        if (count >= currentRequests.get()) {
            timer.cancel(true);
            sendCount.getAndDecrement();

            if (count == currentRequests.get()) {
                listener.accept(new PlayersReady(received, true));
            }
        }

        HypixelTNTRequest.sendRequest();
    }

    @Range(from = 0, to = Integer.MAX_VALUE)
    public static int getCountOfRequesters() {
        long currentTime = System.currentTimeMillis();
        int count = 0;

        Iterator<Long> iterator = PLAYER_TIMEOUTS.values().iterator();
        while (iterator.hasNext()) {
            if (currentTime < iterator.next()) {
                count++;
            } else {
                iterator.remove();
            }
        }

        return count;
    }

    @Range(from = 0, to = Integer.MAX_VALUE)
    public static int getFreeAtMoment() {
        int total = 0;
        for (Balancer<UUID, HypixelPlayer> balancer : SOURCES) {
            total += balancer.getFree();
        }
        return total;
    }

    public record PlayersReady(@NotNull Collection<@NotNull Player> ready, boolean lastPacket) {
    }
}
