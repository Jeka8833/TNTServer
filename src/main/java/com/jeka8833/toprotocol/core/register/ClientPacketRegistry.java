package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class ClientPacketRegistry<Key, ClientboundType extends PacketBase<Attachment>,
        ServerboundType extends PacketBase<Attachment>, Attachment> implements
        ClientBoundRegistry<Key, ClientboundType, ServerboundType, Attachment> {

    private final Map<Key, BiFunction<InputByteArray, Attachment, ClientboundType>> registrations;
    private final Map<Class<? extends ServerboundType>, Key> classToIdentifier;

    ClientPacketRegistry(Map<Key, ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment>> map) {
        Map<Key, BiFunction<InputByteArray, Attachment, ClientboundType>> registrationsTemp =
                new HashMap<>(map.size());
        Map<Class<? extends ServerboundType>, Key> classToIdentifierTemp = new HashMap<>(map.size());

        map.forEach((key, builder) -> {
            if (builder.getClientPacketFactory() != null) {
                registrationsTemp.put(key, builder.getClientPacketFactory());
            }
            if (builder.getServerboundClazz() != null) {
                classToIdentifierTemp.put(builder.getServerboundClazz(), key);
            }
        });

        registrations = new HashMap<>(registrationsTemp);
        classToIdentifier = new HashMap<>(classToIdentifierTemp);
    }

    @Nullable
    @Override
    public ClientboundType createClientBoundPacket(
            @NotNull Key identifier, @NotNull InputByteArray serializer, Attachment attachment) {
        BiFunction<InputByteArray, Attachment, ClientboundType> registration = registrations.get(identifier);
        if (registration == null) return null;

        return registration.apply(serializer, attachment);
    }

    @Nullable
    @Override
    public Key getServerBoundPacketKey(@NotNull Class<? extends ServerboundType> packetClass) {
        return classToIdentifier.get(packetClass);
    }
}
