package com.jeka8833.toprotocol.core.packet;

import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.NotNull;

public interface PacketBase<Attachment> {
    default void write(@NotNull OutputByteArray serializer, Attachment attachment) {
        write(serializer);
    }

    default void write(@NotNull OutputByteArray serializer) {
    }
}
