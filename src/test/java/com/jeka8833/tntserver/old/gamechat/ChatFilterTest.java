package com.jeka8833.tntserver.old.gamechat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatFilterTest {

    @Test
    void stripControlCodes() {
        assertEquals("Hello", ChatFilter.stripControlCodes("§fHello"));
        assertEquals("Hello", ChatFilter.stripControlCodes("§fHello§r"));
        assertEquals("Hello", ChatFilter.stripControlCodes("§fHello§r§f"));

        assertEquals("Hello", ChatFilter.stripControlCodes("&fHello"));
        assertEquals("Hello", ChatFilter.stripControlCodes("&fHello&r"));
        assertEquals("Hello", ChatFilter.stripControlCodes("&fHello&r&f"));
    }
}