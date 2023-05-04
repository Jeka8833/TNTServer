package com.Jeka8833.TNTServer;

import com.Jeka8833.TNTServer.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class AuthManager {
    public static final int ERROR_MANAGER_BUSY = 5;
    public static final int ERROR_INTERNAL_SERVER = 3;
    public static final int ERROR_LOGIN_FAIL = 1;
    public static final int ERROR_SERVER_THROTTLING = 4;

    private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
    private static final Logger logger = LogManager.getLogger(AuthManager.class);
    private static final HttpClient client = HttpClient.newHttpClient();

    @Deprecated
    public static void authHypixel(@NotNull UUID user, @NotNull UUID key, @NotNull AuthResponse response) {
        if (managerIsBusy()) {
            response.bad(ERROR_MANAGER_BUSY);
            return;
        }

        executor.execute(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.hypixel.net/key?key=" + key))
                        .build();

                HttpResponse<String> serverResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (serverResponse.statusCode() == 200) {
                    ParseUser parseUser = Main.GSON.fromJson(serverResponse.body(), ParseUser.class);
                    if (parseUser == null || parseUser.record == null || parseUser.record.owner == null) {
                        response.bad(ERROR_INTERNAL_SERVER);
                    } else if (parseUser.record.owner.equals(user)) {
                        response.good(user, null);
                    } else {
                        response.bad(ERROR_LOGIN_FAIL);
                    }
                    return;
                } else if (serverResponse.statusCode() == 403) {
                    response.bad(ERROR_LOGIN_FAIL);
                    return;
                } else if (serverResponse.statusCode() == 429) {
                    response.bad(ERROR_SERVER_THROTTLING);
                    return;
                }
            } catch (Exception e) {
                response.bad(ERROR_INTERNAL_SERVER);
                logger.warn("Hypixel API is down", e);
                return;
            }

            response.bad(ERROR_INTERNAL_SERVER);
        });
    }

    public static void authMojang(@NotNull String username, @NotNull String key, @NotNull AuthResponse response) {
        if (managerIsBusy()) {
            response.bad(ERROR_MANAGER_BUSY);
            return;
        }

        executor.execute(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(
                        URI.create("https://sessionserver.mojang.com/session/minecraft/hasJoined?serverId=" +
                                key + "&username=" + username)).build();

                HttpResponse<String> serverResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (serverResponse.statusCode() == 200) {
                    ResponseID responseID = Main.GSON.fromJson(serverResponse.body(), ResponseID.class);
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
            } catch (Exception e) {
                response.bad(ERROR_INTERNAL_SERVER);
                logger.warn("Mojang API is down", e);
                return;
            }
            response.bad(ERROR_LOGIN_FAIL);
        });
    }

    public static void authTNTClient(@NotNull UUID user, @NotNull UUID key, @NotNull AuthResponse response) {
        if (user.version() != 4 || user.variant() != 0) {
            response.bad(ERROR_LOGIN_FAIL);
            return;
        }

        if (managerIsBusy()) {
            response.bad(ERROR_MANAGER_BUSY);
            return;
        }

        executor.execute(() -> {
            try {
                String authorizationHeader = "Basic " +
                        new String(Base64.getEncoder().encode((user + ":" + key).getBytes()));

                HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/tempToken/login"))
                        .header("Authorization", authorizationHeader).build();

                HttpResponse<String> serverResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (serverResponse.statusCode() == 200) {
                    Set<String> privileges = BotsManager.validateBotAndCutPrivilege(serverResponse.body());
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
    }

    private static boolean managerIsBusy() {
        return executor.getQueue().size() >= executor.getMaximumPoolSize();
    }

    private static class ResponseID {
        private @Nullable String id;

        @Nullable
        public UUID getUuid() {
            if (id == null) return null;

            return Util.fromString(id);
        }
    }

    private static class ParseUser {
        private @Nullable Record record;
    }

    private static class Record {
        private @Nullable UUID owner;
    }

    public interface AuthResponse {
        void good(@NotNull UUID user, @Nullable Set<String> privileges);

        void bad(int errorCode);
    }
}
