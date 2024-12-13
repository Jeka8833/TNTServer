package com.jeka8833.toprotocol.core.register;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ClientBoundRegistry<Key, ClientboundType extends PacketBase<Attachment>,
        ServerboundType extends PacketBase<Attachment>, Attachment> {

    @Nullable
    ClientboundType createClientBoundPacket(
            @NotNull Key identifier, @NotNull PacketInputSerializer serializer, Attachment attachment);

    @Nullable
    Key getServerBoundPacketKey(@NotNull Class<? extends ServerboundType> packetClass);
}
