package com.jeka8833.tntserver.old.requester;

import com.jeka8833.tntserver.old.database.PlayersDatabase;
import com.jeka8833.tntserver.old.metric.providers.RequesterCacheProvider;
import com.jeka8833.tntserver.old.mojang.MojangAPI;
import com.jeka8833.tntserver.old.requester.balancer.RequestBalancer;
import com.jeka8833.tntserver.old.requester.storage.CacheValue;
import com.jeka8833.tntserver.old.requester.storage.HypixelCompactStructure;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
public final class HypixelCache {

    private static final CustomLoadingCache<UUID, CacheValue, String> CACHE = new CustomLoadingCache<>(
            (key, oldValue, loadParameter) -> {
                CacheValue cacheValue = oldValue == null ? new CacheValue() : oldValue;

                HypixelCompactStructure response = RequestBalancer.get(key, cacheValue);
                cacheValue.update(response, loadParameter);

                return cacheValue;
            }, TimeUnit.DAYS.toNanos(3),
            Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());

    public static void get(@NotNull UUID sender, @NotNull Set<@Nullable UUID> requestedPlayers,
                           @NotNull Consumer<@NotNull Map<@Nullable UUID, @NotNull HypixelCompactStructure>> listener,
                           @NotNull Runnable end) {
        RequesterCacheProvider.requestedCount.getAndAdd(requestedPlayers.size());

        String gameInfo = PlayersDatabase.getGameInfo(sender);

        Collection<UUID> needRequest = new ArrayList<>();
        Map<UUID, HypixelCompactStructure> instantSend = new HashMap<>();

        for (UUID requestedPlayer : requestedPlayers) {
            if (MojangAPI.isPlayer(requestedPlayer)) {
                CacheValue oldValue = CACHE.getIfPresent(requestedPlayer);
                if (oldValue != null) {
                    instantSend.put(requestedPlayer, oldValue.getCompactStructure());

                    if (oldValue.isGameInfoDifferent(gameInfo)) {
                        RequesterCacheProvider.updateCount.getAndIncrement();

                        needRequest.add(requestedPlayer);
                    }
                } else {
                    RequesterCacheProvider.missCount.getAndIncrement();

                    needRequest.add(requestedPlayer);
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

        AtomicInteger counter = new AtomicInteger(needRequest.size());
        for (UUID requestedPlayer : needRequest) {
            CompletableFuture<CacheValue> newValue = CACHE.reload(requestedPlayer, gameInfo);

            newValue.whenComplete((cacheValue, throwable) -> {
                try {
                    if (cacheValue == null) return;

                    HypixelCompactStructure oldValue = instantSend.get(requestedPlayer);

                    if (Objects.equals(oldValue, cacheValue.getCompactStructure())) {
                        RequesterCacheProvider.loadSuccessSameCount.getAndIncrement();
                    } else {
                        listener.accept(Map.of(requestedPlayer, cacheValue.getCompactStructure()));

                        RequesterCacheProvider.loadSuccessNewCount.getAndIncrement();
                    }
                } finally {
                    if (counter.decrementAndGet() == 0) {
                        end.run();
                    }
                }
            });
        }
    }

    public static void storeToFile(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            log.warn("Failed to create cache file", e);
        }

        CACHE.cleanOld();
        Map<UUID, CacheValue> map = CACHE.asMap();

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(path))) {
            objectOutputStream.writeObject(map);
        } catch (Exception e) {
            log.warn("Failed to store cache to file", e);
        }
    }

    public static void loadFromFile(Path path) {
        if (!Files.exists(path)) {
            log.info("Cache file not found, creating new one");

            return;
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(path))) {
            //noinspection unchecked
            HashMap<UUID, CacheValue> map = (HashMap<UUID, CacheValue>) objectInputStream.readObject();

            CACHE.putAll(map);
        } catch (Exception e) {
            log.warn("Failed to load cache from file", e);
        }
    }

    public static long size() {
        return CACHE.size();
    }
}
