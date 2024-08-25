package com.jeka8833.tntserver.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BiMap<K, V> {
    private final Map<K, V> keyToValueMap = new HashMap<>();
    private final Map<V, K> valueToKeyMap = new HashMap<>();

    public final void put(@NotNull K key, @NotNull V value) {
        keyToValueMap.put(key, value);
        valueToKeyMap.put(value, key);
    }

    @Nullable
    public final K getKey(V value) {
        return valueToKeyMap.get(value);
    }

    @Nullable
    public final V getValue(K key) {
        return keyToValueMap.get(key);
    }
}
