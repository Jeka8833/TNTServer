package com.jeka8833.tntserver.services.tntclient;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketStarter {
    private final TNTServerWebSocket tntServerWebSocket;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady(@NotNull ApplicationReadyEvent ignoredEvent) {
        tntServerWebSocket.setConnectionLostTimeout(15);
        tntServerWebSocket.setReuseAddr(true);
        tntServerWebSocket.start();
    }

    @EventListener(ContextClosedEvent.class)
    public void onClose(@NotNull ContextClosedEvent ignoredEvent) throws InterruptedException {
        tntServerWebSocket.stop();
    }
}
