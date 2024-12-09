package com.jeka8833.tntserver.old.user.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum GameServer {
    HYPIXEL("Hypixel"),
    TNT_COMMUNITY("Odyssey"),
    UNKNOWN("Unknown"),
    ;

    private final String serverBrands;

    public final static GameServer[] VALUES = GameServer.values();

    @NotNull
    public static GameServer findByServerBrand(@Nullable String serverBrand) {
        for (GameServer type : VALUES) {
            if (type.serverBrands.equalsIgnoreCase(serverBrand)) {
                return type;
            }
        }

        return UNKNOWN;
    }
}
