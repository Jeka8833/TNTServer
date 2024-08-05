package com.jeka8833.tntserver.mojang;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

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

    public MojangProfile(@NotNull Optional<@NotNull String> name, @NotNull Optional<@NotNull UUID> uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public void setNotFound() {
        isNotFound = true;
    }

    @NotNull
    @Contract(pure = true)
    public Optional<String> getName() {
        return name;
    }

    @NotNull
    @Contract(pure = true)
    public Optional<UUID> getUUID() {
        return uuid;
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
