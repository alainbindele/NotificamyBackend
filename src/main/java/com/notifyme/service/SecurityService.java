package com.notifyme.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SecurityService {

    private static final List<String> SQL_INJECTION_PATTERNS = Arrays.asList(
        "(?i).*('|(\\-\\-)|(;)|(\\|)|(\\*)|(%))",
        "(?i).*(union|select|insert|delete|update|drop|create|alter|exec|execute)",
        "(?i).*(script|javascript|vbscript|onload|onerror|onclick)",
        "(?i).*(or\\s+1\\s*=\\s*1|and\\s+1\\s*=\\s*1)",
        "(?i).*(having|group\\s+by|order\\s+by)",
        "(?i).*(<|>|<|>|&)"
    );

    private static final List<Pattern> COMPILED_PATTERNS = SQL_INJECTION_PATTERNS.stream()
            .map(Pattern::compile)
            .toList();

    // Pattern per validare webhook URLs
    private static final Pattern WEBHOOK_URL_PATTERN = Pattern.compile(
        "^https://(?:discord\\.com/api/webhooks/|hooks\\.slack\\.com/services/)[A-Za-z0-9/_-]+$"
    );
    
    // Pattern per validare numeri di telefono (formato internazionale)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+[1-9]\\d{1,14}$"
    );
    
    // Pattern per validare email
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

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

    /**
     * Valida e sanitizza i canali di notifica
     */
    public boolean validateChannelConfigs(List<String> channels, Map<String, String> channelConfigs) {
        if (channels == null || channelConfigs == null) {
            return true; // Opzionali, quindi OK se null
        }

        for (String channel : channels) {
            String config = channelConfigs.get(channel);
            if (config != null && !config.trim().isEmpty()) {
                
                // Controlla SQL injection su tutti i valori
                if (isSqlInjectionAttempt(config)) {
                    return false;
                }
                
                // Validazione specifica per tipo di canale
                switch (channel.toLowerCase()) {
                    case "email":
                        if (!isValidEmail(config)) {
                            return false;
                        }
                        break;
                    case "whatsapp":
                        if (!isValidPhoneNumber(config)) {
                            return false;
                        }
                        break;
                    case "slack":
                        if (!isValidSlackWebhook(config)) {
                            return false;
                        }
                        break;
                    case "discord":
                        if (!isValidDiscordWebhook(config)) {
                            return false;
                        }
                        break;
                    default:
                        // Canale sconosciuto, rifiuta
                        return false;
                }
            }
        }
        
        return true;
    }

    /**
     * Sanitizza i valori dei canali di notifica
     */
    public Map<String, String> sanitizeChannelConfigs(Map<String, String> channelConfigs) {
        if (channelConfigs == null) {
            return null;
        }

        channelConfigs.replaceAll((key, value) -> {
            if (value == null) return null;
            
            // Rimuovi caratteri pericolosi ma mantieni caratteri validi per URL e telefoni
            String sanitized = value.trim();
            
            // Limita lunghezza
            if (sanitized.length() > 500) {
                sanitized = sanitized.substring(0, 500);
            }
            
            return sanitized;
        });

        return channelConfigs;
    }

    private boolean isValidEmail(String email) {
        if (StringUtils.isBlank(email) || email.length() > 100) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isValidPhoneNumber(String phone) {
        if (StringUtils.isBlank(phone) || phone.length() > 20) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    private boolean isValidSlackWebhook(String webhook) {
        if (StringUtils.isBlank(webhook) || webhook.length() > 200) {
            return false;
        }
        
        String trimmed = webhook.trim();
        
        // Deve essere HTTPS e del dominio Slack
        if (!trimmed.startsWith("https://hooks.slack.com/services/")) {
            return false;
        }
        
        // Controlla pattern generale
        return WEBHOOK_URL_PATTERN.matcher(trimmed).matches();
    }

    private boolean isValidDiscordWebhook(String webhook) {
        if (StringUtils.isBlank(webhook) || webhook.length() > 200) {
            return false;
        }
        
        String trimmed = webhook.trim();
        
        // Deve essere HTTPS e del dominio Discord
        if (!trimmed.startsWith("https://discord.com/api/webhooks/") && 
            !trimmed.startsWith("https://discordapp.com/api/webhooks/")) {
            return false;
        }
        
        // Controlla pattern generale
        return WEBHOOK_URL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Valida che i canali specificati siano supportati
     */
    public boolean areValidChannels(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return true;
        }

        List<String> supportedChannels = Arrays.asList("email", "whatsapp", "slack", "discord");
        
        for (String channel : channels) {
            if (channel == null || !supportedChannels.contains(channel.toLowerCase())) {
                return false;
            }
        }
        
        return true;
    }
}