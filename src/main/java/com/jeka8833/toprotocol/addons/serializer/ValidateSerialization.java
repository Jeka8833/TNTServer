package com.jeka8833.toprotocol.addons.serializer;

import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ValidateSerialization {
    private ValidateSerialization() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Contract(value = "_ -> new")
    public static <T> Function<InputByteArray, T> validateRead(@NotNull Function<InputByteArray, T> function) {
        return inputByteArray -> {
            int available = inputByteArray.available();

            T value = function.apply(inputByteArray);

            if (available >= inputByteArray.available()) {
                throw new IndexOutOfBoundsException(
                        "Nothing was written down or the InputByteArray#mark(int) operator was triggered.");
            }

            return value;
        };
    }

    @NotNull
    @Contract(value = "_ -> new")
    public static <T> BiConsumer<@NotNull OutputByteArray, T> validateWrite(
            @NotNull BiConsumer<@NotNull OutputByteArray, T> consumer) {
        return (inputByteArray, value) -> {
            int available = inputByteArray.available();

            consumer.accept(inputByteArray, value);

            if (available >= inputByteArray.available()) {
                throw new IndexOutOfBoundsException(
                        "Nothing was written down or the OutputByteArray#setSize(int) operator was triggered.");
            }
        };
    }
}
