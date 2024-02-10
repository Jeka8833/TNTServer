package com.jeka8833.tntserver.balancer;

import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.database.storage.HypixelPlayer;
import com.jeka8833.tntserver.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class HypixelBalancer {

    public static final HypixelTNTRequest TNT_USER_REQUESTER = new HypixelTNTRequest();
    public static final HypixelTNTRequest TNT_BOT_REQUESTER = new HypixelTNTRequest();

    private static final long TIME_TO_LEAVE_FROM_QUEUE = TimeUnit.MINUTES.toNanos(1);
    @SuppressWarnings("unchecked")
    private static final Balancer<UUID, HypixelPlayer>[] SOURCES =
            new Balancer[]{new HypixelAPIRequest(), TNT_BOT_REQUESTER, TNT_USER_REQUESTER};
    @SuppressWarnings("unchecked")
    private static final Balancer<UUID, HypixelPlayer>[] SOURCES_CACHE = new Balancer[]{new HypixelCache()};

    private static final Logger logger = LogManager.getLogger(HypixelBalancer.class);
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

    public static void tryGet(@NotNull UUID sender, @NotNull Collection<@NotNull UUID> players,
                              @NotNull Consumer<PlayersReady> listener, long timeSend, int maxSendCount) {
        PLAYER_TIMEOUTS.put(sender, System.nanoTime());

        Queue<Player> received = new ConcurrentLinkedQueue<>();

        AtomicInteger sendCount = new AtomicInteger(maxSendCount);
        AtomicInteger receivedCount = new AtomicInteger();
        AtomicInteger expectedRequests = new AtomicInteger();
        AtomicBoolean blockSend = new AtomicBoolean(true);

        final int freeAtMoment = getFreeAtMoment();
        final int maxRequests = freeAtMoment == 0 ? 0 : Math.max(1, freeAtMoment / getCountOfRequesters());
        for (UUID player : players) {
            boolean onCache = tryGetCache(player, hypixelPlayer -> {
                User user = PlayersDatabase.getOrCreate(player);
                if (user instanceof Player send) {
                    received.add(send);
                }
            });

            if (onCache || expectedRequests.get() >= maxRequests) continue;

            boolean isTake = tryGet(player, hypixelPlayer -> {
                if (sendCount.get() <= 0) return;

                User user = PlayersDatabase.getOrCreate(player);
                if (user instanceof Player send) {
                    received.add(send);
                }

                if (expectedRequests.get() <= receivedCount.incrementAndGet() && !blockSend.get()) {
                    sendCount.set(0);

                    listener.accept(new PlayersReady(received, true));
                }
            });

            if (isTake) expectedRequests.getAndIncrement();
        }
        blockSend.set(false);

        HypixelTNTRequest.sendRequest();

        TIMEOUT_THREAD.scheduleAtFixedRate(() -> {
            int allowCount = sendCount.decrementAndGet();
            if (allowCount <= 0) {
                if (allowCount == 0) listener.accept(new PlayersReady(received, true));

                throw new NullPointerException("Timer stop");
            } else if (expectedRequests.get() <= receivedCount.get()) {
                listener.accept(new PlayersReady(received, true));

                throw new NullPointerException("Timer stop");
            } else {
                Collection<Player> collection = Util.pollAll(received);
                if (!collection.isEmpty()) listener.accept(new PlayersReady(collection, false));
            }
        }, 0, timeSend, TimeUnit.SECONDS);
    }

    @Range(from = 0, to = Integer.MAX_VALUE)
    public static int getCountOfRequesters() {
        int count = 0;

        Iterator<Long> iterator = PLAYER_TIMEOUTS.values().iterator();
        while (iterator.hasNext()) {
            if (System.nanoTime() - iterator.next() < TIME_TO_LEAVE_FROM_QUEUE) {
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
