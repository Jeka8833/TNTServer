package com.Jeka8833.TNTServer.packet;

import com.Jeka8833.TNTServer.Main;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PacketOutputStream extends DataOutputStream {

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
        final byte[] arr = ((ByteArrayOutputStream) this.out).toByteArray();
        final byte[] out = new byte[arr.length + 1];
        out[0] = Main.packetsList.getKey(type);
        System.arraycopy(arr, 0, out, 1, arr.length);

        return ByteBuffer.wrap(out);
    }
}
