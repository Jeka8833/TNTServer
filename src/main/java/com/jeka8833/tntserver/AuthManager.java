package com.jeka8833.tntserver;

import com.jeka8833.tntserver.util.Util;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

public class AuthManager {
    public static final int ERROR_MANAGER_BUSY = 5;
    public static final int ERROR_INTERNAL_SERVER = 3;
    public static final int ERROR_LOGIN_FAIL = 1;

    public static String authURLTNTClient;

    private static final Executor AUTH_POOL = new ThreadPoolExecutor(0, 5,
            10, TimeUnit.SECONDS, new SynchronousQueue<>(),
            Util.getThreadFactory("Auth Thread", Thread.MIN_PRIORITY, true));
    private static final Logger logger = LogManager.getLogger(AuthManager.class);

    public static void authMojang(@NotNull String username, @NotNull String key, @NotNull AuthResponse response) {
        try {
            AUTH_POOL.execute(() -> {
                Request request = new Request.Builder()
                        .url("https://sessionserver.mojang.com/session/minecraft/hasJoined?serverId=" +
                                key + "&username=" + username)
                        .build();
                try (Response serverResponse = Util.clientOk.newCall(request).execute()) {
                    if (serverResponse.isSuccessful()) {
                        try (ResponseBody body = serverResponse.body(); Reader reader = body.charStream()) {
                            ResponseID responseID = Util.GSON.fromJson(reader, ResponseID.class);
                            if (responseID == null) {
                                response.bad(ERROR_INTERNAL_SERVER);
                            } else {
                                UUID uuid = responseID.getUuid();
                                if (uuid != null) {
                                    response.good(uuid, null);
                                } else {
                                    response.bad(ERROR_INTERNAL_SERVER);
                                }
                            }
                            return;
                        }
                    }
                } catch (Exception e) {
                    response.bad(ERROR_INTERNAL_SERVER);
                    logger.warn("Mojang API is down", e);
                    return;
                }
                response.bad(ERROR_LOGIN_FAIL);
            });
        } catch (RejectedExecutionException ignore) {
            response.bad(ERROR_MANAGER_BUSY);
        }
    }

    public static void authTNTClient(@NotNull UUID user, @NotNull UUID key, @NotNull AuthResponse response) {
        if (authURLTNTClient == null || user.version() != 4 || user.variant() != 0) {
            response.bad(ERROR_LOGIN_FAIL);
            return;
        }
        try {
            AUTH_POOL.execute(() -> {
                Request request = new Request.Builder()
                        .url(authURLTNTClient)
                        .header("Authorization", Credentials.basic(user.toString(), key.toString()))
                        .build();
                try (Response serverResponse = Util.clientOk.newCall(request).execute()) {
                    if (serverResponse.isSuccessful()) {
                        Set<String> privileges = BotsManager.validateBotAndCutPrivilege(
                                serverResponse.body().string());
                        if (privileges != null) {
                            response.good(user, privileges);
                        } else {
                            response.bad(ERROR_LOGIN_FAIL);
                        }
                        return;
                    }
                } catch (Exception e) {
                    response.bad(ERROR_INTERNAL_SERVER);
                    logger.warn("TNTClient API is down", e);
                    return;
                }
                response.bad(ERROR_LOGIN_FAIL);
            });
        } catch (RejectedExecutionException ignore) {
            response.bad(ERROR_MANAGER_BUSY);
        }
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
