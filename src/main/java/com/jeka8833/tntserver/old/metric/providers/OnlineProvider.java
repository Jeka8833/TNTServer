package com.jeka8833.tntserver.old.metric.providers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.loki4j.logback.json.AbstractFieldJsonProvider;
import com.github.loki4j.logback.json.JsonEventWriter;
import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.metric.GrafanaProvider;
import com.jeka8833.tntserver.old.mojang.MojangAPI;
import org.java_websocket.WebSocket;

import java.util.Collection;
import java.util.UUID;

public class OnlineProvider extends AbstractFieldJsonProvider {
    @Override
    public boolean writeTo(JsonEventWriter writer, ILoggingEvent event, boolean startWithSeparator) {
        if (event.getMessage().equals(GrafanaProvider.SEND_METRICS)) {
            if (startWithSeparator) writer.writeFieldSeparator();

            int players = 0;
            int unknown = 0;
            Collection<WebSocket> connections = TNTServer.server.getConnections();
            for (WebSocket connection : connections) {
                UUID uuid = connection.getAttachment();

                if (uuid == null) {
                    unknown++;
                } else if (MojangAPI.isPlayer(uuid)) {
                    players++;
                }
            }

            writer.writeNumericField("server_online_players", players);
            writer.writeFieldSeparator();

            writer.writeNumericField("server_online_bots", connections.size() - players - unknown);
            writer.writeFieldSeparator();

            writer.writeNumericField("server_online_unknowns", unknown);
        }

        return true;
    }

    @Override
    protected void writeExactlyOneField(JsonEventWriter jsonEventWriter, ILoggingEvent iLoggingEvent) {
        throw new UnsupportedOperationException();
    }
}