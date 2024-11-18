package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ClientPacketRegistry<
        Key,
        ClientboundType extends PacketBase,
        ServerboundType extends PacketBase> {

    private final Map<Key, Function<PacketInputSerializer, ClientboundType>> registrations;
    private final Map<Class<? extends ServerboundType>, Key> classToIdentifier;

    ClientPacketRegistry(Map<Key, ConnectionBuilder<Key, ClientboundType, ServerboundType>> map) {
        Map<Key, Function<PacketInputSerializer, ClientboundType>> registrationsTemp = new HashMap<>(map.size());
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

    public ClientboundType createPacket(Key identifier, PacketInputSerializer serializer) {
        Function<PacketInputSerializer, ClientboundType> registration = registrations.get(identifier);
        if (registration == null) return null;

        return registration.apply(serializer);
    }

    public Map<Class<? extends ServerboundType>, Key> getIdentifiersMap() {
        return classToIdentifier;
    }
}
