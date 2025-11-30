package com.observability.commons.util;

/**
 * Utility class for logging operations across all microservices.
 * 
 * <p>This class provides common logging utilities to ensure consistent
 * and secure logging practices throughout the application.</p>
 *
 * @since 1.0.0
 */
public final class LogUtils {

    private LogUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Sanitizes user input for logging to prevent log injection attacks.
     * 
     * <p>This method replaces potentially dangerous characters that could be used
     * for log injection attacks, specifically newlines, carriage returns, and tabs,
     * with underscores.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * logger.info("User requested: {}", LogUtils.sanitizeForLog(userInput));
     * }</pre>
     *
     * @param input the input string to sanitize, may be {@code null}
     * @return sanitized string safe for logging, or "null" if input is {@code null}
     */
    public static String sanitizeForLog(final String input) {
        if (input == null) {
            return "null";
        }
        return input.replace("\n", "_").replace("\r", "_").replace("\t", "_");
    }
}
