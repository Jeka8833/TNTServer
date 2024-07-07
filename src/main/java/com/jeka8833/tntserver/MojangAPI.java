package com.jeka8833.tntserver;

import com.google.gson.annotations.SerializedName;
import com.jeka8833.tntserver.util.Util;
import okhttp3.*;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.function.Consumer;

public class MojangAPI {
    @NonBlocking
    public static void getPlayerUUID(@Nullable String name,
                                     @NotNull Consumer<@NotNull Optional<@NotNull UUID>> listener) {
        if (name == null || name.isEmpty()) {
            listener.accept(Optional.empty());
            return;
        }

        Util.HTTP_CLIENT.newCall(new Request.Builder()
                .url("https://api.mojang.com/users/profiles/minecraft/" + name).build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        try (Reader reader = responseBody.charStream()) {
                            final PlayerUUID player = Util.GSON.fromJson(reader, PlayerUUID.class);
                            if (player != null) {
                                UUID uuid = UUID.fromString(player.id
                                        .replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));

                                listener.accept(Optional.of(uuid));
                                return;
                            }
                        }
                    }
                }
                listener.accept(Optional.empty());
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                listener.accept(Optional.empty());
            }
        });
    }

    private static class PlayerUUID {
        @SerializedName("id")
        private String id;
    }
}
