package com.jeka8833.tntserver.requester;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class TaskManager {
    private static final ExecutorService EXECUTORS = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<UUID, Collection<Request>> SENDER_MAP = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> REQUESTED_PLAYER_LOCKS = new ConcurrentHashMap<>();

    public void newTask(@NotNull Runnable runnable, @NotNull UUID sender,
                        @NotNull UUID requestedPlayer, boolean canBeCancelled) {
        EXECUTORS.submit(() -> {
            AtomicInteger locksCount = REQUESTED_PLAYER_LOCKS.compute(requestedPlayer, (key, oldValue) -> {
                AtomicInteger value = oldValue == null ? new AtomicInteger() : oldValue;
                if (!canBeCancelled) value.getAndIncrement();
                return value;
            });

            var request = new Request(locksCount);

            Collection<Request> senderQueue = SENDER_MAP.compute(sender, (key, oldValue) -> {
                Collection<Request> value = oldValue == null ? ConcurrentHashMap.newKeySet() : oldValue;
                value.add(request);
                return value;
            });

            try {
                runnable.run();
            } finally {
                if (!canBeCancelled) locksCount.getAndDecrement();
                senderQueue.remove(request);

                clearUnused();
            }
        });
    }

    public void cancelFor(@NotNull UUID sender, boolean all) {
        Iterable<Request> queue = SENDER_MAP.get(sender);
        if (queue == null) return;

        if (all) {
            for (Request request : queue) {
                request.cancel();
            }
        } else {
            for (Request request : queue) {
                if (request.locksCount().get() <= 0) {
                    request.cancel();
                }
            }
        }
    }

    public void clearUnused() {
        SENDER_MAP.values().removeIf(Collection::isEmpty);
        REQUESTED_PLAYER_LOCKS.values().removeIf(i -> i.get() <= 0);
    }

    private record Request(Thread taskThread, AtomicInteger locksCount) {
        private Request(AtomicInteger locksCount) {
            this(Thread.currentThread(), locksCount);
        }

        private void cancel() {
            taskThread.interrupt();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Request request)) return false;

            return taskThread.equals(request.taskThread);
        }

        @Override
        public int hashCode() {
            return taskThread.hashCode();
        }
    }
}
