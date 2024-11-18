package com.jeka8833.tntserver.database.analytics;

import com.jeka8833.tntserver.Main;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public final class AnalyticManager {
    private static final long SLEEP_TIME = 10_000L;

    private final Path basePath;
    private final long maxFolderSize;
    private final Collection<AnalyticGroup> analyticGroups = new ArrayList<>();
    private boolean isDirectoryOverflow = false;

    public AnalyticManager(Path basePath, long maxFolderSize) {
        this.basePath = basePath;
        this.maxFolderSize = maxFolderSize;
    }

    @Nullable
    public static AnalyticManager createAndStart() {
        if (Main.INSTANCE.analycitcsPath.isEmpty()) {
            log.warn("Analytic path is not set. Analytic will not be started.");

            return null;
        }

        var analytic = new AnalyticManager(Main.INSTANCE.analycitcsPath.get(), Main.INSTANCE.analyticsMaxFolderSize);
        analytic.start();

        return analytic;
    }

    private static boolean isOverflowFolder(Path folder, long maxFolderSize) {
        final AtomicLong directorySize = new AtomicLong();
        try {
            Files.walkFileTree(folder, Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (directorySize.addAndGet(attrs.size()) >= maxFolderSize) {
                                return FileVisitResult.TERMINATE;
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            log.warn("Failed to get file size: {}", file, exc);

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                            if (exc != null) {
                                log.warn("Failed to get directory size: {}", dir, exc);
                            }

                            return FileVisitResult.CONTINUE;
                        }


                    });
        } catch (Exception e) {
            log.error("Failed to get directory size: {}", folder, e);

            return false;
        }

        return directorySize.get() >= maxFolderSize;
    }

    public void start() {
        Thread thread = new Thread(() -> {
            if (!Files.isDirectory(basePath)) {
                try {
                    Files.createDirectories(basePath);
                } catch (IOException e) {
                    log.error("Failed to create analytic folder, stop analytic", e);

                    return;
                }
            }

            while (!Thread.interrupted()) {
                try {
                    isDirectoryOverflow = isOverflowFolder(basePath, maxFolderSize);

                    if (isDirectoryOverflow()) {
                        log.warn("Base analytic folder size is too big, please check it.");

                        for (AnalyticGroup analyticGroup : analyticGroups) {
                            analyticGroup.clear();
                        }

                        analyticGroups.clear();

                        return;
                    }

                    for (AnalyticGroup analyticGroup : analyticGroups) {
                        analyticGroup.process(basePath);
                    }

                    //noinspection BusyWait
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    log.error("Failed to write analytic", e);

                    try {
                        //noinspection BusyWait
                        Thread.sleep(SLEEP_TIME);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            }
        });

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    public void addGroup(AnalyticGroup analyticGroup) {
        analyticGroup.addManager(this);

        analyticGroups.add(analyticGroup);
    }

    public boolean isDirectoryOverflow() {
        return isDirectoryOverflow;
    }
}
