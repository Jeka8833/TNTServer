package com.jeka8833.tntserver.database;

import com.jeka8833.tntserver.Main;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class User {

    public final @NotNull UUID uuid;

    protected User(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    public abstract boolean isInactive();

    public abstract void disconnect();

    @Nullable
    public WebSocket getSocket() {
        for (WebSocket socket : Main.server.getConnections()) {
            if (uuid.equals(socket.getAttachment())) return socket;
        }

        return null;
    }
}
