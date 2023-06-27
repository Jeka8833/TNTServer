package com.jeka8833.tntserver.packet;

import com.jeka8833.tntserver.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PacketInputStream extends DataInputStream {
    private static final Logger logger = LogManager.getLogger(PacketInputStream.class);

    public final Packet packet;

    public PacketInputStream(final ByteBuffer buffer) throws Exception {
        super(new ByteArrayInputStream(buffer.array()));
        if (buffer.limit() > 2 * 1024) // 2 KB
            throw new RuntimeException("Very big data");

        byte ID = readByte();
        Class<? extends Packet> class_ = Main.packetsList.get(ID);
        if (class_ == null) throw new NullPointerException("The received packet has an unknown ID: " + ID);

        packet = class_.getDeclaredConstructor().newInstance();
        logger.debug("Packet ready to be read: " + packet);
    }

    public final UUID readUUID() throws IOException {
        return new UUID(readLong(), readLong());
    }
}
