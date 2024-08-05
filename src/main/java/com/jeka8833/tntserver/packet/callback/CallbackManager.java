package com.jeka8833.tntserver.packet.callback;

import com.jeka8833.tntserver.TNTServer;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class CallbackManager {
    private static final Map<Short, PacketListener> PACKETS_LISTENERS = new ConcurrentHashMap<>();
    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static <T extends PacketCallback> void sendAndGetResult(@NotNull WebSocket socket, @NotNull T packet,
                                                                   @NotNull Consumer<@Nullable T> callback) {
        short id = (short) COUNTER.getAndIncrement();
        packet.setUniqueID(id);

        //noinspection unchecked
        PACKETS_LISTENERS.put(id, new PacketListener((Consumer<PacketCallback>) callback));

        TNTServer.serverSend(socket, packet);
    }

    public static void callPacket(@NotNull PacketCallback packet) {
        PacketListener queue = PACKETS_LISTENERS.remove(packet.getUniqueID());
        if (queue == null) return;

        queue.call(packet);
    }

    public static void checkTimeout() {
        PACKETS_LISTENERS.values().removeIf(PacketListener::checkAndRemoveTimeout);
    }

    private static class PacketListener {
        private static final long TIMEOUT = TimeUnit.SECONDS.toNanos(10);

        private final long START_TIME = System.nanoTime();
        private Consumer<PacketCallback> listener;

        private PacketListener(Consumer<PacketCallback> listener) {
            this.listener = listener;
        }

        private boolean checkAndRemoveTimeout() {
            boolean isTimeout = System.nanoTime() - START_TIME > TIMEOUT;
            if (isTimeout) call(null);

            return isTimeout;
        }

        private synchronized void call(@Nullable PacketCallback packet) {
            listener.accept(packet);

            listener = null;
        }
    }
}
