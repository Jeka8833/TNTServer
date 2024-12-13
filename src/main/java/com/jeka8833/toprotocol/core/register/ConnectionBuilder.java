package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public final class ConnectionBuilder<Key, ClientboundType extends PacketBase<Attachment>,
        ServerboundType extends PacketBase<Attachment>, Attachment> {

    private Class<? extends ClientboundType> clientboundClazz;
    private BiFunction<PacketInputSerializer, Attachment, ClientboundType> clientPacketFactory;
    private Class<? extends ServerboundType> serverboundClazz;
    private BiFunction<PacketInputSerializer, Attachment, ServerboundType> serverPacketFactory;

    @NotNull
    @Contract(value = "_, _ -> this")
    public ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment> clientbound(
            @NotNull Class<? extends ClientboundType> clientboundClazz,
            @NotNull BiFunction<PacketInputSerializer, Attachment, ClientboundType> clientPacketFactory) {
        this.clientboundClazz = clientboundClazz;
        this.clientPacketFactory = clientPacketFactory;

        return this;
    }

    @NotNull
    @Contract(value = "_ -> this")
    public ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment> clientbound(
            @NotNull Class<? extends ClientboundType> clientboundClazz) {
        if (this.clientboundClazz != null && !clientboundClazz.equals(this.clientboundClazz)) {
            throw new IllegalArgumentException("Clientbound class is already set");
        }

        this.clientboundClazz = clientboundClazz;

        return this;
    }

    @NotNull
    @Contract(value = "_, _ -> this")
    public ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment> serverbound(
            @NotNull Class<? extends ServerboundType> serverboundClazz,
            @NotNull BiFunction<PacketInputSerializer, Attachment, ServerboundType> serverPacketFactory) {
        this.serverboundClazz = serverboundClazz;
        this.serverPacketFactory = serverPacketFactory;

        return this;
    }

    @NotNull
    @Contract(value = "_ -> this")
    public ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment> serverbound(
            @NotNull Class<? extends ServerboundType> serverboundClazz) {
        if (this.serverboundClazz != null && !serverboundClazz.equals(this.serverboundClazz)) {
            throw new IllegalArgumentException("Serverbound class is already set");
        }

        this.serverboundClazz = serverboundClazz;

        return this;
    }

    Class<? extends ClientboundType> getClientboundClazz() {
        return clientboundClazz;
    }

    BiFunction<PacketInputSerializer, Attachment, ClientboundType> getClientPacketFactory() {
        return clientPacketFactory;
    }

    Class<? extends ServerboundType> getServerboundClazz() {
        return serverboundClazz;
    }

    BiFunction<PacketInputSerializer, Attachment, ServerboundType> getServerPacketFactory() {
        return serverPacketFactory;
    }
}
