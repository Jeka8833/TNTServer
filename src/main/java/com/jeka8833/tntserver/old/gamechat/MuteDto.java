package com.jeka8833.tntserver.old.gamechat;

import java.time.Instant;

public record MuteDto(String reason, Instant unmuteTime) {
    public boolean isMuted() {
        return unmuteTime.isAfter(Instant.now());
    }
}
