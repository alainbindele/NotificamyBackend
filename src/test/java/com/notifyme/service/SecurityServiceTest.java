package com.notifyme.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SecurityServiceTest {

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService();
    }

    @Test
    void testValidPrompt() {
        String validPrompt = "Notify me when it's time for my daily standup meeting";
        assertTrue(securityService.isValidPrompt(validPrompt));
    }

    @Test
    void testSqlInjectionDetection() {
        String maliciousPrompt = "'; DROP TABLE users; --";
        assertFalse(securityService.isValidPrompt(maliciousPrompt));
    }

    @Test
    void testUnionSqlInjection() {
        String maliciousPrompt = "test UNION SELECT * FROM users";
        assertFalse(securityService.isValidPrompt(maliciousPrompt));
    }

    @Test
    void testScriptInjection() {
        String maliciousPrompt = "<script>alert('xss')</script>";
        assertFalse(securityService.isValidPrompt(maliciousPrompt));
    }

    @Test
    void testSanitizeInput() {
        String input = "Hello <script>alert('test')</script> world";
        String sanitized = securityService.sanitizeInput(input);
        assertFalse(sanitized.contains("<script>"));
        assertFalse(sanitized.contains("</script>"));
    }

    @Test
    void testEmptyPrompt() {
        assertFalse(securityService.isValidPrompt(""));
        assertFalse(securityService.isValidPrompt(null));
        assertFalse(securityService.isValidPrompt("   "));
    }

    // Test per i nuovi metodi di validazione dei canali

    @Test
    void testValidChannels() {
        List<String> validChannels = Arrays.asList("email", "whatsapp", "slack", "discord");
        assertTrue(securityService.areValidChannels(validChannels));
    }

    @Test
    void testInvalidChannels() {
        List<String> invalidChannels = Arrays.asList("email", "telegram", "sms");
        assertFalse(securityService.areValidChannels(invalidChannels));
    }

    @Test
    void testValidChannelConfigs() {
        List<String> channels = Arrays.asList("email", "whatsapp", "slack", "discord");
        Map<String, String> configs = new HashMap<>();
        configs.put("email", "test@example.com");
        configs.put("whatsapp", "+393123456789");
        configs.put("slack", "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX");
        configs.put("discord", "https://discord.com/api/webhooks/123456789/abcdefghijklmnopqrstuvwxyz");
        
        assertTrue(securityService.validateChannelConfigs(channels, configs));
    }

    @Test
    void testInvalidEmail() {
        List<String> channels = Arrays.asList("email");
        Map<String, String> configs = new HashMap<>();
        configs.put("email", "invalid-email");
        
        assertFalse(securityService.validateChannelConfigs(channels, configs));
    }

    @Test
    void testInvalidPhoneNumber() {
        List<String> channels = Arrays.asList("whatsapp");
        Map<String, String> configs = new HashMap<>();
        configs.put("whatsapp", "123456789"); // Manca il prefisso +
        
        assertFalse(securityService.validateChannelConfigs(channels, configs));
    }

    @Test
    void testInvalidSlackWebhook() {
        List<String> channels = Arrays.asList("slack");
        Map<String, String> configs = new HashMap<>();
        configs.put("slack", "https://malicious-site.com/webhook");
        
        assertFalse(securityService.validateChannelConfigs(channels, configs));
    }

    @Test
    void testInvalidDiscordWebhook() {
        List<String> channels = Arrays.asList("discord");
        Map<String, String> configs = new HashMap<>();
        configs.put("discord", "https://malicious-site.com/webhook");
        
        assertFalse(securityService.validateChannelConfigs(channels, configs));
    }

    @Test
    void testSqlInjectionInWebhook() {
        List<String> channels = Arrays.asList("slack");
        Map<String, String> configs = new HashMap<>();
        configs.put("slack", "https://hooks.slack.com/services/'; DROP TABLE users; --");
        
        assertFalse(securityService.validateChannelConfigs(channels, configs));
    }

    @Test
    void testSqlInjectionInPhone() {
        List<String> channels = Arrays.asList("whatsapp");
        Map<String, String> configs = new HashMap<>();
        configs.put("whatsapp", "+39123'; DROP TABLE users; --");
        
        assertFalse(securityService.validateChannelConfigs(channels, configs));
    }

    @Test
    void testChannelConfigSanitization() {
        Map<String, String> configs = new HashMap<>();
        configs.put("email", "  test@example.com  ");
        configs.put("whatsapp", "+393123456789<script>");
        
        Map<String, String> sanitized = securityService.sanitizeChannelConfigs(configs);
        
        assertEquals("test@example.com", sanitized.get("email"));
        assertFalse(sanitized.get("whatsapp").contains("<script>"));
    }
}