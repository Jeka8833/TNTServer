package com.jeka8833.tntserver.packet;

import com.jeka8833.tntserver.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PacketOutputStream extends DataOutputStream {

    private static final Logger logger = LogManager.getLogger(PacketOutputStream.class);

    /**
     * Creates a new data output stream to write data to the specified
     * underlying output stream. The counter {@code written} is
     * set to zero.
     *
     * @see FilterOutputStream#out
     */
    public PacketOutputStream() {
        super(new ByteArrayOutputStream());
    }

    public void writeUUID(final UUID uuid) throws IOException {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
    }

    public ByteBuffer getByteBuffer(final Class<? extends Packet> type) {
        Byte ID = Main.packetsList.getKey(type);
        if (ID == null) throw new NullPointerException("The sent packet has an unknown ID. Class: " + type);

        logger.debug("Packet ready to be write: " + type);

        final byte[] arr = ((ByteArrayOutputStream) this.out).toByteArray();
        final byte[] out = new byte[arr.length + 1];
        out[0] = ID;
        System.arraycopy(arr, 0, out, 1, arr.length);

        return ByteBuffer.wrap(out);
    }
}
