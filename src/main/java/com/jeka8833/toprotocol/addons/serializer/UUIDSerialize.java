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

    @Contract(mutates = "param1")
    public static void writeUUID(@NotNull OutputByteArray outputByteArray, @NotNull UUID uuid) {
        outputByteArray.writeLong(uuid.getMostSignificantBits());
        outputByteArray.writeLong(uuid.getLeastSignificantBits());
    }
}
