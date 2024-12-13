package com.jeka8833.toprotocol.addons.serializer;

import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ArraySerialize {
    private ArraySerialize() {
        throw new UnsupportedOperationException();
    }

    @Contract(value = "_ -> new", mutates = "param1")
    public static byte @NotNull [] readByteArray(@NotNull InputByteArray inputByteArray) {
        int len = VarLengthSerialize.readVarInt(inputByteArray);

        return inputByteArray.readNBytes(len);
    }

    @NotNull
    @Contract(value = "_, _ -> param1", mutates = "param1")
    public static OutputByteArray writeByteArray(@NotNull OutputByteArray outputByteArray, byte @NotNull [] bytes) {
        VarLengthSerialize.writeVarInt(outputByteArray, bytes.length);
        outputByteArray.writeBytes(bytes);  // TODO: check

        return outputByteArray;
    }

    @NotNull
    @Contract(value = "_, _ -> new", mutates = "param1")
    public static <T> Collection<@Nullable T> readArray(
            @NotNull InputByteArray inputByteArray,
            @NotNull Function<@NotNull InputByteArray, @Nullable T> function) {
        int len = VarLengthSerialize.readVarInt(inputByteArray);
        if (len < 0) throw new IndexOutOfBoundsException(len + " < 0");

        Collection<T> collection = new ArrayList<>();   // Don't pre allocate! Attacker can send us a huge array size
        for (int i = 0; i < len; i++) {
            collection.add(function.apply(inputByteArray));
        }

        return collection;
    }

    @NotNull
    @Contract(value = "_, _, _ -> param1", mutates = "param1")
    public static <T> OutputByteArray writeArray(
            @NotNull OutputByteArray outputByteArray, Collection<@Nullable T> collection,
            @NotNull BiConsumer<@NotNull OutputByteArray, @Nullable T> consumer) {
        VarLengthSerialize.writeVarInt(outputByteArray, collection.size());
        for (T t : collection) {
            consumer.accept(outputByteArray, t);
        }

        return outputByteArray;
    }
}
