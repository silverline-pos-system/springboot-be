package com.silverline.erp.common.util;

import java.util.regex.Pattern;

/**
 * Input sanitization and validation utilities.
 * Provides protection against XSS, SQL injection, and other input-based attacks.
 */
public final class InputSanitizer {

    private InputSanitizer() {
        // Utility class - no instantiation
    }

    // Pattern to detect potential XSS attack vectors
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script[^>]*?>.*?</script>|" +
            "<[^>]+on\\w+\\s*=|" +
            "javascript\\s*:|" +
            "vbscript\\s*:|" +
            "data\\s*:",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Pattern to detect SQL injection attempts
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "('\\s*(OR|AND)\\s+')|" +
            "(;\\s*(DROP|ALTER|DELETE|INSERT|UPDATE)\\s+)|" +
            "(UNION\\s+SELECT)|" +
            "(--\\s)|" +
            "(/\\*.*?\\*/)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Sanitize text input - strips HTML tags and trims.
     */
    public static String sanitize(String input) {
        if (input == null) return null;
        // Remove HTML tags
        String sanitized = input.replaceAll("<[^>]*>", "");
        // Trim whitespace
        return sanitized.trim();
    }

    /**
     * Check if input contains potential XSS attack vectors.
     */
    public static boolean containsXss(String input) {
        if (input == null || input.isEmpty()) return false;
        return XSS_PATTERN.matcher(input).find();
    }

    /**
     * Check if input contains potential SQL injection patterns.
     */
    public static boolean containsSqlInjection(String input) {
        if (input == null || input.isEmpty()) return false;
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Validate and sanitize search queries.
     * Limits length and removes dangerous characters.
     */
    public static String sanitizeSearchQuery(String query) {
        if (query == null) return null;
        String sanitized = sanitize(query);
        // Limit search query length
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        // Remove special SQL characters
        return sanitized.replaceAll("[;'\"\\\\]", "");
    }

    /**
     * Validate that a string is a safe filename (no path traversal).
     */
    public static boolean isSafeFilename(String filename) {
        if (filename == null || filename.isEmpty()) return false;
        return !filename.contains("..") &&
               !filename.contains("/") &&
               !filename.contains("\\") &&
               !filename.contains("\0");
    }

    /**
     * Sanitize a filename for safe storage.
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
