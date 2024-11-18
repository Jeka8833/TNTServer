package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ServerPacketRegistry<
        Key,
        ClientboundType extends PacketBase,
        ServerboundType extends PacketBase> {

    private final Map<Key, Function<PacketInputSerializer, ServerboundType>> registrations;
    private final Map<Class<? extends ClientboundType>, Key> classToIdentifier;

    ServerPacketRegistry(Map<Key, ConnectionBuilder<Key, ClientboundType, ServerboundType>> map) {
        Map<Key, Function<PacketInputSerializer, ServerboundType>> registrationsTemp = new HashMap<>(map.size());
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

    public ServerboundType createPacket(Key identifier, PacketInputSerializer serializer) {
        Function<PacketInputSerializer, ServerboundType> registration = registrations.get(identifier);
        if (registration == null) return null;

        return registration.apply(serializer);
    }

    public Map<Class<? extends ClientboundType>, Key> getIdentifiersMap() {
        return classToIdentifier;
    }
}
