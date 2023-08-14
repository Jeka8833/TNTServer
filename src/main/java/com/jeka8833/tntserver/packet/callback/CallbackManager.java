package com.jeka8833.tntserver.packet.callback;

import com.jeka8833.tntserver.Main;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class CallbackManager {
    private static final Map<Short, PacketListener> packetsListeners = new ConcurrentHashMap<>();
    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static <T extends PacketCallback> void sendAndGetResult(@NotNull WebSocket socket, @NotNull T packet,
                                                                   @NotNull Consumer<@Nullable T> callback) {
        short id = (short) COUNTER.getAndIncrement();
        packet.setUniqueID(id);

        //noinspection unchecked
        packetsListeners.put(id, new PacketListener((Consumer<PacketCallback>) callback));

        Main.serverSend(socket, packet);
    }

    public static void callPacket(@NotNull PacketCallback packet) {
        PacketListener queue = packetsListeners.remove(packet.getUniqueID());
        if (queue == null) return;

        queue.call(packet);
    }

    public static void checkTimeout() {
        packetsListeners.values().removeIf(PacketListener::checkAndRemoveTimeout);
    }

    private static class PacketListener {
        private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(10);
        private final long timeoutAt = System.currentTimeMillis() + TIMEOUT;

        private Consumer<PacketCallback> listener;

        private PacketListener(Consumer<PacketCallback> listener) {
            this.listener = listener;
        }

        private boolean checkAndRemoveTimeout() {
            boolean isTimeout = System.currentTimeMillis() > timeoutAt;
            if (isTimeout) call(null);

            return isTimeout;
        }

        private synchronized void call(@Nullable PacketCallback packet) {
            listener.accept(packet);

            listener = null;
        }
    }
}
