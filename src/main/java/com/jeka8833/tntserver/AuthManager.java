package com.jeka8833.tntserver;

import com.jeka8833.tntserver.util.Util;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import java.util.UUID;

public class AuthManager {
    public static final int ERROR_INTERNAL_SERVER = 3;
    public static final int ERROR_LOGIN_FAIL = 1;

    public static String authURLTNTClient;
    private static final Logger logger = LogManager.getLogger(AuthManager.class);

    public static void authMojang(@NotNull String username, @NotNull String key, @NotNull AuthResponse response) {
        Request request = new Request.Builder()
                .url("https://sessionserver.mojang.com/session/minecraft/hasJoined?serverId=" + key +
                        "&username=" + username)
                .build();
        Util.HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                response.bad(ERROR_INTERNAL_SERVER);
                logger.warn("Mojang API is down", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response res) throws IOException {
                try (ResponseBody body = res.body(); Reader reader = body.charStream()) {
                    if (res.isSuccessful()) {
                        ResponseID responseID = Util.GSON.fromJson(reader, ResponseID.class);
                        if (responseID == null) {
                            response.bad(ERROR_INTERNAL_SERVER);
                        } else {
                            UUID uuid = responseID.getUuid();
                            if (uuid == null) {
                                response.bad(ERROR_INTERNAL_SERVER);
                            } else {
                                response.good(uuid, null);
                            }
                        }
                    } else {
                        response.bad(ERROR_LOGIN_FAIL);
                    }
                }
            }
        });
    }

    public static void authTNTClient(@NotNull UUID user, @NotNull UUID key, @NotNull AuthResponse response) {
        if (authURLTNTClient == null || user.version() != 4 || user.variant() != 0) {
            response.bad(ERROR_LOGIN_FAIL);
            return;
        }

        Request request = new Request.Builder()
                .url(authURLTNTClient)
                .header("Authorization", Credentials.basic(user.toString(), key.toString()))
                .build();

        Util.HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                response.bad(ERROR_INTERNAL_SERVER);
                logger.warn("TNTClient API is down", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response res) throws IOException {
                try (ResponseBody body = res.body()) {
                    if (res.isSuccessful()) {
                        Set<String> privileges = BotsManager.validateBotAndCutPrivilege(body.string());

                        if (privileges == null) {
                            response.bad(ERROR_LOGIN_FAIL);
                        } else {
                            response.good(user, privileges);
                        }
                    } else {
                        response.bad(ERROR_LOGIN_FAIL);
                    }
                }
            }
        });
    }

    private static class ResponseID {
        @SuppressWarnings("unused")
        private @Nullable String id;

        @Nullable
        public UUID getUuid() {
            if (id == null) return null;

            return Util.fromString(id);
        }
    }

    public interface AuthResponse {
        void good(@NotNull UUID user, @Nullable Set<String> privileges);

        void bad(int errorCode);
    }
}
