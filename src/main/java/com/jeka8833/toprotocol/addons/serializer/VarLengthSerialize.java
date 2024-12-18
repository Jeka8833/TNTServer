package com.jeka8833.toprotocol.addons.serializer;

import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class VarLengthSerialize {
    private VarLengthSerialize() {
        throw new UnsupportedOperationException();
    }

    @Contract(mutates = "param1")
    public static int readVarInt(@NotNull InputByteArray inputByteArray) {
        int i = 0;
        int j = 0;

        while (true) {
            byte b0 = inputByteArray.readByte();
            i |= (b0 & 127) << j++ * 7;

            if (j > 5) {
                throw new IndexOutOfBoundsException("VarInt too big");
            }

            if ((b0 & 128) != 128) {
                break;
            }
        }

        return i;
    }

    @Contract(mutates = "param1")
    public static void writeVarInt(@NotNull OutputByteArray outputByteArray, int value) {
        while ((value & -128) != 0) {
            outputByteArray.writeByte((byte) (value & 0x7F | 0x80));
            value >>>= 7;
        }

        outputByteArray.writeByte((byte) value);
    }

    @Contract(mutates = "param1")
    public static long readVarLong(@NotNull InputByteArray inputByteArray) {
        long i = 0L;
        int j = 0;

        while (true) {
            byte b0 = inputByteArray.readByte();
            i |= (long) (b0 & 127) << j++ * 7;

            if (j > 10) {
                throw new IndexOutOfBoundsException("VarLong too big");
            }

            if ((b0 & 128) != 128) {
                break;
            }
        }

        return i;
    }

    @Contract(mutates = "param1")
    public static void writeVarLong(@NotNull OutputByteArray outputByteArray, long value) {
        while ((value & -128L) != 0) {
            outputByteArray.writeByte((byte) ((int) value & 0x7F | 0x80));
            value >>>= 7;
        }

        outputByteArray.writeByte((byte) value);
    }
}
