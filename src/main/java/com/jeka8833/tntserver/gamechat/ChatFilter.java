package com.jeka8833.tntserver.gamechat;

import com.jeka8833.tntserver.Main;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public class ChatFilter {
    private static final long DIFFERENT_MESSAGE_TIMING = TimeUnit.MILLISECONDS.toNanos(750);
    private static final long SAME_MESSAGE_TIMING = TimeUnit.SECONDS.toNanos(10);
    private static final long TIME_TO_CLEAR = TimeUnit.MINUTES.toNanos(1);
    private static final String REPLACEMENT_TEXT = "***";

    private static final Pattern LOG4J_EXPLOIT_PATTERN = Pattern.compile("\\$\\{.+}");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(http|ftp|https)://([\\w_\\-]+(?:\\.[\\w_\\-]+)+)([\\w.,@?^=%&:/~+#\\-]*[\\w@?^=%&/~+#\\-])");
    private static final Pattern PATTERN_COLOR_CODE = Pattern.compile("(?i)[\\u00A7&][0-9A-FK-OR]");

    private static final Collection<String> DICTIONARY = new HashSet<>();
    private static final Map<UUID, Deque<MessageTiming>> MINUTE_TIMING_LIST = new ConcurrentHashMap<>();

    public static void loadDictionaries() {
        if (Main.INSTANCE.swearDictionary.isEmpty()) {
            log.warn("Swear Dictionary path is not set. Chat will not be filtered.");

            return;
        }

        Thread thread = new Thread(() -> {
            try (Stream<Path> paths = Files.walk(Main.INSTANCE.swearDictionary.get())) {
                paths
                        .filter(Files::isRegularFile)
                        .forEach(ChatFilter::readFile);
            } catch (IOException e) {
                log.warn("Error load dictionary: {}", Main.INSTANCE.swearDictionary.get(), e);
            }

            log.info("Dictionary loaded: {} words", DICTIONARY.size());
        }, "Dictionary loader");

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    @Nullable
    public static String filter(UUID sender, String text) {
        String filtered = text.strip();
        filtered = filterLog4jExploit(filtered);
        filtered = urlFilter(filtered);
        filtered = filterSwearWords(filtered);

        if (isUserFlood(sender, filtered)) return null;

        return filtered;
    }

    public static void clearOld() {
        long currentTime = System.nanoTime();

        Iterator<Deque<MessageTiming>> iterator = MINUTE_TIMING_LIST.values().iterator();
        while (iterator.hasNext()) {
            Deque<MessageTiming> messageTimings = iterator.next();

            MessageTiming message;
            while ((message = messageTimings.peek()) != null && currentTime - message.messageTime > TIME_TO_CLEAR) {
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
                if (formattedWord.isBlank() || formattedWord.contains(" ")) continue;

                if (DICTIONARY.add(formattedWord)) loaded++;
            }
            log.info("Dictionary loaded: {} ({}/{})", file, loaded, lines.size());
        } catch (MalformedInputException e) {
            log.warn("Fail load dictionary: {} (Incorrect encoding, need UTF-8)", file, e);
        } catch (IOException e) {
            log.warn("Error load dictionary: {}", file, e);
        }
    }

    private static boolean isUserFlood(UUID sender, String text) {
        long currentTime = System.nanoTime();
        int textHash = getHash(text);

        Deque<MessageTiming> messages =
                MINUTE_TIMING_LIST.computeIfAbsent(sender, uuid -> new ConcurrentLinkedDeque<>());

        MessageTiming lastMessage = messages.peekLast();

        if (lastMessage != null) {
            if (currentTime - lastMessage.messageTime < DIFFERENT_MESSAGE_TIMING) return true;

            if (currentTime - lastMessage.messageTime < SAME_MESSAGE_TIMING) {
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
            if (DICTIONARY.contains(stripControlCodes(word[i]).toLowerCase())) {
                word[i] = REPLACEMENT_TEXT;
            }
        }
        return String.join(" ", word);
    }

    public static String stripControlCodes(String text) {
        return PATTERN_COLOR_CODE.matcher(text).replaceAll("");
    }

    @Contract(pure = true)
    private static int getHash(String text) {
        return text.toLowerCase().hashCode();
    }

    private record MessageTiming(long messageTime, int messageHash) {
    }
}
