package com.jeka8833.tntserver.user.player;

import com.alibaba.fastjson2.JSON;
import com.jeka8833.tntserver.user.UserBase;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Getter
@ToString(callSuper = true)
public final class Player extends UserBase {
    private final @NotNull UUID uuid;
    private final @NotNull String version;
    private final @NotNull GameServer gameServer;
    private final byte donate;

    @Setter
    private BitSet forceBlock = new BitSet(0);

    @Setter
    private BitSet forceActive = new BitSet(0);

    @Setter
    private BitSet activeModules = new BitSet(0);

    private @NotNull MinigameLocation playerLocation = MinigameLocation.EMPTY;

    @Setter
    private @NotNull PlayerPing playerPing = PlayerPing.EMPTY;

    @Setter
    private @NotNull OnlineStatus onlineStatus;

    @Setter
    private int fight;

    public Player(@NotNull WebSocket socket, int protocolVersion, @NotNull Set<String> privileges, @NotNull UUID uuid,
                  @NotNull GameServer gameServer, @NotNull String version, byte donate) {
        this(socket, protocolVersion, privileges, uuid, gameServer, version, donate, OnlineStatus.ONLINE);
    }

    public Player(@NotNull WebSocket socket, int protocolVersion, @NotNull Set<String> privileges, @NotNull UUID uuid,
                  @NotNull GameServer gameServer, @NotNull String version, byte donate,
                  @NotNull OnlineStatus onlineStatus) {
        super(socket, protocolVersion, privileges);

        this.uuid = uuid;
        this.gameServer = gameServer;
        this.version = version;
        this.donate = donate;
        this.onlineStatus = onlineStatus;
    }

    public boolean trySetMinigameLocation(@NotNull String jsonPlayerLocation) {
        try {
            MinigameLocation minigameLocation = JSON.parseObject(jsonPlayerLocation, MinigameLocation.class);
            if (minigameLocation == null) return false;

            this.playerLocation = minigameLocation;

            return true;
        } catch (Exception e) {
            log.warn("Fail parse minigame location: {}", jsonPlayerLocation, e);
        }

        return false;
    }
}
