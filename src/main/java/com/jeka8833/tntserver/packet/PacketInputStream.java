package com.jeka8833.tntserver.packet;

import com.jeka8833.tntserver.TNTServer;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

@Slf4j
public class PacketInputStream extends DataInputStream {
    public final Packet packet;

    public PacketInputStream(final ByteBuffer buffer) throws Exception {
        super(new ByteArrayInputStream(buffer.array()));
        if (buffer.limit() > 2 * 1024) // 2 KB
            throw new RuntimeException("Very big data");

        byte ID = readByte();
        Class<? extends Packet> class_ = TNTServer.PACKETS_LIST.getValue(ID);
        if (class_ == null) throw new NullPointerException("The received packet has an unknown ID: " + ID);

        packet = class_.getDeclaredConstructor().newInstance();
        log.debug("Packet ready to be read: {}", packet.getClass().getSimpleName());
    }

    public final UUID readUUID() throws IOException {
        return new UUID(readLong(), readLong());
    }
}
