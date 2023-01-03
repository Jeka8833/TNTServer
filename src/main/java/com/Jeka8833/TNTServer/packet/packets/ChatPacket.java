package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.UUID;

public class ChatPacket implements Packet {

    private UUID user;
    private String text;

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
    public void serverProcess(WebSocket socket, TNTUser user) {
        // Fixed Log4j exploit
        String fixedText = text.replaceAll("\\$\\{.+}", "***")
                .replaceAll(
                        "(http|ftp|https)://([\\w_\\-]+(?:\\.[\\w_\\-]+)+)([\\w.,@?^=%&:/~+#\\-]*[\\w@?^=%&/~+#\\-])",
                        "***");


        Main.serverBroadcast(new ChatPacket(user.user, fixedText));
    }
}
