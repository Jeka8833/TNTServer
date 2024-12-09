package com.jeka8833.tntserver.old.metric;

import com.github.loki4j.logback.JavaHttpSender;
import com.jeka8833.tntserver.old.Main;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class GrafanaProvider extends JavaHttpSender {
    public static final String SEND_METRICS = "metrics";

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    public GrafanaProvider() {
        if (Main.INSTANCE.grafanaMetricsInterval.isEmpty()) {
            log.warn("Grafana Metrics Interval is not set. Metrics will not be sent.");
            return;
        }

        if (Main.INSTANCE.grafanaUrl.isEmpty()) {
            log.warn("Grafana URL is not set. Metrics will not be sent.");
            return;
        }

        if (Main.INSTANCE.grafanaUsername.isEmpty()) {
            log.warn("Grafana Username is not set. Metrics will not be sent.");
            return;
        }

        if (Main.INSTANCE.grafanaPassword.isEmpty()) {
            log.warn("Grafana Password is not set. Metrics will not be sent.");

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
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> log.info(SEND_METRICS),
                interval.toNanos(), interval.toNanos(), TimeUnit.NANOSECONDS);
    }
}
