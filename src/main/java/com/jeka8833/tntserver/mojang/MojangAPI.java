package com.jeka8833.tntserver.mojang;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jeka8833.tntserver.util.Util;
import okhttp3.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class MojangAPI {
    private static final AsyncLoadingCache<UUID, MojangProfile> REQUEST_UUID = Caffeine.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(100)
            .buildAsync(key -> {
                Request request = new Request.Builder()
                        .url("https://sessionserver.mojang.com/session/minecraft/profile/" + key)
                        .build();

                try (Response response = Util.HTTP_CLIENT.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        try (ResponseBody responseBody = response.body();
                             Reader reader = responseBody.charStream()) {
                            PlayerUUID player = JSON.parseObject(reader, PlayerUUID.class);
                            if (player != null) {
                                return new MojangProfile(player.getName(), player.getUUIDOrElse(key));
                            }
                        }
                    } else if (response.code() == 404 || response.code() == 204) {
                        MojangProfile profile = new MojangProfile(key);
                        profile.setNotFound();
                        return profile;
                    }
                }
                return new MojangProfile(key);
            });
    private static final AsyncLoadingCache<PlayerName, MojangProfile> REQUEST_NAME = Caffeine.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(100)
            .buildAsync(name -> {
                Request request = new Request.Builder()
                        .url("https://api.mojang.com/users/profiles/minecraft/" + name.name())
                        .build();

                try (Response response = Util.HTTP_CLIENT.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        try (ResponseBody responseBody = response.body();
                             Reader reader = responseBody.charStream()) {
                            PlayerUUID player = JSON.parseObject(reader, PlayerUUID.class);
                            if (player != null) {
                                return new MojangProfile(player.getNameOrElse(name.name()), player.getUUID());
                            }
                        }
                    } else if (response.code() == 404 || response.code() == 204) {
                        MojangProfile profile = new MojangProfile(name.name());
                        profile.setNotFound();
                        return profile;
                    }
                }
                return new MojangProfile(name.name());
            });

    public static void checkSession(@NotNull String username, @NotNull String key,
                                    @NotNull Consumer<@NotNull MojangProfile> listener) {
        Request request = new Request.Builder()
                .url("https://sessionserver.mojang.com/session/minecraft/hasJoined?serverId=" + key +
                        "&username=" + username)
                .build();

        Util.HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                listener.accept(new MojangProfile(username));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response res) {
                try (ResponseBody body = res.body(); Reader reader = body.charStream()) {
                    if (res.isSuccessful()) {
                        PlayerUUID responseID = JSON.parseObject(reader, PlayerUUID.class);
                        if (responseID != null) {
                            listener.accept(new MojangProfile(responseID.getName(), responseID.getUUID()));
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
                listener.accept(new MojangProfile(username));
            }
        });
    }

    public static void getName(@Nullable UUID uuid, @NotNull Consumer<@NotNull MojangProfile> listener) {
        if (!isPlayer(uuid)) {
            listener.accept(new MojangProfile(uuid));
            return;
        }

        CompletableFuture<MojangProfile> graph = REQUEST_UUID.get(uuid);
        graph.whenComplete((mojangProfile, throwable) -> {
            if (mojangProfile == null) {
                listener.accept(new MojangProfile(uuid));
                return;
            }

            listener.accept(mojangProfile);
        });
    }

    public static void getUUID(@Nullable String name, @NotNull Consumer<@NotNull MojangProfile> listener) {
        PlayerName playerName = new PlayerName(name);
        if (!playerName.isValid()) {
            listener.accept(new MojangProfile(playerName.name()));
            return;
        }

        CompletableFuture<MojangProfile> graph = REQUEST_NAME.get(playerName);
        graph.whenComplete((mojangProfile, throwable) -> {
            if (mojangProfile == null) {
                listener.accept(new MojangProfile(playerName.name()));
                return;
            }

            listener.accept(mojangProfile);
        });
    }

    @Contract(pure = true)
    public static boolean isPlayer(@Nullable UUID playerUUID) {
        return playerUUID != null && playerUUID.version() == 4 && playerUUID.variant() == 2;
    }

    private record PlayerName(@Nullable String name) {
        private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{1,16}$");

        private boolean isValid() {
            return name != null && NAME_PATTERN.matcher(name).matches();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PlayerName that)) return false;

            if (name == null) return that.name == null;
            return name.equalsIgnoreCase(that.name);
        }

        @Override
        public int hashCode() {
            return name == null ? 0 : name.toLowerCase().hashCode();
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class PlayerUUID {
        public @JSONField(name = "name") Optional<String> name;
        public @JSONField(name = "id") Optional<String> id;

        @NotNull
        @Contract(pure = true)
        private Optional<@NotNull String> getName() {
            return name;
        }

        @NotNull
        @Contract(pure = true)
        private Optional<@NotNull String> getNameOrElse(@Nullable String defaultValue) {
            return name.isPresent() ? name : Optional.ofNullable(defaultValue);
        }

        @NotNull
        @Contract(pure = true)
        private Optional<@NotNull UUID> getUUID() {
            try {
                return id.map(s -> UUID.fromString(
                        s.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        @NotNull
        @Contract(pure = true)
        private Optional<@NotNull UUID> getUUIDOrElse(@Nullable UUID defaultValue) {
            Optional<UUID> uuid = getUUID();

            return uuid.isPresent() ? uuid : Optional.ofNullable(defaultValue);
        }
    }
}
