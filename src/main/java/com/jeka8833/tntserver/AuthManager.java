package com.jeka8833.tntserver;

import com.jeka8833.tntserver.database.RemoteDB;
import com.jeka8833.tntserver.mojang.MojangAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class AuthManager {
    public static final int ERROR_INTERNAL_SERVER = 3;
    public static final int ERROR_LOGIN_FAIL = 1;

    public static void authMojang(@NotNull String username, @NotNull String key, @NotNull AuthResponse response) {
        MojangAPI.checkSession(username, key, mojangProfile -> {
            if (mojangProfile.getUuid().isPresent()) {
                response.good(mojangProfile.getUuid().get(), null);
            } else {
                response.bad(ERROR_LOGIN_FAIL);
            }
        });
    }

    public static void authTNTClient(@NotNull UUID user, @NotNull UUID key, @NotNull AuthResponse response) {
        RemoteDB.loginAndGetPrivileges(user, key, optionalPrivileges -> {
            if (optionalPrivileges.isEmpty()) {
                response.bad(ERROR_INTERNAL_SERVER);
            } else {
                Set<String> privileges = optionalPrivileges.get();

                if (privileges.isEmpty()) {
                    response.bad(ERROR_LOGIN_FAIL);
                } else {
                    response.good(user, privileges);
                }
            }
        });
    }

    public interface AuthResponse {
        void good(@NotNull UUID user, @Nullable Set<String> privileges);

        void bad(int errorCode);
    }
}
