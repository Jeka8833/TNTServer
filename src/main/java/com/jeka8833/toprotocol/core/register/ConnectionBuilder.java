package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class ConnectionBuilder<Key, ClientboundType extends PacketBase<Attachment>,
        ServerboundType extends PacketBase<Attachment>, Attachment> {

    private Class<? extends ClientboundType> clientboundClazz;
    private BiFunction<InputByteArray, Attachment, ClientboundType> clientPacketFactory;
    private Class<? extends ServerboundType> serverboundClazz;
    private BiFunction<InputByteArray, Attachment, ServerboundType> serverPacketFactory;

    @NotNull
    @Contract(value = "_, _ -> this")
    public ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment> clientbound(
            @NotNull Class<? extends ClientboundType> clientboundClazz,
            @NotNull Function<InputByteArray, ClientboundType> clientPacketFactory) {
        return clientbound(clientboundClazz, (inputByteArray, attachment) ->
                clientPacketFactory.apply(inputByteArray));
    }

    @NotNull
    @Contract(value = "_, _ -> this")
    public ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment> clientbound(
            @NotNull Class<? extends ClientboundType> clientboundClazz,
            @NotNull BiFunction<InputByteArray, Attachment, ClientboundType> clientPacketFactory) {
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
            @NotNull Function<InputByteArray, ServerboundType> serverPacketFactory) {
        return serverbound(serverboundClazz, (inputByteArray, attachment) ->
                serverPacketFactory.apply(inputByteArray));
    }

    @NotNull
    @Contract(value = "_, _ -> this")
    public ConnectionBuilder<Key, ClientboundType, ServerboundType, Attachment> serverbound(
            @NotNull Class<? extends ServerboundType> serverboundClazz,
            @NotNull BiFunction<InputByteArray, Attachment, ServerboundType> serverPacketFactory) {
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

    BiFunction<InputByteArray, Attachment, ClientboundType> getClientPacketFactory() {
        return clientPacketFactory;
    }

    Class<? extends ServerboundType> getServerboundClazz() {
        return serverboundClazz;
    }

    BiFunction<InputByteArray, Attachment, ServerboundType> getServerPacketFactory() {
        return serverPacketFactory;
    }
}
