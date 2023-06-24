package com.Jeka8833.TNTServer.database.storage;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;

public record HypixelPlayerLoading(@NotNull Collection<Consumer<HypixelPlayer>> listeners) implements HypixelPlayer {
}
