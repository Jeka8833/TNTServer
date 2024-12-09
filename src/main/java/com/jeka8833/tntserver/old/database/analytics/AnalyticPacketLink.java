package com.jeka8833.tntserver.old.database.analytics;

public record AnalyticPacketLink(AnalyticGroup group, Class<? extends AnalyticPacket> packetClass) {
}
