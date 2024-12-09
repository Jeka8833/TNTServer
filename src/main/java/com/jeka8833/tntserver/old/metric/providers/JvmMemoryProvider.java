package com.jeka8833.tntserver.old.metric.providers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.loki4j.logback.json.AbstractFieldJsonProvider;
import com.github.loki4j.logback.json.JsonEventWriter;
import com.jeka8833.tntserver.old.metric.GrafanaProvider;

public final class JvmMemoryProvider extends AbstractFieldJsonProvider {
    @Override
    public boolean writeTo(JsonEventWriter writer, ILoggingEvent event, boolean startWithSeparator) {
        if (event.getMessage().equals(GrafanaProvider.SEND_METRICS)) {
            if (startWithSeparator) writer.writeFieldSeparator();

            Runtime instance = Runtime.getRuntime();

            writer.writeNumericField("jvm_totalMemory", instance.totalMemory());
            writer.writeFieldSeparator();

            writer.writeNumericField("jvm_freeMemory", instance.freeMemory());
        }

        return true;
    }

    @Override
    protected void writeExactlyOneField(JsonEventWriter jsonEventWriter, ILoggingEvent iLoggingEvent) {
        throw new UnsupportedOperationException();
    }
}
