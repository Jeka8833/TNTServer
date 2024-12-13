package com.jeka8833.tntserver.configs;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {
    private static final AtomicInteger counter = new AtomicInteger();

    private final ScheduledExecutorService scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "global-single-threaded-scheduler-" + counter.getAndIncrement());
                thread.setDaemon(true);

                return thread;
            });

    @Bean(name = "virtual-executor")
    public AsyncTaskExecutor taskExecutor() {
        var taskExecutor = new SimpleAsyncTaskExecutor("global-virtualthread-executor");
        taskExecutor.setVirtualThreads(true);
        taskExecutor.setDaemon(true);
        taskExecutor.setTaskDecorator(runnable -> () -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                log.warn("Failed to execute task", e);
            }
        });

        return taskExecutor;
    }

    @Bean("single-threaded-scheduler")
    public ScheduledExecutorService scheduledExecutorService() {
        return scheduledExecutorService;
    }

    @EventListener(ContextClosedEvent.class)
    public void onClose(@NotNull ContextClosedEvent ignoredEvent) {
        for (Runnable remainingTask : scheduledExecutorService.shutdownNow()) {
            if (remainingTask instanceof Future<?> future) {
                future.cancel(true);
            }
        }
    }
}
