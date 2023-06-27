package com.jeka8833.tntserver.database.storage;

import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.function.Consumer;

public record HypixelPlayerLoading(@NotNull Queue<Consumer<HypixelPlayer>> listeners) implements HypixelPlayer {
}
