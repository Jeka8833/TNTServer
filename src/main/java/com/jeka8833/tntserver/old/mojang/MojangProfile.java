package com.jeka8833.tntserver.old.mojang;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class MojangProfile {
    private final @NotNull Optional<String> name;
    private final @NotNull Optional<UUID> uuid;

    private boolean isNotFound = false;

    public MojangProfile() {
        this(Optional.empty(), Optional.empty());
    }

    public MojangProfile(@Nullable String name) {
        this(Optional.ofNullable(name), Optional.empty());
    }

    public MojangProfile(@Nullable UUID uuid) {
        this(Optional.empty(), Optional.ofNullable(uuid));
    }

    public MojangProfile(@Nullable String name, @Nullable UUID uuid) {
        this.name = Optional.ofNullable(name);
        this.uuid = Optional.ofNullable(uuid);
    }

    public void setNotFound() {
        isNotFound = true;
    }

    @Contract(pure = true)
    public boolean isComplete() {
        return name.isPresent() && uuid.isPresent();
    }

    @Contract(pure = true)
    public boolean isNotFound() {
        return isNotFound;
    }
}
