package com.jeka8833.tntserver.user;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.user.player.Player;
import lombok.RequiredArgsConstructor;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class UserDatabase {
    private final TNTServer server;

    private final Cache<UUID, Player> playerCache = Caffeine.newBuilder()
            .weakValues()
            .build();

    public void updateAttachment(@NotNull UserBase user) {
        user.getSocket().setAttachment(user);

        if (user instanceof Player player) {
            playerCache.put(player.getUuid(), player);
        }
    }

    @Nullable
    @Contract(value = "null -> null", pure = true)
    public Player getPlayer(@Nullable UUID uuid) {
        if (uuid == null) return null;

        return playerCache.getIfPresent(uuid);
    }

    public Collection<Player> getAllPlayers() {
        return playerCache.asMap().values();
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public List<Bot> findAllBotsByPrivilege(@Nullable String privilege) {
        List<Bot> bots = new ArrayList<>();

        for (WebSocket webSocket : server.getConnections()) {
            UserBase user = webSocket.getAttachment();
            if (user instanceof Bot bot && bot.hasPrivilege(privilege)) {
                bots.add(bot);
            }
        }

        return bots;
    }
}
