package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class ServerPacketRegistry<Key, ClientboundType extends PacketBase<Attachment>,
        ServerboundType extends PacketBase<Attachment>, Attachment> implements
        ServerBoundRegistry<Key, ClientboundType, ServerboundType, Attachment> {

    private final Map<Key, BiFunction<PacketInputSerializer, Attachment, ServerboundType>> registrations;
    private final Map<Class<? extends ClientboundType>, Key> classToIdentifier;

    ServerPacketRegistry(Map<Key, ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment>> map) {
        Map<Key, BiFunction<PacketInputSerializer, Attachment, ServerboundType>> registrationsTemp =
                new HashMap<>(map.size());
        Map<Class<? extends ClientboundType>, Key> classToIdentifierTemp = new HashMap<>(map.size());

        map.forEach((key, builder) -> {
            if (builder.getServerPacketFactory() != null) {
                registrationsTemp.put(key, builder.getServerPacketFactory());
            }
            if (builder.getClientboundClazz() != null) {
                classToIdentifierTemp.put(builder.getClientboundClazz(), key);
            }
        });

        registrations = new HashMap<>(registrationsTemp);
        classToIdentifier = new HashMap<>(classToIdentifierTemp);
    }

    @Nullable
    @Override
    public ServerboundType createServerBoundPacket(
            @NotNull Key identifier, @NotNull PacketInputSerializer serializer, Attachment attachment) {
        BiFunction<PacketInputSerializer, Attachment, ServerboundType> registration = registrations.get(identifier);
        if (registration == null) return null;

        return registration.apply(serializer, attachment);
    }

    @Nullable
    @Override
    public Key getClientBoundPacketKey(@NotNull Class<? extends ClientboundType> packetClass) {
        return classToIdentifier.get(packetClass);
    }
}
