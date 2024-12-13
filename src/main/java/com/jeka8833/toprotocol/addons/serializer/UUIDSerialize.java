package com.jeka8833.toprotocol.addons.serializer;

import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class UUIDSerialize {
    private UUIDSerialize() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Contract(value = "_ -> new", mutates = "param1")
    public static UUID readUUID(@NotNull InputByteArray inputByteArray) {
        return new UUID(inputByteArray.readLong(), inputByteArray.readLong());
    }

    @NotNull
    @Contract(value = "_, _ -> param1", mutates = "param1")
    public static OutputByteArray writeUUID(@NotNull OutputByteArray outputByteArray, @NotNull UUID uuid) {
        outputByteArray.writeLong(uuid.getMostSignificantBits());
        outputByteArray.writeLong(uuid.getLeastSignificantBits());

        return outputByteArray;
    }
}
