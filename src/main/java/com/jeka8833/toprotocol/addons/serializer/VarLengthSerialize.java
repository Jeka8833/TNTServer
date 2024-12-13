package com.jeka8833.toprotocol.addons.serializer;

import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.NotNull;

public final class VarLengthSerialize {
    private VarLengthSerialize() {
        throw new UnsupportedOperationException();
    }

    public static int readVarInt(@NotNull InputByteArray inputByteArray) {

    }

    public static void writeVarInt(@NotNull OutputByteArray outputByteArray, int value) {
    }
}
