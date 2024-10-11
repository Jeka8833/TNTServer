package com.jeka8833.tntserver.requester;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class CustomLoadingCache<Key, Value, AdditionalParameter> {
    private final Map<Key, ValueTimeWrapper<Value>> map = new ConcurrentHashMap<>();
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
        ValueTimeWrapper<Value> future = map.get(key);
        if (future == null) return null;

        return future.getNow();
    }

    public CompletableFuture<Value> reload(Key key, AdditionalParameter parameter) {
        return map.compute(key, (key_, oldValueTimeWrapper) -> {
            if (oldValueTimeWrapper == null || oldValueTimeWrapper.value().isDone()) {
                Value oldValue = oldValueTimeWrapper == null ? null : oldValueTimeWrapper.getNow();

                CompletableFuture<Value> future = new CompletableFuture<>();

                executor.execute(() -> {
                    try {
                        future.complete(function.load(key_, oldValue, parameter));
                    } catch (Throwable e) {
                        future.completeExceptionally(new LoadException(oldValue, e));
                    }
                });

                return new ValueTimeWrapper<>(future);
            }

            return oldValueTimeWrapper;
        }).value();
    }

    public void cleanOld() {
        long now = System.nanoTime();

        map.entrySet().removeIf(entry ->
                now - entry.getValue().loadTimeNanos() > expirationTimeNanos);
    }

    public void putAll(Map<Key, Value> map) {
        for (Map.Entry<Key, Value> entry : map.entrySet()) {
            if (entry.getValue() == null) continue;

            this.map.put(entry.getKey(),
                    new ValueTimeWrapper<>(CompletableFuture.completedFuture(entry.getValue())));
        }

    }

    public Map<Key, Value> asMap() {
        Map<Key, Value> map = new HashMap<>(this.map.size());
        for (Map.Entry<Key, ValueTimeWrapper<Value>> entry : this.map.entrySet()) {
            Value value = entry.getValue().getNow();
            if (value == null) continue;

            map.put(entry.getKey(), value);
        }
        return map;
    }

    public int size() {
        return map.size();
    }

    public static final class LoadException extends Exception {
        private final Object previousValue;
        private final Throwable cause;

        private LoadException(Object previousValue, Throwable cause) {
            this.previousValue = previousValue;
            this.cause = cause;
        }

        public Object getPreviousValue() {
            return previousValue;
        }

        public Throwable getCause() {
            return cause;
        }
    }

    @FunctionalInterface
    public interface Loader<Key, Value, AdditionalParameter> {
        Value load(Key key, Value oldValue, AdditionalParameter parameter) throws Exception;
    }

    private static final class ValueTimeWrapper<Value> {
        private final Long loadTimeNanos = System.nanoTime();
        private final CompletableFuture<Value> value;

        private ValueTimeWrapper(CompletableFuture<Value> value) {
            this.value = value;
        }

        private Value getNow() {
            try {
                return value.getNow(null);
            } catch (CompletionException e) {
                if (e.getCause() instanceof LoadException) {
                    //noinspection unchecked
                    return (Value) ((LoadException) e.getCause()).getPreviousValue();
                }
            } catch (Exception ignored) {
            }

            return null;
        }

        private Long loadTimeNanos() {
            return loadTimeNanos;
        }

        private CompletableFuture<Value> value() {
            return value;
        }
    }
}
