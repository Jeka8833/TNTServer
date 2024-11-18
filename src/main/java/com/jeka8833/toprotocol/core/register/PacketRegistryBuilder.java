package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PacketRegistryBuilder<
        Key,
        ClientboundType extends PacketBase,
        ServerboundType extends PacketBase> {

    private final Map<Key, ConnectionBuilder<Key, ClientboundType, ServerboundType>> map = new ConcurrentHashMap<>();

    public PacketRegistryBuilder<Key, ClientboundType, ServerboundType> register(
            Key identifier,
            Consumer<ConnectionBuilder<Key, ClientboundType, ServerboundType>> builder) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(builder);

        ConnectionBuilder<Key, ClientboundType, ServerboundType> connectionBuilder =
                map.computeIfAbsent(identifier, k -> new ConnectionBuilder<>());
        builder.accept(connectionBuilder);

        if (connectionBuilder.getClientboundClazz() == null && connectionBuilder.getServerboundClazz() == null) {
            throw new IllegalStateException("Both serverbound and clientbound cannot be null");
        }

        return this;
    }

    public ClientPacketRegistry<Key, ClientboundType, ServerboundType> buildForClient() {
        return new ClientPacketRegistry<>(map);
    }

    public ServerPacketRegistry<Key, ClientboundType, ServerboundType> buildForServer() {
        return new ServerPacketRegistry<>(map);
    }

    public PacketRegistry<Key, ClientboundType, ServerboundType> build() {
        return new PacketRegistry<>(map);
    }
}
