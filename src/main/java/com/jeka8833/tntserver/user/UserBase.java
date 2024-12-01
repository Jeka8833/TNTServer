package com.jeka8833.tntserver.user;

import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@Slf4j
@Getter
@ToString
@AllArgsConstructor
public abstract class UserBase {
    private final @NotNull WebSocket socket;
    private final int protocolVersion;
    private final @NotNull Set<String> privileges;

    @Contract(value = "null -> false", pure = true)
    public boolean hasPrivilege(@Nullable String privilege) {
        if (privilege == null) return false;

        return privileges.contains(privilege);
    }

    public boolean sendPacket(@NotNull Packet packet) {
        try (PacketOutputStream stream = new PacketOutputStream()){
            packet.write(stream, protocolVersion);

            try {
                socket.send(stream.getByteBuffer(packet.getClass()));

                return true;
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.error("Fail to encode packet, internal bug", e);
        }

        return false;
    }

    public void disconnect() {
        socket.close();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof UserBase userBase)) return false;

        return socket.equals(userBase.socket);
    }

    @Override
    public int hashCode() {
        return socket.hashCode();
    }
}
