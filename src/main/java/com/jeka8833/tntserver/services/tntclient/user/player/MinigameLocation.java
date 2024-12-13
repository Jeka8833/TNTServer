package com.jeka8833.tntserver.services.tntclient.user.player;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jeka8833.tntserver.services.StaticJSON;
import org.jetbrains.annotations.Nullable;

public record MinigameLocation(
        @Nullable String serverName,
        @Nullable String lobbyName,
        @Nullable String mode,
        @Nullable String map) {
    public static final MinigameLocation EMPTY =
            new MinigameLocation(null, null, null, null);

    public String getJsonValue() throws JsonProcessingException {
        return StaticJSON.getObjectMapper().writeValueAsString(this);
    }
}
