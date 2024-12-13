package com.jeka8833.toprotocol.addons.serializer;

import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public final class StringSerialize {
    private StringSerialize() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Contract(value = "_ -> new", mutates = "param1")
    public static String readString(@NotNull InputByteArray inputByteArray) {
        return new String(ArraySerialize.readByteArray(inputByteArray), StandardCharsets.UTF_8);
    }

    @NotNull
    @Contract(value = "_, _ -> param1", mutates = "param1")
    public static OutputByteArray writeString(@NotNull OutputByteArray outputByteArray, @NotNull String string) {
        return ArraySerialize.writeByteArray(outputByteArray, string.getBytes(StandardCharsets.UTF_8));
    }
}
