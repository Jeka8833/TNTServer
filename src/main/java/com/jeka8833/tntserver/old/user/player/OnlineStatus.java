package com.jeka8833.tntserver.old.user.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum OnlineStatus {
    UNKNOWN(-1),
    OFFLINE(0),
    INVISIBLE(1),
    AFK(2),
    ONLINE(3),
    ;

    private final int code;

    public static final OnlineStatus[] VALUES = OnlineStatus.values();

    @NotNull
    public static OnlineStatus fromCode(int code) {
        for (OnlineStatus status : VALUES) {
            if (status.code == code) return status;
        }

        return UNKNOWN;
    }
}
