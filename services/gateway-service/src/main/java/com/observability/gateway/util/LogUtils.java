package com.observability.gateway.util;

/**
 * Utility class for logging operations.
 */
public final class LogUtils {

    private LogUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Sanitize user input for logging to prevent log injection attacks.
     * Replaces newlines, carriage returns, and tabs with underscores.
     *
     * @param input the input string to sanitize
     * @return sanitized string safe for logging
     */
    public static String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }
        return input.replace("\n", "_").replace("\r", "_").replace("\t", "_");
    }
}
