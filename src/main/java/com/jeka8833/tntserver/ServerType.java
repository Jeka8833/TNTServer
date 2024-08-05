package com.jeka8833.tntserver;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum ServerType {
    HYPIXEL("Hypixel"),
    TNT_COMMUNITY("Odyssey"),
    UNKNOWN("Unknown");

    private final String[] serverBrands;

    ServerType(String... serverBrands) {
        this.serverBrands = serverBrands;
    }

    public static ServerType getServer(String serverBrand) {
        for (ServerType type : ServerType.values()) {
            for (String brand : type.serverBrands) {
                if (brand.equalsIgnoreCase(serverBrand)) {
                    return type;
                }
            }
        }

        return UNKNOWN;
    }

    @NotNull
    @Contract(pure = true)
    public String getActualServerBrand() {
        return serverBrands[0];
    }
}
