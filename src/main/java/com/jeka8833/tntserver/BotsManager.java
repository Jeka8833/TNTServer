package com.jeka8833.tntserver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.jeka8833.tntserver.database.storage.Bot;
import com.jeka8833.tntserver.database.storage.User;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BotsManager {
    private static final String BOT_LABEL = "TNTCLIENT_SERVER";

    private static final TypeReference<HashSet<String>> HASH_SET_TYPE = new TypeReference<>() {
    };

    @Nullable
    @UnmodifiableView
    @Contract("null->null")
    public static Set<String> validateBotAndCutPrivilege(@Nullable String privileges) {
        if (privileges == null) return null;

        Set<String> privilegesArray = JSON.parseObject(privileges, HASH_SET_TYPE);
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
