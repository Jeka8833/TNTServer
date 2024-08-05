package com.jeka8833.tntserver;

import com.jeka8833.tntserver.mojang.MojangAPI;
import com.jeka8833.tntserver.util.Util;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class AuthManager {
    public static final int ERROR_INTERNAL_SERVER = 3;
    public static final int ERROR_LOGIN_FAIL = 1;

    private static final @Nullable String authURLTNTClient = Main.INSTANCE.tntClientWebApiUrl.orElse(null);
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthManager.class);

    public static void authMojang(@NotNull String username, @NotNull String key, @NotNull AuthResponse response) {
        MojangAPI.checkSession(username, key, mojangProfile -> {
            if (mojangProfile.getUUID().isPresent()) {
                response.good(mojangProfile.getUUID().get(), null);
            } else {
                response.bad(ERROR_LOGIN_FAIL);
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
                LOGGER.warn("TNTClient API is down", e);
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

    public interface AuthResponse {
        void good(@NotNull UUID user, @Nullable Set<String> privileges);

        void bad(int errorCode);
    }
}
