package com.jeka8833.tntserver.balancer;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.database.storage.HypixelPlayer;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.packets.RequestHypixelPlayerPacket;
import com.jeka8833.tntserver.packet.packets.UpdateFreeRequestsPacket;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class HypixelTNTRequest implements Balancer<UUID, HypixelPlayer> {

    private static final long AVAILABLE_TIMEOUT = TimeUnit.MINUTES.toNanos(1);

    @SuppressWarnings("unchecked")
    private static final Map.Entry<UUID, AvailableCount>[] TO_ARRAY_ENTRY =
            (Map.Entry<UUID, AvailableCount>[]) new Map.Entry[0];
    private static final Packet REQUEST_AVAILABLE_COUNT_PACKET = new UpdateFreeRequestsPacket();
    private static final Map<WebSocket, Queue<UUID>> SEND_QUEUE = new ConcurrentHashMap<>();

    private final Map<UUID, AvailableCount> availableCountMap = new ConcurrentHashMap<>();
    private final AtomicInteger takeCount = new AtomicInteger();

    @Override
    public boolean requestInfo(@NotNull UUID key, @NotNull Consumer<HypixelPlayer> data) {
        AvailableCount client = takeClient();
        if (client == null) return false;

        User user = PlayersDatabase.getOrCreate(key);
        if (user instanceof Player player) {
            return player.tryAddToLoadingQueue(loading -> SEND_QUEUE.computeIfAbsent(client.socket(),
                    socket -> new ConcurrentLinkedQueue<>()).offer(key), data);
        }
        return false;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int getFree() {
        int count = 0;

        Iterator<AvailableCount> iterator = availableCountMap.values().iterator();
        while (iterator.hasNext()) {
            AvailableCount availableCount = iterator.next();
            int available = availableCount.count().get();
            if (available > 0 && System.nanoTime() - availableCount.timeout < AVAILABLE_TIMEOUT &&
                    availableCount.socket().isOpen()) {
                count += available;
            } else {
                iterator.remove();
                Main.serverSend(availableCount.socket(), REQUEST_AVAILABLE_COUNT_PACKET);
            }
        }
        return count;
    }

    public void setAvailable(@NotNull WebSocket socket, @Range(from = 0, to = Integer.MAX_VALUE) int available) {
        UUID player = socket.getAttachment();
        if (player == null) return;

        if (available == 0) {
            availableCountMap.remove(player);
        } else {
            availableCountMap.put(player, new AvailableCount(System.nanoTime(), socket, new AtomicInteger(available)));
        }
    }

    @Nullable
    private AvailableCount takeClient() {
        int shift = takeCount.incrementAndGet();

        while (true) {  // If thread collision, or timeout
            Map.Entry<UUID, AvailableCount>[] array = availableCountMap.entrySet().toArray(TO_ARRAY_ENTRY);
            if (array.length == 0) return null;

            Map.Entry<UUID, AvailableCount> selected = array[Math.floorMod(shift, array.length)];

            if (System.nanoTime() - selected.getValue().timeout > AVAILABLE_TIMEOUT) {
                availableCountMap.remove(selected.getKey());
                Main.serverSend(selected.getValue().socket(), REQUEST_AVAILABLE_COUNT_PACKET);
                continue;
            }

            int left = selected.getValue().count().decrementAndGet();
            if (left <= 0) {
                availableCountMap.remove(selected.getKey());
                Main.serverSend(selected.getValue().socket(), REQUEST_AVAILABLE_COUNT_PACKET);
            }

            if (left >= 0) return selected.getValue();
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

    private record AvailableCount(long timeout, @NotNull WebSocket socket, @NotNull AtomicInteger count) {
    }
}
