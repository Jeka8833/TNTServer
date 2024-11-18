package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PacketRegistry<
        Key,
        ClientboundType extends PacketBase,
        ServerboundType extends PacketBase> {

    private final Map<Key, PairFunction<ClientboundType, ServerboundType>> registrations;
    private final Map<Class<? extends PacketBase>, Key> classToIdentifier;

    PacketRegistry(Map<Key, ConnectionBuilder<Key, ClientboundType, ServerboundType>> map) {
        Map<Key, PairFunction<ClientboundType, ServerboundType>> registrationsTemp = new HashMap<>(map.size());
        Map<Class<? extends PacketBase>, Key> classToIdentifierTemp = new HashMap<>(map.size());

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

    public ClientboundType createClientboundPacket(Key identifier, PacketInputSerializer serializer) {
        PairFunction<ClientboundType, ServerboundType> pairFunction = registrations.get(identifier);
        if (pairFunction == null || pairFunction.clientPacketFactory == null) return null;

        return pairFunction.clientPacketFactory.apply(serializer);
    }

    public ServerboundType createServerboundPacket(Key identifier, PacketInputSerializer serializer) {
        PairFunction<ClientboundType, ServerboundType> pairFunction = registrations.get(identifier);
        if (pairFunction == null || pairFunction.serverPacketFactory == null) return null;

        return pairFunction.serverPacketFactory.apply(serializer);
    }

    public Map<Class<? extends PacketBase>, Key> getIdentifiersMap() {
        return classToIdentifier;
    }

    private static final class PairFunction<
            ClientboundType extends PacketBase,
            ServerboundType extends PacketBase> {

        private final Function<PacketInputSerializer, ClientboundType> clientPacketFactory;
        private final Function<PacketInputSerializer, ServerboundType> serverPacketFactory;

        private PairFunction(Function<PacketInputSerializer, ClientboundType> clientPacketFactory,
                             Function<PacketInputSerializer, ServerboundType> serverPacketFactory) {
            this.clientPacketFactory = clientPacketFactory;
            this.serverPacketFactory = serverPacketFactory;
        }
    }
}
