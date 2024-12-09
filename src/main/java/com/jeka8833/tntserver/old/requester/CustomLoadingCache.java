package com.jeka8833.tntserver.old.requester;

import com.jeka8833.tntserver.old.requester.balancer.SilentCancelException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class CustomLoadingCache<Key, Value, AdditionalParameter> {
    private final Map<Key, TimedCompletableFuture<Value>> map = new ConcurrentHashMap<>();
    private final Loader<Key, Value, AdditionalParameter> function;
    private final long expirationTimeNanos;
    private final Executor executor;

    public CustomLoadingCache(Loader<Key, Value, AdditionalParameter> function, long expirationTimeNanos,
                              Executor executor, ScheduledExecutorService scheduledExecutorService) {
        this.function = function;
        this.expirationTimeNanos = expirationTimeNanos;
        this.executor = executor;

        scheduledExecutorService.scheduleWithFixedDelay(this::cleanOld, 5, 5, TimeUnit.MINUTES);
    }

    public Value getIfPresent(Key key) {
        TimedCompletableFuture<Value> future = map.get(key);
        if (future == null) return null;

        return future.getNow();
    }

    public CompletableFuture<Value> reload(Key key, AdditionalParameter parameter) {
        return map.compute(key, (key_, oldValueTimeWrapper) -> {
            if (oldValueTimeWrapper == null || oldValueTimeWrapper.isDone()) {
                TimedCompletableFuture<Value> future = new TimedCompletableFuture<>();

                executor.execute(() -> {
                    Value oldValue = oldValueTimeWrapper == null ? null : oldValueTimeWrapper.getNow();

                    try {
                        future.complete(function.load(key_, oldValue, parameter));
                    } catch (Throwable e) {
                        if (oldValue == null) {
                            map.remove(key_);   // Remove ghost value
                        }

                        future.completeExceptionally(new LoadException(oldValue));

                        // Bad, but memory optimization
                        if (e instanceof SilentCancelException) return;
                        log.warn("Error while getting Hypixel data for player {}", key_, e);
                    }
                });

                return future;
            }

            return oldValueTimeWrapper;
        });
    }

    public void cleanOld() {
        long now = System.nanoTime();

        map.values().removeIf(value -> now - value.getCreateTimeNanos() > expirationTimeNanos);
    }

    public void putAll(Map<Key, Value> map) {
        for (Map.Entry<Key, Value> entry : map.entrySet()) {
            if (entry.getValue() == null) continue;

            TimedCompletableFuture<Value> future = new TimedCompletableFuture<>();
            future.complete(entry.getValue());

            this.map.put(entry.getKey(), future);
        }

    }

    public Map<Key, Value> asMap() {
        Map<Key, Value> map = new HashMap<>(this.map.size());
        for (Map.Entry<Key, TimedCompletableFuture<Value>> entry : this.map.entrySet()) {
            Value value = entry.getValue().getNow();
            if (value == null) continue;

            map.put(entry.getKey(), value);
        }
        return map;
    }

    public int size() {
        return map.size();
    }

    @FunctionalInterface
    public interface Loader<Key, Value, AdditionalParameter> {
        Value load(Key key, Value oldValue, AdditionalParameter parameter) throws Exception;
    }

    @Getter
    @RequiredArgsConstructor
    private static final class LoadException extends Exception {
        private final Object previousValue;
    }

    @Getter
    private static final class TimedCompletableFuture<Value> extends CompletableFuture<Value> {
        private final long createTimeNanos = System.nanoTime();

        @Nullable
        private Value getNow() {
            try {
                return getNow(null);
            } catch (CompletionException e) {
                if (e.getCause() instanceof LoadException loadException) {
                    //noinspection unchecked
                    return (Value) loadException.getPreviousValue();
                }

                log.error("Failed to getNow value", e);
            } catch (Throwable e) {
                log.error("Failed to getNow value", e);
            }
            return null;
        }
    }
}
