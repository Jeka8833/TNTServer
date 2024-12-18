package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ServerBoundRegistry<Key, ClientboundType extends PacketBase<Attachment>,
        ServerboundType extends PacketBase<Attachment>, Attachment> {

    @Nullable
    default ServerboundType createServerBoundPacket(@NotNull Key identifier, @NotNull InputByteArray serializer) {
        return createServerBoundPacket(identifier, serializer, null);
    }

    @Nullable
    ServerboundType createServerBoundPacket(
            @NotNull Key identifier, @NotNull InputByteArray serializer, Attachment attachment);

    @Nullable
    Key getClientBoundPacketKey(@NotNull Class<? extends ClientboundType> packetClass);
}
