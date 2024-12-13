package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class PacketRegistry<Key, ClientboundType extends PacketBase<Attachment>,
        ServerboundType extends PacketBase<Attachment>, Attachment> implements
        ClientBoundRegistry<Key, ClientboundType, ServerboundType, Attachment>,
        ServerBoundRegistry<Key, ClientboundType, ServerboundType, Attachment> {

    private final Map<Key, PairFunction<ClientboundType, ServerboundType, Attachment>> registrations;
    private final Map<Class<? extends PacketBase<Attachment>>, Key> classToIdentifier;

    PacketRegistry(Map<Key, ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment>> map) {
        Map<Key, PairFunction<ClientboundType, ServerboundType, Attachment>> registrationsTemp =
                new HashMap<>(map.size());
        Map<Class<? extends PacketBase<Attachment>>, Key> classToIdentifierTemp = new HashMap<>(map.size());

        map.forEach((key, builder) -> {
            if (builder.getClientPacketFactory() != null || builder.getServerPacketFactory() != null) {
                registrationsTemp.put(key,
                        new PairFunction<>(builder.getClientPacketFactory(), builder.getServerPacketFactory()));
            }

            if (builder.getClientboundClazz() != null) {
                classToIdentifierTemp.put(builder.getClientboundClazz(), key);
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
            @NotNull Key identifier, @NotNull PacketInputSerializer serializer, Attachment attachment) {
        PairFunction<ClientboundType, ServerboundType, Attachment> pairFunction = registrations.get(identifier);
        if (pairFunction == null || pairFunction.clientPacketFactory == null) return null;

        return pairFunction.clientPacketFactory.apply(serializer, attachment);
    }

    @Nullable
    @Override
    public Key getServerBoundPacketKey(@NotNull Class<? extends ServerboundType> packetClass) {
        return classToIdentifier.get(packetClass);
    }

    @Nullable
    @Override
    public ServerboundType createServerBoundPacket(
            @NotNull Key identifier, @NotNull PacketInputSerializer serializer, Attachment attachment) {
        PairFunction<ClientboundType, ServerboundType, Attachment> pairFunction = registrations.get(identifier);
        if (pairFunction == null || pairFunction.serverPacketFactory == null) return null;

        return pairFunction.serverPacketFactory.apply(serializer, attachment);
    }

    @Nullable
    @Override
    public Key getClientBoundPacketKey(@NotNull Class<? extends ClientboundType> packetClass) {
        return classToIdentifier.get(packetClass);
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static final class PairFunction<ClientboundType extends PacketBase<Attachment>,
            ServerboundType extends PacketBase<Attachment>, Attachment> {

        @Nullable
        private final BiFunction<PacketInputSerializer, Attachment, ClientboundType> clientPacketFactory;

        @Nullable
        private final BiFunction<PacketInputSerializer, Attachment, ServerboundType> serverPacketFactory;

        private PairFunction(
                @Nullable BiFunction<PacketInputSerializer, Attachment, ClientboundType> clientPacketFactory,
                @Nullable BiFunction<PacketInputSerializer, Attachment, ServerboundType> serverPacketFactory) {
            this.clientPacketFactory = clientPacketFactory;
            this.serverPacketFactory = serverPacketFactory;
        }
    }
}
