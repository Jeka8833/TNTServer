package com.jeka8833.tntserver.services.tntclient.user;

import com.jeka8833.tntserver.services.tntclient.TNTServerWebSocket;
import com.jeka8833.tntserver.services.tntclient.packet.ClientBoundPacket;
import com.jeka8833.toprotocol.core.serializer.ArrayOutputSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@Slf4j
@Getter
@AllArgsConstructor
public abstract class UserBase {
    private final @NotNull TNTServerWebSocket tntServerWebSocket;
    private final @NotNull WebSocket userWebsocketChannel;
    private final int protocolVersion;
    private final @NotNull Set<String> privileges;

    @Contract(value = "null -> false", pure = true)
    public boolean hasPrivilege(@Nullable String privilege) {
        if (privilege == null) return false;

        return privileges.contains(privilege);
    }

    public boolean sendPacket(@NotNull ClientBoundPacket packet) {
        Byte clientBoundPacketKey = tntServerWebSocket.getRegistry().getClientBoundPacketKey(packet.getClass());
        if (clientBoundPacketKey == null) {
            log.error("Unknown packet type: {}", packet);

            return false;
        }

        ArrayOutputSerializer arrayOutputSerializer = new ArrayOutputSerializer(tntServerWebSocket.getMaxFrameSize());
        try {
            arrayOutputSerializer.writeByte(clientBoundPacketKey);
            packet.write(arrayOutputSerializer, protocolVersion);

            userWebsocketChannel.send(arrayOutputSerializer.array());

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void onDisconnect() {
    }

    public void disconnect() {
        userWebsocketChannel.close();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof UserBase userBase)) return false;

        return userWebsocketChannel.equals(userBase.userWebsocketChannel);
    }

    @Override
    public int hashCode() {
        return userWebsocketChannel.hashCode();
    }

    @Override
    public String toString() {
        return "UserBase{" +
                "ip=" + userWebsocketChannel.getRemoteSocketAddress() +
                ", protocolVersion=" + protocolVersion +
                ", privileges=" + privileges +
                '}';
    }
}
