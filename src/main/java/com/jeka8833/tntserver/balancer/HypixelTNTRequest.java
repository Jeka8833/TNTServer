package com.jeka8833.tntserver.balancer;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.HypixelPlayer;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.packets.RequestHypixelPlayerPacket;
import com.jeka8833.tntserver.packet.packets.UpdateFreeRequestsPacket;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class HypixelTNTRequest implements Balancer<UUID, HypixelPlayer> {

    private static final long AVAILABLE_TIMEOUT = 60_000L;

    @SuppressWarnings("unchecked")
    private static final Map.Entry<UUID, AvailableCount>[] TO_ARRAY_ENTRY =
            (Map.Entry<UUID, AvailableCount>[]) new Map.Entry[0];
    private static final Map<UUID, AvailableCount> AVAILABLE_COUNT_MAP = new ConcurrentHashMap<>();
    private static final Packet REQUEST_AVAILABLE_COUNT_PACKET = new UpdateFreeRequestsPacket();
    private static final Map<WebSocket, Queue<UUID>> SEND_QUEUE = new ConcurrentHashMap<>();
    private static final AtomicInteger TAKE_COUNT = new AtomicInteger();

    @Override
    public boolean requestInfo(@NotNull UUID key, @NotNull Consumer<HypixelPlayer> data) {
        AvailableCount client = takeClient();
        if (client == null) return false;

        Player player = PlayersDatabase.getOrCreate(key);
        return player.tryAddToLoadingQueue(loading ->
                SEND_QUEUE.computeIfAbsent(client.socket(), socket -> new ConcurrentLinkedQueue<>()).offer(key), data);
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int getFree() {
        long time = System.currentTimeMillis();
        int count = 0;

        Iterator<AvailableCount> iterator = AVAILABLE_COUNT_MAP.values().iterator();
        while (iterator.hasNext()) {
            AvailableCount availableCount = iterator.next();
            int available = availableCount.count().get();
            if (available > 0 && availableCount.timeout > time && availableCount.socket().isOpen()) {
                count += available;
            } else {
                iterator.remove();
                Main.serverSend(availableCount.socket(), REQUEST_AVAILABLE_COUNT_PACKET);
            }
        }
        return count;
    }

    public static void setAvailable(@NotNull WebSocket socket, @Range(from = 0, to = Integer.MAX_VALUE) int available) {
        UUID player = socket.getAttachment();
        if (player == null) return;

        if (available == 0) {
            AVAILABLE_COUNT_MAP.remove(player);
        } else {
            AVAILABLE_COUNT_MAP.put(player, new AvailableCount(
                    System.currentTimeMillis() + AVAILABLE_TIMEOUT, socket, new AtomicInteger(available)));
        }
    }

    public static void sendRequest() {
        Iterator<Map.Entry<WebSocket, Queue<UUID>>> iterator = SEND_QUEUE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<WebSocket, Queue<UUID>> entry = iterator.next();
            iterator.remove();

            Main.serverSend(entry.getKey(), new RequestHypixelPlayerPacket(entry.getValue()));
        }
    }

    @Nullable
    private static AvailableCount takeClient() {
        int shift = Math.abs(TAKE_COUNT.incrementAndGet());

        while (true) {  // If thread collision, or timeout
            Map.Entry<UUID, AvailableCount>[] array = AVAILABLE_COUNT_MAP.entrySet().toArray(TO_ARRAY_ENTRY);
            if (array.length == 0) return null;

            Map.Entry<UUID, AvailableCount> selected = array[shift % array.length];

            if (selected.getValue().timeout < System.currentTimeMillis()) {
                AVAILABLE_COUNT_MAP.remove(selected.getKey());
                Main.serverSend(selected.getValue().socket(), REQUEST_AVAILABLE_COUNT_PACKET);
                continue;
            }

            int left = selected.getValue().count().decrementAndGet();
            if (left <= 0) {
                AVAILABLE_COUNT_MAP.remove(selected.getKey());
                Main.serverSend(selected.getValue().socket(), REQUEST_AVAILABLE_COUNT_PACKET);
            }

            if (left >= 0) return selected.getValue();
        }
    }

    private record AvailableCount(long timeout, @NotNull WebSocket socket, @NotNull AtomicInteger count) {
    }
}
