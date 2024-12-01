package com.jeka8833.toprotocol.core.packet;

import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;
import org.jetbrains.annotations.NotNull;

public interface PacketBase {
    default void write(@NotNull PacketOutputSerializer serializer, int protocolVersion) {
    }
}
