package com.jeka8833.tntserver;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class VersionUtil {
    /**
     * Compares two version strings and returns an integer indicating their order.
     * The version strings are expected to consist of numbers separated by dots, and may contain other non-numeric characters that are ignored.
     * For example, compareVersions(“1.2.3”, “1.2.4”) returns -1, compareVersions(“2.0”, “2.0.0”) returns 0, and compareVersions(“3.1.1”, “3.0.9”) returns 1.
     *
     * @param x the first version string to compare
     * @param y the second version string to compare
     * @return -1 if x is less than y, 0 if x is equal to y, or 1 if x is greater than y
     * @throws NumberFormatException if the string does not contain a parsable unsigned integer
     */
    @Contract(pure = true)
    @Range(from = -1, to = 1)
    public static int compareVersions(@NotNull String x, @NotNull String y) {
        String[] parts1 = x.replaceAll("[^0-9.]+", "").split("\\.");
        String[] parts2 = y.replaceAll("[^0-9.]+", "").split("\\.");
        int minLength = Math.min(parts1.length, parts2.length);

        for (int i = 0; i < minLength; i++) {
            int num1 = Integer.parseInt(parts1[i]);
            int num2 = Integer.parseInt(parts2[i]);

            if (num1 != num2) return num1 > num2 ? 1 : -1;
        }

        for (int i = minLength; i < parts1.length; i++) {
            if (Integer.parseInt(parts1[i]) != 0) return 1;
        }
        for (int i = minLength; i < parts2.length; i++) {
            if (Integer.parseInt(parts2[i]) != 0) return -1;
        }

        return 0;
    }
}
