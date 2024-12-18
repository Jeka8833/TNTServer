package com.jeka8833.toprotocol.core.serializer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class InputByteArray extends ByteArrayInputStream {
    public InputByteArray(byte[] buffer) {
        super(buffer);
    }

    @Override
    @Contract(mutates = "this")
    public void skipNBytes(long n) {
        if (n <= 0L) return;
        if (count - pos < n) throw new IndexOutOfBoundsException("Not enough data to skip " + n + " bytes");

        pos += (int) n;
    }

    @Override
    @Contract(mutates = "this")
    public byte @NotNull [] readNBytes(int len) {
        if (len < 0) throw new IndexOutOfBoundsException(len + " < 0");

        byte[] bytes = Arrays.copyOfRange(buf, pos, pos + len);
        pos += len;

        return bytes;
    }

    @Override
    @Contract(mutates = "this, param1")
    public int read(byte @NotNull [] b) {
        return read(b, 0, b.length);
    }

    @Contract(mutates = "this")
    public boolean readBoolean() {
        boolean value = buf[pos] != 0;
        pos++;

        return value;
    }

    @Contract(mutates = "this")
    public byte readByte() {
        byte value = buf[pos];
        pos++;

        return value;
    }

    @Contract(mutates = "this")
    public short readShort() {
        short value = (short) ((buf[pos] & 0xFF) << 8 | (buf[pos + 1] & 0xFF));
        pos += 2;

        return value;
    }

    @Contract(mutates = "this")
    public char readChar() {
        char value = (char) ((buf[pos] & 0xFF) << 8 | (buf[pos + 1] & 0xFF));
        pos += 2;

        return value;
    }

    @Contract(mutates = "this")
    public int readInt() {
        int value = (buf[pos] & 0xFF) << 24 |
                (buf[pos + 1] & 0xFF) << 16 |
                (buf[pos + 2] & 0xFF) << 8 |
                (buf[pos + 3] & 0xFF);
        pos += 4;

        return value;
    }

    @Contract(mutates = "this")
    public long readLong() {
        long value = (buf[pos] & 0xFFL) << 56 |
                (buf[pos + 1] & 0xFFL) << 48 |
                (buf[pos + 2] & 0xFFL) << 40 |
                (buf[pos + 3] & 0xFFL) << 32 |
                (buf[pos + 4] & 0xFFL) << 24 |
                (buf[pos + 5] & 0xFFL) << 16 |
                (buf[pos + 6] & 0xFFL) << 8 |
                (buf[pos + 7] & 0xFFL);
        pos += 8;

        return value;
    }

    @Contract(mutates = "this")
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    @Contract(mutates = "this")
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }
}
