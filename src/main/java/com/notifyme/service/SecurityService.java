package com.notifyme.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SecurityService {

    private static final List<String> SQL_INJECTION_PATTERNS = Arrays.asList(
        "(?i).*('|(\\-\\-)|(;)|(\\|)|(\\*)|(%))",
        "(?i).*(union|select|insert|delete|update|drop|create|alter|exec|execute)",
        "(?i).*(script|javascript|vbscript|onload|onerror|onclick)",
        "(?i).*(or\\s+1\\s*=\\s*1|and\\s+1\\s*=\\s*1)",
        "(?i).*(having|group\\s+by|order\\s+by)",
        "(?i).*(<|>|&lt;|&gt;|&amp;)"
    );

    private static final List<Pattern> COMPILED_PATTERNS = SQL_INJECTION_PATTERNS.stream()
            .map(Pattern::compile)
            .toList();

    public boolean isSqlInjectionAttempt(String input) {
        if (StringUtils.isBlank(input)) {
            return false;
        }

        String cleanInput = input.trim().toLowerCase();
        
        return COMPILED_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(cleanInput).matches());
    }

    public String sanitizeInput(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }

        // Remove potentially dangerous characters
        String sanitized = input.replaceAll("[<>\"'&]", "");
        
        // Limit length
        if (sanitized.length() > 2000) {
            sanitized = sanitized.substring(0, 2000);
        }

        return sanitized.trim();
    }

    public boolean isValidPrompt(String prompt) {
        if (StringUtils.isBlank(prompt)) {
            return false;
        }

        // Check for SQL injection
        if (isSqlInjectionAttempt(prompt)) {
            return false;
        }

        // Check length
        if (prompt.length() > 2000) {
            return false;
        }

        return true;
    }
}