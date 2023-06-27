package com.jeka8833.tntserver.packet;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface StreamSerializer {
    void readStream(@NotNull PacketInputStream stream) throws IOException;

    void writeStream(@NotNull PacketOutputStream stream) throws IOException;
}
