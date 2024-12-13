package com.jeka8833.tntserver.services.tntclient.user;

import com.jeka8833.tntserver.services.tntclient.TNTServerWebSocket;
import lombok.Getter;
import lombok.ToString;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

@Getter
@ToString(callSuper = true)
public final class Bot extends UserBase {
    private final @NotNull UUID uuid;

    public Bot(@NotNull TNTServerWebSocket tntServerWebSocket, @NotNull WebSocket socket, int protocolVersion,
               @NotNull Set<String> privileges, @NotNull UUID uuid) {
        super(tntServerWebSocket, socket, protocolVersion, privileges);

        this.uuid = uuid;
    }
}
