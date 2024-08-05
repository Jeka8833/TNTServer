package com.jeka8833.tntserver.requester.ratelimiter;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ResetListener {

    void call(@NotNull ResetManager manager) throws InterruptedException;

}
