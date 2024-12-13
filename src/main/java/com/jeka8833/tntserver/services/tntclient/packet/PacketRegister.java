package com.jeka8833.tntserver.services.tntclient.packet;

import com.jeka8833.tntserver.services.tntclient.packet.serverbound.WebTokenUserServerbound;
import com.jeka8833.toprotocol.core.register.PacketRegistryBuilder;

public class PacketRegister {

    public static PacketRegistryBuilder<Byte, ClientBoundPacket, ServerBoundPacket, Integer> getPacketRegistry() {
        return new PacketRegistryBuilder<Byte, ClientBoundPacket, ServerBoundPacket, Integer>()
                .register((byte) 1, builder -> builder
                        .serverbound(WebTokenUserServerbound.class, WebTokenUserServerbound::new)
                )
                ;
    }

}
