package com.notifyme.service;

import com.notifyme.entity.Keychain;
import com.notifyme.repository.KeychainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ApiKeyService {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyService.class);

    @Autowired
    private KeychainRepository keychainRepository;

    public boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Empty or null API key provided");
            return false;
        }

        try {
            Optional<Keychain> keychain = keychainRepository.findValidApikey(apiKey.trim());
            
            if (keychain.isPresent()) {
                logger.info("Valid API key found for alias: {}", keychain.get().getAlias());
                return true;
            } else {
                logger.warn("Invalid or inactive API key provided: {}", apiKey.substring(0, Math.min(8, apiKey.length())) + "...");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error validating API key: ", e);
            return false;
        }
    }

    public Optional<Keychain> getKeychainByApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return keychainRepository.findValidApikey(apiKey.trim());
        } catch (Exception e) {
            logger.error("Error retrieving keychain for API key: ", e);
            return Optional.empty();
        }
    }
}