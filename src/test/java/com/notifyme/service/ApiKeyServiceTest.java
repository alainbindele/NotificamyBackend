package com.notifyme.service;

import com.notifyme.entity.Keychain;
import com.notifyme.repository.KeychainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private KeychainRepository keychainRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private Keychain validKeychain;
    private Keychain expiredKeychain;
    private Keychain disabledKeychain;

    @BeforeEach
    void setUp() {
        validKeychain = new Keychain("valid-api-key-123", "test-alias");
        
        expiredKeychain = new Keychain("expired-api-key-456", "expired-alias");
        expiredKeychain.setExpired(true);
        
        disabledKeychain = new Keychain("disabled-api-key-789", "disabled-alias");
        disabledKeychain.setDisabled(true);
    }

    @Test
    void testValidApiKey() {
        when(keychainRepository.findValidApikey("valid-api-key-123"))
            .thenReturn(Optional.of(validKeychain));

        assertTrue(apiKeyService.isValidApiKey("valid-api-key-123"));
    }

    @Test
    void testInvalidApiKey() {
        when(keychainRepository.findValidApikey("invalid-key"))
            .thenReturn(Optional.empty());

        assertFalse(apiKeyService.isValidApiKey("invalid-key"));
    }

    @Test
    void testNullApiKey() {
        assertFalse(apiKeyService.isValidApiKey(null));
    }

    @Test
    void testEmptyApiKey() {
        assertFalse(apiKeyService.isValidApiKey(""));
        assertFalse(apiKeyService.isValidApiKey("   "));
    }

    @Test
    void testGetKeychainByApiKey() {
        when(keychainRepository.findValidApikey("valid-api-key-123"))
            .thenReturn(Optional.of(validKeychain));

        Optional<Keychain> result = apiKeyService.getKeychainByApiKey("valid-api-key-123");
        
        assertTrue(result.isPresent());
        assertEquals("test-alias", result.get().getAlias());
        assertEquals("valid-api-key-123", result.get().getApikey());
    }

    @Test
    void testGetKeychainByInvalidApiKey() {
        when(keychainRepository.findValidApikey(anyString()))
            .thenReturn(Optional.empty());

        Optional<Keychain> result = apiKeyService.getKeychainByApiKey("invalid-key");
        
        assertFalse(result.isPresent());
    }
}