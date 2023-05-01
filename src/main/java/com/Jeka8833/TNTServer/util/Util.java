package com.Jeka8833.TNTServer.util;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Util {

    public static String getParam(final String[] args, final String key) {
        for (int i = 0; i < args.length - 1; i++)
            if (args[i].equalsIgnoreCase(key))
                return args[i + 1];
        return System.getenv(key.substring(1).toUpperCase());
    }

    @NotNull
    public static UUID fromString(@NotNull String input) {
        return UUID.fromString(
                input.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }
}
