package com.jeka8833.tntserver.requester;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.jeka8833.tntserver.mojang.MojangAPI;
import com.jeka8833.tntserver.requester.storage.HypixelCompactStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class HypixelCache {
    private static final long REFRESH_INTERVAL = TimeUnit.MINUTES.toNanos(30);

    private static final LoadingCache<UUID, HypixelCompactStorage> CACHE = Caffeine.newBuilder()
            .refreshAfterWrite(REFRESH_INTERVAL, TimeUnit.NANOSECONDS)
            .maximumSize(500_000)       // (96 + 32 + 96) * 500_000 = +-112 MB
            .recordStats()
            .build(RequestBalancer::get);

    public static final TaskManager TASK_MANAGER = new TaskManager();
    private static final Logger LOGGER = LoggerFactory.getLogger(HypixelCache.class);

    private static final Map<UUID, Long> FORCE_REFRESH = new ConcurrentHashMap<>();
    private static final AtomicInteger FORCE_REFRESH_COUNTER = new AtomicInteger();

    public static void get(@NotNull UUID sender, @Nullable UUID requestedPlayer, boolean canBeCancelled,
                           @NotNull Consumer<@NotNull HypixelCompactStorage> listener) {
        if (!MojangAPI.isPlayer(requestedPlayer)) {
            listener.accept(HypixelCompactStorage.EMPTY_INSTANCE);
            return;
        }

        HypixelCompactStorage oldValue = CACHE.getIfPresent(requestedPlayer);
        if (oldValue != null) listener.accept(oldValue);

        //noinspection DataFlowIssue
        TASK_MANAGER.newTask(() -> {
            try {
                //noinspection DataFlowIssue
                HypixelCompactStorage newValue = getFromCache(requestedPlayer);

                if (!Objects.equals(oldValue, newValue)) {
                    listener.accept(newValue);
                }
            } catch (CompletionException e) {
                if (e.getCause() instanceof InterruptedException) {
                    return;
                }

                LOGGER.warn("Error while getting Hypixel data for player {}", requestedPlayer, e);
            } catch (Exception e) {
                LOGGER.warn("Error while getting Hypixel data for player {}", requestedPlayer, e);
            }
        }, sender, requestedPlayer, canBeCancelled);
    }

    public static void get(@NotNull UUID sender, @Nullable UUID @NotNull [] requestedPlayers, boolean canBeCancelled,
                           @NotNull Consumer<@Unmodifiable @NotNull Map<
                                   @Nullable UUID, @NotNull HypixelCompactStorage>> listener) {
        SequencedCollection<UUID> needRequest = new ArrayDeque<>(requestedPlayers.length);
        Map<UUID, HypixelCompactStorage> instantSend = new HashMap<>();

        for (UUID requestedPlayer : requestedPlayers) {
            if (MojangAPI.isPlayer(requestedPlayer)) {
                HypixelCompactStorage oldValue = CACHE.getIfPresent(requestedPlayer);
                if (oldValue != null) {
                    instantSend.put(requestedPlayer, oldValue);

                    needRequest.addLast(requestedPlayer);
                } else {
                    needRequest.addFirst(requestedPlayer);
                }
            } else {
                instantSend.put(requestedPlayer, HypixelCompactStorage.EMPTY_INSTANCE);
            }
        }

        if (!instantSend.isEmpty()) {
            listener.accept(Collections.unmodifiableMap(instantSend));
        }

        for (UUID requestedPlayer : needRequest) {
            TASK_MANAGER.newTask(() -> {
                try {
                    HypixelCompactStorage newValue = getFromCache(requestedPlayer);
                    HypixelCompactStorage oldValue = instantSend.get(requestedPlayer);

                    if (!Objects.equals(oldValue, newValue)) {
                        listener.accept(Map.of(requestedPlayer, newValue));
                    }
                } catch (CompletionException e) {
                    if (e.getCause() instanceof InterruptedException) {
                        return;
                    }

                    LOGGER.warn("Error while getting Hypixel data for player {}", requestedPlayer, e);
                } catch (Exception e) {
                    LOGGER.warn("Error while getting Hypixel data for player {}", requestedPlayer, e);
                }
            }, sender, requestedPlayer, canBeCancelled);
        }
    }

    public static void get(@NotNull UUID sender, @Nullable UUID @NotNull [] requestedPlayers, boolean canBeCancelled,
                           @NotNull Consumer<@Unmodifiable @NotNull Map<
                                   @Nullable UUID, @NotNull HypixelCompactStorage>> listener, @NotNull Runnable end) {
        SequencedCollection<UUID> needRequest = new ArrayDeque<>(requestedPlayers.length);
        Map<UUID, HypixelCompactStorage> instantSend = new HashMap<>();

        for (UUID requestedPlayer : requestedPlayers) {
            if (MojangAPI.isPlayer(requestedPlayer)) {
                HypixelCompactStorage oldValue = CACHE.getIfPresent(requestedPlayer);
                if (oldValue != null) {
                    instantSend.put(requestedPlayer, oldValue);
                    needRequest.addLast(requestedPlayer);
                } else {
                    needRequest.addFirst(requestedPlayer);
                }
            } else {
                instantSend.put(requestedPlayer, HypixelCompactStorage.EMPTY_INSTANCE);
            }
        }

        if (!instantSend.isEmpty()) {
            listener.accept(Collections.unmodifiableMap(instantSend));
        }

        if (needRequest.isEmpty()) {
            end.run();
            return;
        }

        AtomicInteger counter = new AtomicInteger();
        for (UUID requestedPlayer : needRequest) {
            TASK_MANAGER.newTask(() -> {
                try {
                    HypixelCompactStorage newValue = getFromCache(requestedPlayer);
                    HypixelCompactStorage oldValue = instantSend.get(requestedPlayer);

                    if (!Objects.equals(oldValue, newValue)) {
                        listener.accept(Map.of(requestedPlayer, newValue));
                    }
                } catch (CompletionException e) {
                    if (e.getCause() instanceof InterruptedException) {
                        return;
                    }

                    LOGGER.warn("Error while getting Hypixel data for player {}", requestedPlayer, e);
                } catch (Exception e) {
                    LOGGER.warn("Error while getting Hypixel data for player {}", requestedPlayer, e);
                } finally {
                    if (counter.incrementAndGet() == needRequest.size()) {
                        end.run();
                    }
                }
            }, sender, requestedPlayer, canBeCancelled);
        }
    }

    private static HypixelCompactStorage getFromCache(@NotNull UUID requestedPlayer) throws Exception {
        Long forceRefresh = FORCE_REFRESH.remove(requestedPlayer);
        if (forceRefresh != null) {
            FORCE_REFRESH_COUNTER.getAndIncrement();

            return CACHE.refresh(requestedPlayer).get();
        }

        return CACHE.get(requestedPlayer);
    }

    public static void forceRefresh(@NotNull UUID player) {
        long currentTime = System.nanoTime();
        FORCE_REFRESH.values().removeIf(time -> currentTime - time >= REFRESH_INTERVAL);

        FORCE_REFRESH.put(player, currentTime);
    }

    public static void cancelLoadFor(@NotNull UUID player, boolean all) {
        TASK_MANAGER.cancelFor(player, all);
    }

    public static CacheStats getStatistic() {
        return CACHE.stats();
    }

    public static int getStatisticForceRefresh() {
        return FORCE_REFRESH_COUNTER.get();
    }

    public static long size() {
        return CACHE.estimatedSize();
    }
}
