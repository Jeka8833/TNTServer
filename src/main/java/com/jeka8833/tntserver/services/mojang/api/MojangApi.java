package com.jeka8833.tntserver.services.mojang.api;

import com.jeka8833.tntserver.services.mojang.MojangProfile;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MojangApi {
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{1,16}$");

    private final MojangApiRequester apiRequester;
    private final MojangApiAsyncWrapper mojangApiAsyncWrapper;

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public Map<UUID, MojangProfile> getProfiles(@NotNull Set<UUID> playersUUIDs) {
        Collection<CompletableFuture<MojangProfile>> futures = new ArrayList<>(playersUUIDs.size());
        Map<UUID, MojangProfile> profiles = new HashMap<>(playersUUIDs.size());

        for (UUID uuid : playersUUIDs) {
            futures.add(getProfileAsync(uuid));

            profiles.put(uuid, new MojangProfile(null, uuid));
        }

        for (CompletableFuture<MojangProfile> future : futures) {
            try {
                MojangProfile profile = future.join();

                profiles.put(profile.uuid(), profile);
            } catch (Exception ignored) {
            }
        }

        return profiles;
    }

    public CompletableFuture<MojangProfile> getProfileAsync(@Nullable String playerName) {
        if (isPlayerName(playerName)) return mojangApiAsyncWrapper.getProfileAsync(playerName);

        return CompletableFuture.completedFuture(new MojangProfile(playerName, null));
    }

    public CompletableFuture<MojangProfile> getProfileAsync(@Nullable UUID playerUUID) {
        if (isPlayerUUID(playerUUID)) return mojangApiAsyncWrapper.getProfileAsync(playerUUID);

        return CompletableFuture.completedFuture(new MojangProfile(null, playerUUID));
    }

    @NotNull
    public MojangProfile getProfile(@Nullable String playerName) {
        if (isPlayerName(playerName)) return apiRequester.getProfile(playerName);

        return new MojangProfile(playerName, null);
    }

    @NotNull
    public MojangProfile getProfile(@Nullable UUID playerUUID) {
        if (isPlayerUUID(playerUUID)) return apiRequester.getProfile(playerUUID);

        return new MojangProfile(null, playerUUID);
    }

    @Contract(pure = true)
    public static boolean isPlayerName(@Nullable String playerName) {
        return playerName != null && NAME_PATTERN.matcher(playerName).matches();
    }

    @Contract(pure = true)
    public static boolean isPlayerUUID(@Nullable UUID playerUUID) {
        return playerUUID != null && playerUUID.version() == 4 && playerUUID.variant() == 2;
    }
}
