package com.jeka8833.tntserver.old.database.storage;

import com.jeka8833.tntserver.old.TNTServer;
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
        for (WebSocket socket : TNTServer.server.getConnections()) {
            if (uuid.equals(socket.getAttachment())) return socket;
        }

        return null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;

        return uuid.equals(user.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
