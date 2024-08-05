package com.jeka8833.tntserver.metric;

import com.github.loki4j.logback.JavaHttpSender;
import com.jeka8833.tntserver.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class GrafanaProvider extends JavaHttpSender {
    public static final String SEND_METRICS = "metrics";


    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private static final Logger LOGGER = LoggerFactory.getLogger(GrafanaProvider.class);

    public GrafanaProvider() {
        if (Main.INSTANCE.grafanaMetricsInterval.isEmpty()) {
            LOGGER.warn("Grafana Metrics Interval is not set. Metrics will not be sent.");
            return;
        }

        if (Main.INSTANCE.grafanaUrl.isEmpty()) {
            LOGGER.warn("Grafana URL is not set. Metrics will not be sent.");
            return;
        }

        if (Main.INSTANCE.grafanaUsername.isEmpty()) {
            LOGGER.warn("Grafana Username is not set. Metrics will not be sent.");
            return;
        }

        if (Main.INSTANCE.grafanaPassword.isEmpty()) {
            LOGGER.warn("Grafana Password is not set. Metrics will not be sent.");

            return;
        }

        setUrl(Main.INSTANCE.grafanaUrl.get());

        var auth = new BasicAuth();
        auth.setUsername(Main.INSTANCE.grafanaUsername.get());
        auth.setPassword(Main.INSTANCE.grafanaPassword.get());

        setAuth(auth);

        startTask(Main.INSTANCE.grafanaMetricsInterval.get());
    }

    private void startTask(Duration interval) {
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> LOGGER.info(SEND_METRICS),
                interval.toNanos(), interval.toNanos(), TimeUnit.NANOSECONDS);
    }
}
