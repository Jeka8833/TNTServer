package com.jeka8833.tntserver.requester;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
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
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class HypixelCache {
    private static final LoadingCache<UUID, CacheValue> CACHE = Caffeine.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .maximumSize(150_000)
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build(loadStrategy());

    private static final Logger LOGGER = LoggerFactory.getLogger(HypixelCache.class);
    private static final ThreadLocal<String> GAME_INFO = new ThreadLocal<>();

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

        GAME_INFO.set(gameInfo);

        Collection<CompletableFuture<CacheValue>> futures = new ArrayList<>(needRequest.size());
        for (UUID requestedPlayer : needRequest) {
            HypixelCompactStructure oldValue = instantSend.get(requestedPlayer);

            CompletableFuture<CacheValue> newValue = CACHE.refresh(requestedPlayer);
            futures.add(newValue);

            newValue.whenComplete((cacheValue, throwable) -> {
                if (throwable instanceof SilentCancelException || (throwable instanceof CompletionException &&
                        throwable.getCause() instanceof SilentCancelException)) {
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
        Map<UUID, CacheValue> map = new HashMap<>(CACHE.asMap());

        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to create cache file", e);
        }

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

            for (HashMap.Entry<UUID, CacheValue> entry : map.entrySet()) {
                CACHE.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load cache from file", e);
        }
    }

    public static long size() {
        return CACHE.estimatedSize();
    }

    private static CacheLoader<UUID, CacheValue> loadStrategy() {
        return new CacheLoader<>() {
            @Override
            public CacheValue load(UUID key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<CacheValue> asyncLoad(UUID key, Executor executor) {
                requireNonNull(key);
                requireNonNull(executor);

                String gameInfo = GAME_INFO.get();

                return CompletableFuture.supplyAsync(() -> {
                    try {
                        CacheValue cacheValue = new CacheValue();

                        HypixelCompactStructure response = RequestBalancer.get(key, cacheValue);
                        cacheValue.update(response, gameInfo);

                        return cacheValue;
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor);
            }

            @Override
            public CompletableFuture<CacheValue> asyncReload(UUID key, CacheValue oldValue, Executor executor) {
                requireNonNull(key);
                requireNonNull(executor);

                String gameInfo = GAME_INFO.get();

                return CompletableFuture.supplyAsync(() -> {
                    try {
                        HypixelCompactStructure response = RequestBalancer.get(key, oldValue);
                        oldValue.update(response, gameInfo);

                        return oldValue;
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor);
            }
        };
    }
}
