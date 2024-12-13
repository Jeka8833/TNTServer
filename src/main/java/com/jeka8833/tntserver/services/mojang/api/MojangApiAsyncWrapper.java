package com.jeka8833.tntserver.services.mojang.api;

import com.jeka8833.tntserver.services.mojang.MojangProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
class MojangApiAsyncWrapper {
    private final MojangApiRequester apiRequester;

    @Async("virtual-executor")
    CompletableFuture<MojangProfile> getProfileAsync(String playerName) {
        return CompletableFuture.completedFuture(apiRequester.getProfile(playerName));
    }

    @Async("virtual-executor")
    CompletableFuture<MojangProfile> getProfileAsync(UUID playerUUID) {
        return CompletableFuture.completedFuture(apiRequester.getProfile(playerUUID));
    }
}
