package com.jeka8833.tntserver.requester.storage;

import com.jeka8833.tntserver.requester.balancer.BalancerRefresh;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CacheValue implements Serializable, BalancerRefresh {
    @Serial
    private static final long serialVersionUID = -4596793276874632260L;

    @Getter
    private Map<UUID, Long> requestTimeMap = new ConcurrentHashMap<>();

    @Nullable
    private String gameInfo;

    @Getter
    @NotNull
    private HypixelCompactStructure compactStructure = HypixelCompactStructure.EMPTY_INSTANCE;

    public void update(@NotNull HypixelCompactStructure response, @Nullable String gameInfo) {
        this.compactStructure = response;
        this.gameInfo = gameInfo;
    }

    public boolean isGameInfoDifferent(String gameInfo) {
        return !Objects.equals(this.gameInfo, gameInfo);
    }

    @Override
    public int getMaxWins() {
        return Math.max(compactStructure.tntRunWins(), Math.max(compactStructure.pvpRunWins(),
                Math.max(compactStructure.bowSpleefWins(), Math.max(compactStructure.tntTagWins(),
                        Math.max(compactStructure.wizardsWins(), compactStructure.bowSpleefDuelsWins())))));
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(compactStructure);
        out.writeObject(gameInfo);

        HashMap<UUID, Long> map = new HashMap<>(requestTimeMap.size());
        for (Map.Entry<UUID, Long> entry : requestTimeMap.entrySet()) {
            map.put(entry.getKey(), System.currentTimeMillis() -
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - entry.getValue()));
        }

        out.writeObject(map);
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        compactStructure = (HypixelCompactStructure) in.readObject();
        gameInfo = (String) in.readObject();

        //noinspection unchecked
        Map<UUID, Long> map = (HashMap<UUID, Long>) in.readObject();
        requestTimeMap = new ConcurrentHashMap<>(map.size());

        for (Map.Entry<UUID, Long> entry : map.entrySet()) {
            requestTimeMap.put(entry.getKey(), System.nanoTime() -
                    TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis() - entry.getValue()));
        }
    }
}
