package com.jeka8833.tntserver.old.metric.providers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.loki4j.logback.json.AbstractFieldJsonProvider;
import com.github.loki4j.logback.json.JsonEventWriter;
import com.jeka8833.tntserver.old.metric.GrafanaProvider;
import com.jeka8833.tntserver.old.requester.balancer.RequestBalancer;
import com.jeka8833.tntserver.old.requester.balancer.node.BalancerNode;
import com.jeka8833.tntserver.old.requester.balancer.node.LocalNode;

public class NodesProvider extends AbstractFieldJsonProvider {
    @Override
    public boolean writeTo(JsonEventWriter writer, ILoggingEvent event, boolean startWithSeparator) {
        if (event.getMessage().equals(GrafanaProvider.SEND_METRICS)) {
            if (startWithSeparator) writer.writeFieldSeparator();

            BalancerNode[] nodes = RequestBalancer.getNodes();
            long localNode = 0;
            long remoteNode = 0;
            for (BalancerNode node : nodes) {
                if (node instanceof LocalNode) {
                    localNode += node.getAvailable();
                } else {
                    remoteNode += node.getAvailable();
                }
            }

            writer.writeNumericField("node_local", localNode);
            writer.writeFieldSeparator();

            writer.writeNumericField("node_remote", remoteNode);
        }

        return true;
    }

    @Override
    protected void writeExactlyOneField(JsonEventWriter jsonEventWriter, ILoggingEvent iLoggingEvent) {
        throw new UnsupportedOperationException();
    }
}