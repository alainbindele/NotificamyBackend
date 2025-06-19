package com.notifyme.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
}