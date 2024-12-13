package com.jeka8833.tntserver.services.mojang.dtos;

import org.jetbrains.annotations.Nullable;

public record MojangProfileDto(@Nullable String name, @Nullable String id) {
    public boolean isFullAbsent() {
        return name == null && id == null;
    }
}
