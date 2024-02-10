package com.jeka8833.tntserver.balancer;

import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.database.storage.HypixelPlayer;
import com.jeka8833.tntserver.database.storage.HypixelPlayerError;
import com.jeka8833.tntserver.database.storage.HypixelPlayerStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class HypixelCache implements Balancer<UUID, HypixelPlayer> {

    public static final long INVALIDATE_CACHE_AFTER = TimeUnit.MINUTES.toMillis(2);


    @Override
    public boolean requestInfo(@NotNull UUID key, @NotNull Consumer<HypixelPlayer> data) {
        User user = PlayersDatabase.getUser(key);
        if (user instanceof Player player) {
            if (player.hypixelPlayerInfo instanceof HypixelPlayerError ||
                    (player.hypixelPlayerInfo instanceof HypixelPlayerStorage storage &&
                            System.nanoTime() - storage.createdTime < INVALIDATE_CACHE_AFTER)) {
                data.accept(player.hypixelPlayerInfo);
                return true;
            }

            // The information is sent anyway, but if the data is out of date, a more recent version is requested.
            if (player.hypixelPlayerInfo instanceof HypixelPlayerStorage storage) {
                data.accept(storage);
            }
        }
        return false;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int getFree() {
        return Integer.MAX_VALUE;
    }
}
