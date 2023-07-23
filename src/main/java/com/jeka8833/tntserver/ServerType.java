package com.jeka8833.tntserver;

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
}
