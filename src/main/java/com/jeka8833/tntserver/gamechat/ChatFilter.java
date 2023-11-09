package com.jeka8833.tntserver.gamechat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ChatFilter {
    private static final long DIFFERENT_MESSAGE_TIMING = 500;
    private static final long SAME_MESSAGE_TIMING = 10_000;
    private static final long TIME_TO_CLEAR = 60_000;
    private static final String REPLACEMENT_TEXT = "***";
    private static final Pattern LOG4J_EXPLOIT_PATTERN = Pattern.compile("\\$\\{.+}");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(http|ftp|https)://([\\w_\\-]+(?:\\.[\\w_\\-]+)+)([\\w.,@?^=%&:/~+#\\-]*[\\w@?^=%&/~+#\\-])");


    private static final Set<String> dictionary = new HashSet<>();
    private static final Logger logger = LogManager.getLogger(ChatFilter.class);
    private static final Map<UUID, Deque<MessageTiming>> minuteTimingList = new ConcurrentHashMap<>();

    public static void loadDictionaries(String folderPath) {
        new Thread(() -> {
            try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
                paths
                        .filter(Files::isRegularFile)
                        .forEach(ChatFilter::readFile);
            } catch (IOException e) {
                logger.warn("Error load dictionary: " + folderPath, e);
            }
            logger.info("Dictionary loaded: " + dictionary.size() + " words");
        }, "Dictionary loader").start();
    }

    @Nullable
    public static String filter(UUID sender, String text) {
        String filtered = text.strip();
        filtered = filterLog4jExploit(filtered);
        filtered = urlFilter(filtered);
        filtered = filterSwearWords(filtered);

        if (isUserFlood(sender, filtered)) return null;

        logger.info(sender + ": " + filtered);

        return filtered;
    }

    public static void clearOld() {
        long currentTime = System.currentTimeMillis();

        Iterator<Deque<MessageTiming>> iterator = minuteTimingList.values().iterator();
        while (iterator.hasNext()) {
            Deque<MessageTiming> messageTimings = iterator.next();

            MessageTiming message;
            while ((message = messageTimings.peek()) != null && message.messageTime + TIME_TO_CLEAR < currentTime) {
                messageTimings.remove();
            }

            // Thread collision may occur, but it is not critical
            if (messageTimings.isEmpty()) iterator.remove();
        }
    }

    private static void readFile(Path file) {
        try {
            int loaded = 0;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String word : lines) {
                String formattedWord = word.strip().toLowerCase();
                if (formattedWord.isEmpty() || formattedWord.contains(" ")) continue;

                if (dictionary.add(formattedWord)) loaded++;
            }
            logger.info("Dictionary loaded: " + file + " (" + loaded + "/" + lines.size() + ")");
        } catch (MalformedInputException e) {
            logger.warn("Fail load dictionary: " + file + " (Incorrect encoding, need UTF-8)", e);
        } catch (IOException e) {
            logger.warn("Error load dictionary: " + file, e);
        }
    }

    private static boolean isUserFlood(UUID sender, String text) {
        long currentTime = System.currentTimeMillis();
        int textHash = getHash(text);

        Deque<MessageTiming> messages =
                minuteTimingList.computeIfAbsent(sender, uuid -> new ConcurrentLinkedDeque<>());

        MessageTiming lastMessage = messages.peekLast();

        if (lastMessage != null) {
            if (lastMessage.messageTime + DIFFERENT_MESSAGE_TIMING > currentTime) return true;

            if (lastMessage.messageTime + SAME_MESSAGE_TIMING > currentTime) {
                for (MessageTiming messageTiming : messages) {
                    if (messageTiming.messageHash == textHash) return true;
                }
            }
        }

        messages.add(new MessageTiming(currentTime, textHash));
        return false;
    }

    @NotNull
    @Contract(pure = true)
    private static String filterLog4jExploit(String text) {
        return LOG4J_EXPLOIT_PATTERN.matcher(text).replaceAll(REPLACEMENT_TEXT);
    }

    @NotNull
    @Contract(pure = true)
    private static String urlFilter(String text) {
        return URL_PATTERN.matcher(text).replaceAll(REPLACEMENT_TEXT);
    }

    @NotNull
    @Contract(pure = true)
    private static String filterSwearWords(String text) {
        String[] word = text.split(" ");

        for (int i = 0; i < word.length; i++) {
            if (dictionary.contains(word[i].toLowerCase())) {
                word[i] = REPLACEMENT_TEXT;
            }
        }
        return String.join(" ", word);
    }

    @Contract(pure = true)
    private static int getHash(String text) {
        return text.toLowerCase().hashCode();
    }

    private record MessageTiming(long messageTime, int messageHash) {
    }
}
