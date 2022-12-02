package com.Jeka8833.TNTServer.util;

import java.util.HashMap;
import java.util.Map;

public class BiMap<K, V> {

    private final Map<K, V> keyToValueMap = new HashMap<>();
    private final Map<V, K> valueToKeyMap = new HashMap<>();

    public final void put(K key, V value) {
        keyToValueMap.put(key, value);
        valueToKeyMap.put(value, key);
    }

    public final K getKey(V value) {
        return valueToKeyMap.get(value);
    }

    public final V get(K key) {
        return keyToValueMap.get(key);
    }
}
