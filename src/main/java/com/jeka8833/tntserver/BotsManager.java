package com.jeka8833.tntserver;

import com.google.gson.reflect.TypeToken;
import com.jeka8833.tntserver.database.Bot;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.util.Util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BotsManager {
    private static final String BOT_LABEL = "TNTCLIENT_SERVER";

    private static final Type setType = new TypeToken<HashSet<String>>() {
    }.getType();

    @Nullable
    @UnmodifiableView
    @Contract("null->null")
    public static Set<String> validateBotAndCutPrivilege(@Nullable String privileges) {
        if (privileges == null) return null;

        Set<String> privilegesArray = Util.GSON.fromJson(privileges, setType);
        if (!privilegesArray.contains(BOT_LABEL)) return null;

        return Collections.unmodifiableSet(privilegesArray);
    }

    @Contract(value = "null, _ -> true; _, null -> true", pure = true)
    public static boolean isAbsent(@Nullable User user, @Nullable String privilege) {
        if (user instanceof Bot bot) {
            return !bot.hasPrivilege(privilege);
        }
        return true;
    }
}
