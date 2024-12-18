package com.jeka8833.toprotocol.core.serializer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Objects;

public class OutputByteArray extends ByteArrayOutputStream {
    private final int limit;

    public OutputByteArray(int limit) {
        this(limit, 16);
    }

    public OutputByteArray(int limit, int initialSize) {
        super(initialSize);

        if (limit <= 0) throw new IllegalArgumentException("limit for OutputByteArray cannot be <= 0");
        this.limit = limit;
    }

    private void reserveOrGrow(int reserve) {
        if (limit - count < reserve) {
            throw new IndexOutOfBoundsException("OutputByteArray.reserveOrGrow() exceeded the limit");
        }

        int minCapacity = count + reserve;
        if (minCapacity > buf.length) {
            int newCapacity = Math.min(Math.max(minCapacity, buf.length << 1), limit);

            buf = Arrays.copyOf(buf, newCapacity);
        }
    }

    @Override
    @Contract(mutates = "this")
    public void write(int b) {
        writeByte((byte) b);
    }

    @Override
    @Contract(mutates = "this")
    public void write(byte @NotNull [] b) {
        write(b, 0, b.length);
    }

    @Override
    public void writeBytes(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    @Contract(mutates = "this")
    public void write(byte[] b, int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        reserveOrGrow(len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    @Contract(mutates = "this")
    public void setSize(int size) {
        if (size < 0) throw new IllegalArgumentException("size cannot be < 0");
        if (size > limit) throw new IllegalArgumentException("size cannot be > limit: " + limit);

        count = size;
    }

    public int available() {
        return Math.max(0, limit - count);
    }

    @Contract(mutates = "this")
    public void skip(int n) {
        if (n <= 0) return;

        if (limit - count < n) throw new IndexOutOfBoundsException("OutputByteArray.skip() exceeded the limit");
        count += n; // Without array grow
    }

    @Contract(mutates = "this")
    public void writeBoolean(boolean b) {
        writeByte(b ? (byte) 1 : (byte) 0);
    }

    @Contract(mutates = "this")
    public void writeByte(byte b) {
        reserveOrGrow(1);
        buf[count++] = b;
    }

    @Contract(mutates = "this")
    public void writeShort(short s) {
        reserveOrGrow(2);
        buf[count++] = (byte) (s >> 8);
        buf[count++] = (byte) s;
    }

    @Contract(mutates = "this")
    public void writeChar(char c) {
        reserveOrGrow(2);
        buf[count++] = (byte) (c >> 8);
        buf[count++] = (byte) c;
    }

    @Contract(mutates = "this")
    public void writeInt(int i) {
        reserveOrGrow(4);
        buf[count++] = (byte) (i >> 24);
        buf[count++] = (byte) (i >> 16);
        buf[count++] = (byte) (i >> 8);
        buf[count++] = (byte) i;
    }

    @Contract(mutates = "this")
    public void writeLong(long l) {
        reserveOrGrow(8);
        buf[count++] = (byte) (l >> 56);
        buf[count++] = (byte) (l >> 48);
        buf[count++] = (byte) (l >> 40);
        buf[count++] = (byte) (l >> 32);
        buf[count++] = (byte) (l >> 24);
        buf[count++] = (byte) (l >> 16);
        buf[count++] = (byte) (l >> 8);
        buf[count++] = (byte) l;
    }

    @Contract(mutates = "this")
    public void writeFloat(float f) {
        writeInt(Float.floatToRawIntBits(f));
    }

    @Contract(mutates = "this")
    public void writeDouble(double d) {
        writeLong(Double.doubleToRawLongBits(d));
    }
}
