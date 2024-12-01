package com.jeka8833.tntserver.user.player;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import org.jetbrains.annotations.Nullable;

public record MinigameLocation(
        @Nullable String serverName,
        @Nullable String lobbyName,
        @Nullable String mode,
        @Nullable String map) {
    public static final MinigameLocation EMPTY =
            new MinigameLocation(null, null, null, null);

    @JSONField(serialize = false, deserialize = false)
    public String getJsonValue() {
        return JSON.toJSONString(this);
    }
}
