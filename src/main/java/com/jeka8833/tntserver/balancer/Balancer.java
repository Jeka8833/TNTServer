package com.jeka8833.tntserver.balancer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.function.Consumer;

public interface Balancer<K, D> {

    boolean requestInfo(@NotNull K key, @NotNull Consumer<D> data);

    @Range(from = 0, to = Integer.MAX_VALUE)
    int getFree();

}
