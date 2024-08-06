package com.jeka8833.tntserver.metric.providers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.loki4j.logback.json.AbstractFieldJsonProvider;
import com.github.loki4j.logback.json.JsonEventWriter;
import com.jeka8833.tntserver.metric.GrafanaProvider;
import com.jeka8833.tntserver.requester.HypixelCache;

public final class RequesterCacheProvider extends AbstractFieldJsonProvider {
    @Override
    public boolean writeTo(JsonEventWriter writer, ILoggingEvent event, boolean startWithSeparator) {
        if (event.getMessage().equals(GrafanaProvider.SEND_METRICS)) {
            CacheStats cacheStats = HypixelCache.getStatistic();
            if (cacheStats.equals(CacheStats.empty())) return true;

            if (startWithSeparator) writer.writeFieldSeparator();

            writer.writeNumericField("cache_hitCount", cacheStats.hitCount());
            writer.writeFieldSeparator();

            writer.writeNumericField("cache_missCount", cacheStats.missCount());
            writer.writeFieldSeparator();

            writer.writeNumericField("cache_loadSuccessCount", cacheStats.loadSuccessCount());
            writer.writeFieldSeparator();

            writer.writeNumericField("cache_loadFailureCount", cacheStats.loadFailureCount());
            writer.writeFieldSeparator();

            writer.writeNumericField("cache_totalLoadTime", cacheStats.totalLoadTime());
            writer.writeFieldSeparator();

            writer.writeNumericField("cache_evictionCount", cacheStats.evictionCount());
            writer.writeFieldSeparator();

            writer.writeNumericField("cache_evictionWeight", cacheStats.evictionWeight());
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
