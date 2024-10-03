package com.jeka8833.tntserver.metric.providers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.loki4j.logback.json.AbstractFieldJsonProvider;
import com.github.loki4j.logback.json.JsonEventWriter;
import com.jeka8833.tntserver.metric.GrafanaProvider;
import com.jeka8833.tntserver.requester.HypixelCache;

import java.util.concurrent.atomic.AtomicLong;

public final class RequesterCacheProvider extends AbstractFieldJsonProvider {

    public static final AtomicLong requestedCount = new AtomicLong();
    public static final AtomicLong updateCount = new AtomicLong();
    public static final AtomicLong missCount = new AtomicLong();
    public static final AtomicLong loadSuccessNewCount = new AtomicLong();
    public static final AtomicLong loadSuccessSameCount = new AtomicLong();

    @Override
    public boolean writeTo(JsonEventWriter writer, ILoggingEvent event, boolean startWithSeparator) {
        if (event.getMessage().equals(GrafanaProvider.SEND_METRICS)) {
            if (startWithSeparator) writer.writeFieldSeparator();

            writer.writeNumericField("cache_requestedCount", requestedCount.get());
            writer.writeFieldSeparator();

            writer.writeNumericField("cache_updateCount", updateCount.get());
            writer.writeFieldSeparator();

            writer.writeNumericField("cache_missCount", missCount.get());
            writer.writeFieldSeparator();

            writer.writeNumericField("cache_loadSuccessCount", loadSuccessNewCount.get());
            writer.writeFieldSeparator();

            writer.writeNumericField("cache_loadSuccessSameCount", loadSuccessSameCount.get());
            writer.writeFieldSeparator();

            writer.writeNumericField("cache_size", HypixelCache.size());
        }

        return true;
    }

    @Override
    protected void writeExactlyOneField(JsonEventWriter jsonEventWriter, ILoggingEvent iLoggingEvent) {
        throw new UnsupportedOperationException();
    }
}
