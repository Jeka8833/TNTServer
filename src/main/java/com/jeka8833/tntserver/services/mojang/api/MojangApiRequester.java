package com.jeka8833.tntserver.services.mojang.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeka8833.tntserver.services.mojang.MojangProfile;
import com.jeka8833.tntserver.services.mojang.dtos.MojangProfileDto;
import com.jeka8833.tntserver.services.mojang.mappers.MojangProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class MojangApiRequester {
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final MojangProfileMapper mojangProfileMapper;

    @Cacheable(value = "mojang", unless = "!#result.fullCompleted")
    public MojangProfile getProfile(String name) {
        Request request = new Request.Builder()
                .url("https://api.mojang.com/users/profiles/minecraft/" + name)
                .build();

        try {
            return makeRequest(request);
        } catch (Exception e) {
            log.warn("Failed to get profile: {}", name, e);
        }

        return new MojangProfile(name, null);
    }

    @Cacheable(value = "mojang", unless = "!#result.fullCompleted")
    public MojangProfile getProfile(UUID uuid) {
        Request request = new Request.Builder()
                .url("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)
                .build();

        try {
            return makeRequest(request);
        } catch (Exception e) {
            log.warn("Failed to get profile: {}", uuid, e);
        }

        return new MojangProfile(null, uuid);
    }

    private MojangProfile makeRequest(Request request) throws IOException {
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            try (ResponseBody responseBody = response.body();
                 InputStream inputStream = responseBody.byteStream()) {
                MojangProfileDto dto = objectMapper.readValue(inputStream, MojangProfileDto.class);
                if (dto == null || dto.isFullAbsent()) throw new IOException("Profile not found: " + response);

                return mojangProfileMapper.toMojangProfile(dto);
            }
        }
    }

}
