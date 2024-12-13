package com.jeka8833.tntserver.services.tntclient.user.player;

import com.jeka8833.tntserver.services.StaticJSON;
import com.jeka8833.tntserver.services.mojang.MojangProfile;
import com.jeka8833.tntserver.services.tntclient.TNTServerWebSocket;
import com.jeka8833.tntserver.services.tntclient.user.UserBase;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.Set;

@Slf4j
@Getter
@ToString(callSuper = true)
public final class Player extends UserBase {
    private final @NotNull MojangProfile mojangProfile;
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

    public Player(@NotNull TNTServerWebSocket tntServerWebSocket, @NotNull WebSocket socket, int protocolVersion,
                  @NotNull Set<String> privileges, @NotNull MojangProfile mojangProfile,
                  @NotNull GameServer gameServer, @NotNull String version, byte donate) {
        this(tntServerWebSocket, socket, protocolVersion, privileges, mojangProfile, gameServer, version, donate,
                OnlineStatus.ONLINE);
    }

    public Player(@NotNull TNTServerWebSocket tntServerWebSocket, @NotNull WebSocket socket, int protocolVersion,
                  @NotNull Set<String> privileges, @NotNull MojangProfile mojangProfile, @NotNull GameServer gameServer,
                  @NotNull String version, byte donate, @NotNull OnlineStatus onlineStatus) {
        super(tntServerWebSocket, socket, protocolVersion, privileges);

        this.mojangProfile = mojangProfile;
        this.gameServer = gameServer;
        this.version = version;
        this.donate = donate;
        this.onlineStatus = onlineStatus;
    }

    public boolean trySetMinigameLocation(@NotNull String jsonPlayerLocation) {
        try {
            MinigameLocation minigameLocation = StaticJSON.getObjectMapper()
                    .readValue(jsonPlayerLocation, MinigameLocation.class);
            if (minigameLocation == null) return false;

            this.playerLocation = minigameLocation;

            return true;
        } catch (Exception e) {
            log.warn("Fail parse minigame location: {}", jsonPlayerLocation, e);
        }

        return false;
    }
}
