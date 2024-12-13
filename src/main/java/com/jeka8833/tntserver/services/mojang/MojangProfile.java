package com.jeka8833.tntserver.services.mojang;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record MojangProfile(@Nullable String name, @Nullable UUID uuid) {
    public boolean isFullCompleted() {
        return name != null && uuid != null;
    }

    public boolean isFullAbsent() {
        return name == null && uuid == null;
    }

    public String getNameAndUuidAsText() {
        if (isFullAbsent()) {
            return "Unknown";
        } else if (isFullCompleted()) {
            return name + " (" + uuid + ")";
        } else if (uuid != null) {
            return uuid.toString();
        } else {
            return name;
        }
    }

    @Nullable
    public String getAvatarUrl() {
        if (uuid != null) return "https://mc-heads.net/avatar/" + uuid;
        if (name != null) return "https://mc-heads.net/avatar/" + name;

        return null;
    }
}
