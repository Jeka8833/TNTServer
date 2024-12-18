package com.jeka8833.toprotocol.addons.serializer;

import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.nio.charset.StandardCharsets;

public final class StringSerialize {
    private StringSerialize() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Contract(value = "_ -> new", mutates = "param1")
    public static String readUTF8(@NotNull InputByteArray inputByteArray) {
        return new String(ArraySerialize.readByteArray(inputByteArray), StandardCharsets.UTF_8);
    }

    @Contract(mutates = "param1")
    public static void writeUTF8(@NotNull OutputByteArray outputByteArray, @NotNull String string) {
        ArraySerialize.writeByteArray(outputByteArray, string.getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    @Contract(value = "_ -> new", mutates = "param1")
    public static String readModifiedUTF8(@NotNull InputByteArray inputByteArray) {
        DataInputStream dataInputStream = new DataInputStream(inputByteArray);

        try {
            return dataInputStream.readUTF();
        } catch (IndexOutOfBoundsException exception) {
            throw exception;
        } catch (EOFException exception) {
            throw new IndexOutOfBoundsException("Not enough data to read string");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Contract(mutates = "param1")
    public static void writeModifiedUTF8(@NotNull OutputByteArray outputByteArray, @NotNull String string) {
        DataOutputStream dataOutputStream = new DataOutputStream(outputByteArray);

        try {
            dataOutputStream.writeUTF(string);
        } catch (IndexOutOfBoundsException exception) {
            throw exception;
        } catch (Exception ioException) {
            throw new RuntimeException(ioException);
        }
    }
}
