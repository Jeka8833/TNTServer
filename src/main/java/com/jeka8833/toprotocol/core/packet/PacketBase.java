package com.jeka8833.toprotocol.core.packet;

import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;

public interface PacketBase {
    default void write(PacketOutputSerializer serializer) {
    }
}
