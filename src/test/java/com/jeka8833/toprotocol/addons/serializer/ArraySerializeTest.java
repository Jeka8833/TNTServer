package com.jeka8833.toprotocol.addons.serializer;

import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ArraySerializeTest {

    @Test
    void readByteArray() {
        OutputByteArray outputByteArray = new OutputByteArray(Integer.MAX_VALUE);
        VarLengthSerialize.writeVarInt(outputByteArray, Integer.MAX_VALUE);
        outputByteArray.writeBytes(new byte[]{1, 2, 3});

        InputByteArray inputByteArray = new InputByteArray(outputByteArray.toByteArray());

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ArraySerialize.readByteArray(inputByteArray));
    }

    @Test
    void writeByteArray() {

    }
}