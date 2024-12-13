package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PacketRegistryBuilder<Key, ClientboundType extends PacketBase<Attachment>,
        ServerboundType extends PacketBase<Attachment>, Attachment> {

    private final Map<Key, ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment>> map =
            new ConcurrentHashMap<>();

    @NotNull
    @Contract(value = "_, _ -> this")
    public PacketRegistryBuilder<Key, ClientboundType, ServerboundType, Attachment> register(
            @NotNull Key identifier,
            @NotNull Consumer<ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment>> builder) {
        ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment> connectionBuilder =
                map.computeIfAbsent(identifier, k -> new ConnectionBuilder<>());
        builder.accept(connectionBuilder);

        if (connectionBuilder.getClientboundClazz() == null && connectionBuilder.getServerboundClazz() == null) {
            throw new IllegalStateException("Both serverbound and clientbound cannot be null");
        }

        return this;
    }

    @NotNull
    public Set<Key> getRegisteredKeys() {
        return map.keySet();
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    public ClientPacketRegistry<Key, ClientboundType, ServerboundType, Attachment> buildForClient() {
        return new ClientPacketRegistry<>(map);
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    public ServerPacketRegistry<Key, ClientboundType, ServerboundType, Attachment> buildForServer() {
        return new ServerPacketRegistry<>(map);
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    public PacketRegistry<Key, ClientboundType, ServerboundType, Attachment> build() {
        return new PacketRegistry<>(map);
    }
}
