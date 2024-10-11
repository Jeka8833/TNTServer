package com.jeka8833.tntserver.requester;

import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.metric.providers.RequesterCacheProvider;
import com.jeka8833.tntserver.mojang.MojangAPI;
import com.jeka8833.tntserver.requester.balancer.RequestBalancer;
import com.jeka8833.tntserver.requester.balancer.SilentCancelException;
import com.jeka8833.tntserver.requester.storage.CacheValue;
import com.jeka8833.tntserver.requester.storage.HypixelCompactStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class HypixelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(HypixelCache.class);

    private static final CustomLoadingCache<UUID, CacheValue, String> CACHE = new CustomLoadingCache<>(
            (key, oldValue, loadParameter) -> {
                CacheValue cacheValue = oldValue == null ? new CacheValue() : oldValue;

                HypixelCompactStructure response = RequestBalancer.get(key, cacheValue);
                cacheValue.update(response, loadParameter);

                return cacheValue;
            }, TimeUnit.DAYS.toNanos(2),
            Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());

    public static void get(@NotNull UUID sender, @Nullable UUID @NotNull [] requestedPlayers,
                           @NotNull Consumer<@NotNull Map<@Nullable UUID, @NotNull HypixelCompactStructure>> listener,
                           @NotNull Runnable end) {
        RequesterCacheProvider.requestedCount.getAndAccumulate(requestedPlayers.length, Long::sum);

        String gameInfo = PlayersDatabase.getGameInfo(sender);

        SequencedCollection<UUID> needRequest = new ArrayDeque<>(requestedPlayers.length);
        Map<UUID, HypixelCompactStructure> instantSend = new HashMap<>();

        for (UUID requestedPlayer : requestedPlayers) {
            if (MojangAPI.isPlayer(requestedPlayer)) {
                CacheValue oldValue = CACHE.getIfPresent(requestedPlayer);
                if (oldValue != null) {
                    instantSend.put(requestedPlayer, oldValue.getCompactStructure());

                    if (oldValue.isGameInfoDifferent(gameInfo)) {
                        RequesterCacheProvider.updateCount.getAndIncrement();

                        needRequest.addLast(requestedPlayer);
                    }
                } else {
                    RequesterCacheProvider.missCount.getAndIncrement();

                    needRequest.addFirst(requestedPlayer);
                }
            } else {
                instantSend.put(requestedPlayer, HypixelCompactStructure.EMPTY_INSTANCE);
            }
        }

        if (!instantSend.isEmpty()) {
            listener.accept(instantSend);
        }

        if (needRequest.isEmpty()) {
            end.run();

            return;
        }

        Collection<CompletableFuture<CacheValue>> futures = new ArrayList<>(needRequest.size());
        for (UUID requestedPlayer : needRequest) {
            HypixelCompactStructure oldValue = instantSend.get(requestedPlayer);

            CompletableFuture<CacheValue> newValue = CACHE.reload(requestedPlayer, gameInfo);
            futures.add(newValue);

            newValue.whenComplete((cacheValue, throwable) -> {
                if (throwable instanceof CustomLoadingCache.LoadException e &&
                        e.getCause() instanceof SilentCancelException) {
                    return;
                }

                if (throwable != null) {
                    LOGGER.warn("Error while getting Hypixel data for player {}", requestedPlayer, throwable);

                    return;
                }

                if (cacheValue == null) return;

                if (!Objects.equals(oldValue, cacheValue.getCompactStructure())) {
                    RequesterCacheProvider.loadSuccessNewCount.getAndIncrement();

                    listener.accept(Map.of(requestedPlayer, cacheValue.getCompactStructure()));
                } else {
                    RequesterCacheProvider.loadSuccessSameCount.getAndIncrement();
                }
            });
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .whenComplete((unused, throwable) -> end.run());
    }

    public static void storeToFile(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to create cache file", e);
        }

        CACHE.cleanOld();
        Map<UUID, CacheValue> map = CACHE.asMap();

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(path))) {
            objectOutputStream.writeObject(map);
        } catch (Exception e) {
            LOGGER.warn("Failed to store cache to file", e);
        }
    }

    public static void loadFromFile(Path path) {
        if (!Files.exists(path)) {
            LOGGER.info("Cache file not found, creating new one");
            return;
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(path))) {
            //noinspection unchecked
            HashMap<UUID, CacheValue> map = (HashMap<UUID, CacheValue>) objectInputStream.readObject();

            CACHE.putAll(map);
        } catch (Exception e) {
            LOGGER.warn("Failed to load cache from file", e);
        }
    }

    public static long size() {
        return CACHE.size();
    }
}
