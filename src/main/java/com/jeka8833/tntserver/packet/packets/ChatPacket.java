package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatPacket implements Packet {

    private static final long DIFFERENT_MESSAGE_TIMING = 500;
    private static final long SAME_MESSAGE_TIMING = 3_000;

    private static final Map<UUID, MessageTiming> MESSAGE_TIMING_MAP = new ConcurrentHashMap<>();
    private static final Logger logger = LogManager.getLogger(ChatPacket.class);


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

        int hash = fixedText.hashCode();

        clearMap();
        MessageTiming messageTiming = MESSAGE_TIMING_MAP.get(user.uuid);

        if (messageTiming != null && (hash == messageTiming.messageHash ||
                messageTiming.timeSend + DIFFERENT_MESSAGE_TIMING > System.currentTimeMillis())) {
            Main.serverSend(socket,
                    // Color genius...
                    new ChatPacket(user.uuid, "§cYou §care §csending §cmessages §ctoo §cfast, §cyour §cmessage " +
                            "§chas §cnot §cbeen §cdelivered. §cOnly §cyou §ccan §csee §cthis §cmessage."));
            return;
        }

        MESSAGE_TIMING_MAP.put(user.uuid, new MessageTiming(hash, System.currentTimeMillis()));
        sendMessage(user, fixedText);
    }

    private static void sendMessage(Player player, String message) {
        var packet = new ChatPacket(player.uuid, message);

        try (final PacketOutputStream stream = new PacketOutputStream()) {
            packet.write(stream);
            ByteBuffer send = stream.getByteBuffer(packet.getClass());

            for (WebSocket client : Main.server.getConnections()) {
                try {
                    Player toPlayer = PlayersDatabase.getUser(client.getAttachment());
                    if (toPlayer == null || !player.serverType.equals(toPlayer.serverType)) continue;

                    if (client.isOpen()) client.send(send);
                } catch (Exception e) {
                    logger.error("Fail send packet:", e);
                }
            }
        } catch (Exception e) {
            logger.error("Fail generate packet:", e);
        }
    }

    private static void clearMap() {
        long currentTime = System.currentTimeMillis();

        MESSAGE_TIMING_MAP.values().removeIf(messageTiming ->
                messageTiming.timeSend() + SAME_MESSAGE_TIMING < currentTime);
    }

    private record MessageTiming(int messageHash, long timeSend) {
    }
}
