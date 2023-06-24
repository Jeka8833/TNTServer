package com.Jeka8833.TNTServer.util;

import com.google.gson.Gson;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.net.http.HttpClient;
import java.util.UUID;
import java.util.concurrent.*;

public class Util {

    public static final HttpClient client = HttpClient.newHttpClient();
    public static final Gson GSON = new Gson();

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

    @NotNull
    @Contract(pure = true, value = "_, _, _ -> new")
    public static ThreadFactory getThreadFactory(@NotNull String name,
                                                 @Range(from = Thread.MIN_PRIORITY, to = Thread.MAX_PRIORITY)
                                                 int priority, boolean daemon) {
        return r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setPriority(priority);
            t.setDaemon(daemon);
            t.setName(name);
            return t;
        };
    }
}
