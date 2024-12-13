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

    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= 0 || minCapacity > limit) {
            throw new IndexOutOfBoundsException("PacketOutputSerializer exceeded limit of " + limit);
        }

        if (minCapacity > buf.length) {
            int newCapacity = Math.max(minCapacity, buf.length << 1);

            if (newCapacity > limit) {
                newCapacity = limit;
            }

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
    @Contract(mutates = "this")
    public void write(byte[] b, int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        ensureCapacity(count + len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    @NotNull
    @Contract(value = "_ -> this", mutates = "this")
    public OutputByteArray skip(int n) {
        if (n < 0) throw new IllegalArgumentException("Can't skip negative bytes");

        count += n; // Without array grow

        return this;
    }

    @NotNull
    @Contract(value = "_ -> this", mutates = "this")
    public OutputByteArray writeBoolean(boolean b) {
        return writeByte(b ? (byte) 1 : (byte) 0);
    }

    @NotNull
    @Contract(value = "_ -> this", mutates = "this")
    public OutputByteArray writeByte(byte b) {
        ensureCapacity(count + 1);
        buf[count++] = b;

        return this;
    }

    @NotNull
    @Contract(value = "_ -> this", mutates = "this")
    public OutputByteArray writeShort(short s) {
        ensureCapacity(count + 2);
        buf[count++] = (byte) (s >> 8);
        buf[count++] = (byte) s;

        return this;
    }

    @NotNull
    @Contract(value = "_ -> this", mutates = "this")
    public OutputByteArray writeChar(char c) {
        ensureCapacity(count + 2);
        buf[count++] = (byte) (c >> 8);
        buf[count++] = (byte) c;

        return this;
    }

    @NotNull
    @Contract(value = "_ -> this", mutates = "this")
    public OutputByteArray writeInt(int i) {
        ensureCapacity(count + 4);
        buf[count++] = (byte) (i >> 24);
        buf[count++] = (byte) (i >> 16);
        buf[count++] = (byte) (i >> 8);
        buf[count++] = (byte) i;

        return this;
    }

    @NotNull
    @Contract(value = "_ -> this", mutates = "this")
    public OutputByteArray writeLong(long l) {
        ensureCapacity(count + 8);
        buf[count++] = (byte) (l >> 56);
        buf[count++] = (byte) (l >> 48);
        buf[count++] = (byte) (l >> 40);
        buf[count++] = (byte) (l >> 32);
        buf[count++] = (byte) (l >> 24);
        buf[count++] = (byte) (l >> 16);
        buf[count++] = (byte) (l >> 8);
        buf[count++] = (byte) l;

        return this;
    }

    @NotNull
    @Contract(value = "_ -> this", mutates = "this")
    public OutputByteArray writeFloat(float f) {
        return writeInt(Float.floatToRawIntBits(f));
    }

    @NotNull
    @Contract(value = "_ -> this", mutates = "this")
    public OutputByteArray writeDouble(double d) {
        return writeLong(Double.doubleToRawLongBits(d));
    }
}
