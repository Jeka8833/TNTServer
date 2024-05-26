package com.jeka8833.tntserver.database.analytics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

public final class AnalyticManager {
    private static final long SLEEP_TIME = 10_000L;
    private static final Logger LOGGER = LogManager.getLogger(AnalyticManager.class);

    private final Path basePath;
    private final long maxFolderSize;
    private boolean isDirectoryOverflow = false;
    private final Collection<AnalyticGroup> analyticGroups = new ArrayList<>();

    public AnalyticManager(Path basePath, long maxFolderSize) {
        this.basePath = basePath;
        this.maxFolderSize = maxFolderSize;
    }

    public void start() {
        Thread thread = new Thread(() -> {
            if (!Files.isDirectory(basePath)) {
                try {
                    Files.createDirectories(basePath);
                } catch (IOException e) {
                    LOGGER.error("Failed to create analytic folder, stop analytic", e);

                    return;
                }
            }

            while (!Thread.interrupted()) {
                try {
                    isDirectoryOverflow = isOverflowFolder(basePath, maxFolderSize);

                    if (isDirectoryOverflow()) {
                        LOGGER.warn("Base analytic folder size is too big, please check it.");

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
                    LOGGER.error("Failed to write analytic", e);

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
                            LOGGER.warn("Failed to get file size: " + file, exc);

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                            if (exc != null) {
                                LOGGER.warn("Failed to get directory size: " + dir, exc);
                            }

                            return FileVisitResult.CONTINUE;
                        }


                    });
        } catch (Exception e) {
            LOGGER.error("Failed to get directory size: " + folder, e);

            return false;
        }

        return directorySize.get() >= maxFolderSize;
    }
}
