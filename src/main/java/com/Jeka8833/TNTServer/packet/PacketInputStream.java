package com.Jeka8833.TNTServer.packet;

import com.Jeka8833.TNTServer.Main;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PacketInputStream extends DataInputStream {

    public final Packet packet;

    public PacketInputStream(final ByteBuffer buffer) throws Exception {
        super(new ByteArrayInputStream(buffer.array()));
        if (buffer.limit() > 2 * 1024) // 2 KB
            throw new RuntimeException("Very big data");

        packet = Main.packetsList.get(readByte()).getDeclaredConstructor().newInstance();
        //System.out.println("In: " + packet);
    }

    public final UUID readUUID() throws IOException {
        return new UUID(readLong(), readLong());
    }
}
