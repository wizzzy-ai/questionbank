package com.example.questionbank;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing user input to prevent XSS attacks.
 * This is a defense-in-depth measure in addition to output encoding in templates.
 */
@Component
public class XssSanitizer {

    // Patterns for common XSS attack vectors
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ON_EVENT_PATTERN = Pattern.compile("\\s+(on\\w+)\\s*=\\s*['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAGS_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern ALERT_PATTERN = Pattern.compile("alert\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVAL_PATTERN = Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE);

    /**
     * Sanitizes input by removing dangerous HTML and script content.
     * Use this for plain text fields.
     */
    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = input;

        // Remove script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove javascript: protocol
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove event handlers (onclick, onload, etc.)
        sanitized = ON_EVENT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove dangerous JavaScript functions
        sanitized = ALERT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = EVAL_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = EXPRESSION_PATTERN.matcher(sanitized).replaceAll("");

        return sanitized;
    }

    /**
     * Completely strips all HTML tags. Use this for fields that should be plain text only.
     */
    public String stripHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = sanitize(input);
        return HTML_TAGS_PATTERN.matcher(sanitized).replaceAll("");
    }

    /**
     * Sanitizes input while preserving basic formatting tags (b, i, u, p, br).
     * Use this for rich text fields if needed.
     */
    public String sanitizePreserveFormatting(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // First apply basic sanitization
        String sanitized = sanitize(input);

        // Remove all tags except allowed ones
        sanitized = sanitized.replaceAll("</?(?!b\\s*|/b\\s*|i\\s*|/i\\s*|u\\s*|/u\\s*|p\\s*|/p\\s*|br\\s*|/br\\s*)[^>]*>", "");

        return sanitized;
    }

    /**
     * Validates that input doesn't contain suspicious patterns.
     * Returns true if input is safe, false if it contains suspicious content.
     */
    public boolean isValid(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }

        String check = input.toLowerCase();

        // Check for script tags
        if (check.contains("<script") || check.contains("</script>")) {
            return false;
        }

        // Check for javascript protocol
        if (check.contains("javascript:")) {
            return false;
        }

        // Check for common event handlers
        if (check.contains("onerror=") || check.contains("onload=") ||
            check.contains("onclick=") || check.contains("onmouseover=")) {
            return false;
        }

        return true;
    }
}
