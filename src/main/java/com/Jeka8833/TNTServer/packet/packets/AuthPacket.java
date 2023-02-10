package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.dataBase.TNTClientDBManager;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import com.Jeka8833.TNTServer.util.Util;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class AuthPacket implements Packet {

    public static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

    public UUID user;
    public UUID key;
    public String version;

    private int closeCode;
    private String closeMessage;

    public AuthPacket() {

    }

    public AuthPacket(int closeCode, String closeMessage) {
        this.closeCode = closeCode;
        this.closeMessage = closeMessage;
    }

    @Override
    public void write(final PacketOutputStream stream) throws IOException {
        stream.write(closeCode);
        stream.writeUTF(closeMessage);
    }

    @Override
    public void read(final PacketInputStream stream) throws IOException {
        user = stream.readUUID();
        key = stream.readUUID();
        version = stream.readUTF();
    }

    @Override
    public void serverProcess(final WebSocket socket, final TNTUser user) {
        if (executor.getQueue().size() == executor.getMaximumPoolSize()) {
            Main.serverSend(socket, new AuthPacket(5, "This server is overloading"));
            socket.close();
        } else {
            if (this.user == null || this.key == null) {
                Main.serverSend(socket, new AuthPacket(Util.FAIL_CONNECTION,
                        "Internal server error"));
                socket.close();
            }

            TNTUser.uuid2User.remove(this.user);

            executor.execute(() -> {
                int status = Util.checkKey(this.user, key);
                switch (status) {
                    case Util.GOOD_AUTH -> {
                        socket.setAttachment(this.user);

                        TNTClientDBManager.readOrCashUser(this.user, tntUser -> {
                            final TNTUser account = tntUser == null ? new TNTUser(this.user, this.key, this.version) : tntUser;
                            account.key = key;
                            account.version = version;
                            account.timeLogin = System.currentTimeMillis();
                            account.heartBeat();
                            TNTUser.addUser(account);

                            TNTClientDBManager.writeUser(this.user, null);

                            Main.serverSend(socket, new BlockModulesPacket(account.forceBlock, account.forceActive));
                        });
                    }
                    case Util.FAIL_AUTH -> {
                        Main.serverSend(socket, new AuthPacket(Util.FAIL_AUTH,
                                "Fail authentication, incorrect user"));
                        socket.close();
                    }
                    case Util.FAIL_CONNECTION -> {
                        Main.serverSend(socket, new AuthPacket(Util.FAIL_CONNECTION,
                                "Internal server error"));
                        socket.close();
                    }
                    case Util.KEY_THROTTLING -> {
                        Main.serverSend(socket, new AuthPacket(Util.KEY_THROTTLING,
                                "Key throttling"));
                        socket.close();
                    }
                    case Util.FAIL_PARSE -> {
                        Main.serverSend(socket, new AuthPacket(Util.FAIL_PARSE,
                                "Fail process the server data"));
                        socket.close();
                    }
                }
            });
        }
    }
}
