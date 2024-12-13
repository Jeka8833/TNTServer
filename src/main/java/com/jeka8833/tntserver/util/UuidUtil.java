package com.jeka8833.tntserver.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UuidUtil {

    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static UUID parseOrNull(@Nullable String value) {
        if (value == null) return null;

        try {
            return parse(value);
        } catch (Throwable e) {
            return null;
        }
    }

    @NotNull
    @Contract(value = "null -> fail; _-> new", pure = true)
    public static UUID parse(@Nullable String value) throws IllegalArgumentException {
        if (value == null) throw new IllegalArgumentException("Value cannot be null");

        try {
            return UUID.fromString(value);
        } catch (Throwable e) {
            return parseWithoutDash(value);
        }
    }

    @NotNull
    @Contract(value = "null -> fail; _-> new", pure = true)
    public static UUID parseWithoutDash(@Nullable String value) throws IllegalArgumentException {
        if (value == null) throw new IllegalArgumentException("Value cannot be null");

        return UUID.fromString(
                value.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }
}
