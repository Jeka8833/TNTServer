package com.jeka8833.tntserver.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public final class StaticJSON {
    @Nullable
    private static ObjectMapper objectMapper;

    StaticJSON(@Nullable ObjectMapper objectMapper) {
        StaticJSON.objectMapper = objectMapper;
    }

    @NotNull
    @Contract(pure = true)
    public static ObjectMapper getObjectMapper() {
        return Objects.requireNonNullElseGet(objectMapper, ObjectMapper::new); // If something called before Spring boot started
    }
}
