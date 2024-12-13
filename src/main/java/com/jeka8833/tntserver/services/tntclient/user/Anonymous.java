package com.jeka8833.tntserver.services.tntclient.user;

import com.jeka8833.tntserver.services.tntclient.TNTServerWebSocket;
import lombok.ToString;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ToString(callSuper = true)
public final class Anonymous extends UserBase {
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE =
            Executors.newSingleThreadScheduledExecutor();

    public Anonymous(@NotNull TNTServerWebSocket tntServerWebSocket, @NotNull WebSocket socket, int protocolVersion,
                     long disconnectAfter, @NotNull TimeUnit unit) {
        super(tntServerWebSocket, socket, protocolVersion, Collections.emptySet());

        if (disconnectAfter < 0) throw new IllegalArgumentException("disconnectAfter must be >= 0");
        if (disconnectAfter == 0) return;

        SCHEDULED_EXECUTOR_SERVICE.schedule(() -> {
            WebSocket socket_ = this.getUserWebsocketChannel();

            if (this.equals(socket_.getAttachment())) {
                socket_.close();
            }
        }, disconnectAfter, unit);
    }

    @Override
    public boolean equals(Object object) {
        return this == object;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
