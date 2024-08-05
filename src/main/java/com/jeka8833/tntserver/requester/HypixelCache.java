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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class HypixelCache {
    private static final TaskManager TASK_MANAGER = new TaskManager();
    private static final Logger LOGGER = LoggerFactory.getLogger(HypixelCache.class);

    private static final LoadingCache<UUID, HypixelCompactStorage> CACHE = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .refreshAfterWrite(2, TimeUnit.MINUTES)
            .maximumWeight(256 * 1024 * 1024 - 1120) // 256 MB - 1120 bytes(Caffeine Baseline) -> 1 242 751 entries
            .weigher((k, v) -> {
                if (v == HypixelCompactStorage.EMPTY_INSTANCE) {
                    return 32 + 88; // HypixelCompactStorage -> 0 bytes; UUID -> 32 bytes; Node -> 88 bytes
                } else {
                    return 96 + 32 + 88; // HypixelCompactStorage -> 96 bytes; UUID -> 32 bytes; Node -> 88 bytes
                }
            })
            .recordStats()
            .build(RequestBalancer::get);

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
                HypixelCompactStorage newValue = CACHE.get(requestedPlayer);

                if (oldValue == null || !oldValue.equals(newValue)) {
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
                    HypixelCompactStorage newValue = CACHE.get(requestedPlayer);
                    HypixelCompactStorage oldValue = instantSend.get(requestedPlayer);

                    if (oldValue == null || !oldValue.equals(newValue)) {
                        listener.accept(Map.of(requestedPlayer, newValue));
                    }
                } catch (CompletionException e) {
                    if (e.getCause() instanceof InterruptedException) {
                        return;
                    }

                    LOGGER.warn("Balancer has error for {} player", requestedPlayer, e);
                } catch (Exception e) {
                    LOGGER.warn("Balancer has error for {} player", requestedPlayer, e);
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
                    HypixelCompactStorage newValue = CACHE.get(requestedPlayer);
                    HypixelCompactStorage oldValue = instantSend.get(requestedPlayer);

                    if (oldValue == null || !oldValue.equals(newValue)) {
                        listener.accept(Map.of(requestedPlayer, newValue));
                    }
                } catch (CompletionException e) {
                    if (e.getCause() instanceof InterruptedException) {
                        return;
                    }

                    LOGGER.warn("Balancer has error for {} player", requestedPlayer, e);
                } catch (Exception e) {
                    LOGGER.warn("Balancer has error for {} player", requestedPlayer, e);
                } finally {
                    if (counter.incrementAndGet() == needRequest.size()) {
                        end.run();
                    }
                }
            }, sender, requestedPlayer, canBeCancelled);
        }
    }

    public static void cancelLoadFor(@NotNull UUID player, boolean all) {
        TASK_MANAGER.cancelFor(player, all);
    }

    public static CacheStats getStatistic() {
        return CACHE.stats();
    }
}
