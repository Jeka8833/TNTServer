package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;

import java.util.Objects;
import java.util.function.Function;

public class ConnectionBuilder<
        Key,
        ClientboundType extends PacketBase,
        ServerboundType extends PacketBase> {

    protected Class<? extends ClientboundType> clientboundClazz;
    protected Function<PacketInputSerializer, ClientboundType> clientPacketFactory;
    protected Class<? extends ServerboundType> serverboundClazz;
    protected Function<PacketInputSerializer, ServerboundType> serverPacketFactory;

    public ConnectionBuilder<Key, ClientboundType, ServerboundType> clientbound(
            Class<? extends ClientboundType> clientboundClazz,
            Function<PacketInputSerializer, ClientboundType> clientPacketFactory) {
        this.clientboundClazz = Objects.requireNonNull(clientboundClazz);
        this.clientPacketFactory = Objects.requireNonNull(clientPacketFactory);

        return this;
    }

    public ConnectionBuilder<Key, ClientboundType, ServerboundType> clientbound(
            Class<? extends ClientboundType> clientboundClazz) {
        Class<? extends ClientboundType> aClass = Objects.requireNonNull(clientboundClazz);
        if (this.clientboundClazz != null && !aClass.equals(this.clientboundClazz)) {
            throw new IllegalArgumentException("Clientbound class is already set");
        }

        this.clientboundClazz = aClass;

        return this;
    }

    public ConnectionBuilder<Key, ClientboundType, ServerboundType> serverbound(
            Class<? extends ServerboundType> serverboundClazz,
            Function<PacketInputSerializer, ServerboundType> serverPacketFactory) {
        this.serverboundClazz = Objects.requireNonNull(serverboundClazz);
        this.serverPacketFactory = Objects.requireNonNull(serverPacketFactory);

        return this;
    }

    public ConnectionBuilder<Key, ClientboundType, ServerboundType> serverbound(
            Class<? extends ServerboundType> serverboundClazz) {
        Class<? extends ServerboundType> aClass = Objects.requireNonNull(serverboundClazz);
        if (this.serverboundClazz != null && !aClass.equals(this.serverboundClazz)) {
            throw new IllegalArgumentException("Serverbound class is already set");
        }

        this.serverboundClazz = aClass;

        return this;
    }

    public Class<? extends ClientboundType> getClientboundClazz() {
        return clientboundClazz;
    }

    public Function<PacketInputSerializer, ClientboundType> getClientPacketFactory() {
        return clientPacketFactory;
    }

    public Class<? extends ServerboundType> getServerboundClazz() {
        return serverboundClazz;
    }

    public Function<PacketInputSerializer, ServerboundType> getServerPacketFactory() {
        return serverPacketFactory;
    }
}
