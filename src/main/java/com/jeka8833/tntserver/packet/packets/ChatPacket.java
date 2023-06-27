package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.UUID;

public class ChatPacket implements Packet {

    private UUID user;
    private String text;

    @SuppressWarnings("unused")
    public ChatPacket() {
    }

    public ChatPacket(UUID user, String text) {
        this.user = user;
        this.text = text;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeUUID(user);
        stream.writeUTF(text);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        text = stream.readUTF();
    }

    @Override
    public void serverProcess(WebSocket socket, Player user) {
        if (user == null) {
            socket.close();
            return;
        }

        // Fixed Log4j exploit
        String fixedText = text.replaceAll("\\$\\{.+}", "***")
                .replaceAll(
                        "(http|ftp|https)://([\\w_\\-]+(?:\\.[\\w_\\-]+)+)([\\w.,@?^=%&:/~+#\\-]*[\\w@?^=%&/~+#\\-])",
                        "***");


        Main.serverBroadcast(new ChatPacket(user.uuid, fixedText));
    }
}
