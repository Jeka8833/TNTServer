package com.jeka8833.tntserver.metric;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public final class GrafanaMessageFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent iLoggingEvent) {
        if (iLoggingEvent.getMessage().equals(GrafanaProvider.SEND_METRICS)) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}
