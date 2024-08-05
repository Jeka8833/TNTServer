package com.jeka8833.tntserver.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Arrays;
import java.util.regex.Pattern;

public class VersionUtil {
    private static final Pattern VERSION_SPLIT = Pattern.compile("\\D+");

    /**
     * Compares two version strings and returns an integer indicating their order.
     * The version strings are expected to consist of numbers separated by dots, and may contain other non-numeric characters that are ignored.
     * For example, compareVersions(“1.2.3”, “1.2.4”) returns -1, compareVersions(“2.0”, “2.0.0”) returns 0, and compareVersions(“3.1.1”, “3.0.9”) returns 1.
     *
     * @param x the first version string to compare
     * @param y the second version string to compare
     * @return -1 if x is less than y, 0 if x is equal to y, or 1 if x is greater than y
     */
    @Contract(pure = true)
    @Range(from = -1, to = 1)
    public static int compareVersions(@NotNull String x, @NotNull String y) {
        String[] parts1 = deleteStartEmptyString(VERSION_SPLIT.split(x));
        String[] parts2 = deleteStartEmptyString(VERSION_SPLIT.split(y));

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (v1 == v2) continue;
            return Integer.compare(v1, v2);
        }
        return 0;
    }

    /**
     * This method removes any leading empty strings from the input array.
     *
     * @param array The input array of strings. This array may contain empty strings at the beginning.
     * @return A new array of strings, which is a copy of the input array but without any leading empty strings.
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    private static String[] deleteStartEmptyString(String... array) {
        int start = 0;
        while (start < array.length && array[start].isEmpty()) {
            start++;
        }

        return Arrays.copyOfRange(array, start, array.length);
    }
}
