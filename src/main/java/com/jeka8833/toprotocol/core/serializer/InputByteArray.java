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
        if (skip(n) != n) throw new IndexOutOfBoundsException("Not enough data to skip " + n + " bytes");
    }

    @Override
    @Contract(mutates = "this")
    public byte @NotNull [] readNBytes(int len) {
        return Arrays.copyOfRange(buf, pos, pos += len);
    }

    @Override
    @Contract(mutates = "this, param1")
    public int read(byte @NotNull [] b) {
        return read(b, 0, b.length);
    }

    @Contract(mutates = "this")
    public boolean readBoolean() {
        return buf[pos++] != 0;
    }

    @Contract(mutates = "this")
    public byte readByte() {
        return buf[pos++];
    }

    @Contract(mutates = "this")
    public short readShort() {
        return (short) ((buf[pos++] & 0xFF) << 8 | (buf[pos++] & 0xFF));
    }

    @Contract(mutates = "this")
    public char readChar() {
        return (char) ((buf[pos++] & 0xFF) << 8 | (buf[pos++] & 0xFF));
    }

    @Contract(mutates = "this")
    public int readInt() {
        return (buf[pos++] & 0xFF) << 24 |
                (buf[pos++] & 0xFF) << 16 |
                (buf[pos++] & 0xFF) << 8 |
                (buf[pos++] & 0xFF);
    }

    @Contract(mutates = "this")
    public long readLong() {
        return (long) (buf[pos++] & 0xFF) << 56 |
                (long) (buf[pos++] & 0xFF) << 48 |
                (long) (buf[pos++] & 0xFF) << 40 |
                (long) (buf[pos++] & 0xFF) << 32 |
                (long) (buf[pos++] & 0xFF) << 24 |
                (long) (buf[pos++] & 0xFF) << 16 |
                (long) (buf[pos++] & 0xFF) << 8 |
                (long) (buf[pos++] & 0xFF);
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
