package com.jeka8833.toprotocol.addons.serializer;

import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class OptionalSerialize {
    private OptionalSerialize() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Contract(mutates = "param1")
    public static OptionalInt readOptionalInt(@NotNull InputByteArray inputByteArray) {
        if (inputByteArray.readBoolean()) return OptionalInt.of(inputByteArray.readInt());

        return OptionalInt.empty();
    }

    @Contract(mutates = "param1")
    public static void writeOptionalInt(@NotNull OutputByteArray outputByteArray,
                                        @NotNull OptionalInt optionalInt) {
        outputByteArray.writeBoolean(optionalInt.isPresent());
        if (optionalInt.isPresent()) outputByteArray.writeInt(optionalInt.getAsInt());
    }

    @Contract(mutates = "param1")
    public static void writeOptionalInt(@NotNull OutputByteArray outputByteArray, @Nullable Integer value) {
        outputByteArray.writeBoolean(value != null);
        if (value != null) outputByteArray.writeInt(value);
    }

    @NotNull
    @Contract(mutates = "param1")
    public static OptionalLong readOptionalLong(@NotNull InputByteArray inputByteArray) {
        if (inputByteArray.readBoolean()) return OptionalLong.of(inputByteArray.readLong());

        return OptionalLong.empty();
    }

    @Contract(mutates = "param1")
    public static void writeOptionalLong(@NotNull OutputByteArray outputByteArray,
                                         @NotNull OptionalLong optionalLong) {
        outputByteArray.writeBoolean(optionalLong.isPresent());
        if (optionalLong.isPresent()) outputByteArray.writeLong(optionalLong.getAsLong());
    }

    @Contract(mutates = "param1")
    public static void writeOptionalLong(@NotNull OutputByteArray outputByteArray, @Nullable Long value) {
        outputByteArray.writeBoolean(value != null);
        if (value != null) outputByteArray.writeLong(value);
    }

    @NotNull
    @Contract(mutates = "param1")
    public static OptionalDouble readOptionalDouble(@NotNull InputByteArray inputByteArray) {
        if (inputByteArray.readBoolean()) return OptionalDouble.of(inputByteArray.readDouble());

        return OptionalDouble.empty();
    }

    @Contract(mutates = "param1")
    public static void writeOptionalDouble(@NotNull OutputByteArray outputByteArray,
                                           @NotNull OptionalDouble optionalDouble) {
        outputByteArray.writeBoolean(optionalDouble.isPresent());
        if (optionalDouble.isPresent()) outputByteArray.writeDouble(optionalDouble.getAsDouble());
    }

    @Contract(mutates = "param1")
    public static void writeOptionalDouble(@NotNull OutputByteArray outputByteArray,
                                           @Nullable Double value) {
        outputByteArray.writeBoolean(value != null);
        if (value != null) outputByteArray.writeDouble(value);
    }

    @NotNull
    @Contract(mutates = "param1")
    public static <T> Optional<T> readOptional(@NotNull InputByteArray inputByteArray,
                                               @NotNull Function<@NotNull InputByteArray, @Nullable T> function) {
        return inputByteArray.readBoolean() ? Optional.ofNullable(
                ValidateSerialization.validateRead(function).apply(inputByteArray)) : Optional.empty();
    }

    @Contract(mutates = "param1")
    public static <T> void writeOptional(
            @NotNull OutputByteArray outputByteArray, @NotNull Optional<@NotNull T> optional,
            @NotNull BiConsumer<@NotNull OutputByteArray, @NotNull T> consumer) {
        outputByteArray.writeBoolean(optional.isPresent());
        optional.ifPresent(t -> ValidateSerialization.validateWrite(consumer).accept(outputByteArray, t));
    }

    @Nullable
    @Contract(mutates = "param1")
    public static <T> T readOptionally(@NotNull InputByteArray inputByteArray,
                                       @NotNull Function<@NotNull InputByteArray, @NotNull T> function) {
        return inputByteArray.readBoolean() ? ValidateSerialization.validateRead(function).apply(inputByteArray) : null;
    }

    @Contract(mutates = "param1")
    public static <T> void writeOptionally(@NotNull OutputByteArray outputByteArray, @Nullable T optional,
                                           @NotNull BiConsumer<@NotNull OutputByteArray, @NotNull T> consumer) {
        outputByteArray.writeBoolean(optional != null);
        if (optional != null) ValidateSerialization.validateWrite(consumer).accept(outputByteArray, optional);
    }
}
