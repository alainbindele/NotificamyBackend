package com.notifyme.service;

import com.notifyme.entity.TUser;
import com.notifyme.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    public TUser findOrCreateUser(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    logger.info("Creating new user with email: {}", email);
                    TUser newUser = new TUser(email);
                    return userRepository.save(newUser);
                });
    }
    
    public TUser findOrCreateUserWithChannels(String email, List<String> channels, Map<String, String> channelConfigs) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        TUser user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    logger.info("Creating new user with email: {}", email);
                    return new TUser(email);
                });
        
        // Aggiorna i canali di notifica se forniti
        if (channels != null && channelConfigs != null) {
            updateUserChannels(user, channels, channelConfigs);
        }
        
        return userRepository.save(user);
    }
    
    private void updateUserChannels(TUser user, List<String> channels, Map<String, String> channelConfigs) {
        logger.info("Updating notification channels for user: {}", user.getEmail());
        
        for (String channel : channels) {
            String config = channelConfigs.get(channel);
            if (config != null && !config.trim().isEmpty()) {
                switch (channel.toLowerCase()) {
                    case "whatsapp":
                        user.setPhone(config.trim());
                        logger.debug("Updated WhatsApp phone for user {}: {}", user.getEmail(), config);
                        break;
                    case "slack":
                        user.setSlackWebhook(config.trim());
                        logger.debug("Updated Slack webhook for user {}: {}", user.getEmail(), maskWebhook(config));
                        break;
                    case "discord":
                        user.setDiscordWebhook(config.trim());
                        logger.debug("Updated Discord webhook for user {}: {}", user.getEmail(), maskWebhook(config));
                        break;
                    case "email":
                        // L'email è già impostata come campo principale
                        logger.debug("Email channel confirmed for user: {}", user.getEmail());
                        break;
                    default:
                        logger.warn("Unknown notification channel: {} for user: {}", channel, user.getEmail());
                        break;
                }
            }
        }
    }
    
    private String maskWebhook(String webhook) {
        if (webhook == null || webhook.length() < 10) {
            return webhook;
        }
        return webhook.substring(0, 10) + "***" + webhook.substring(webhook.length() - 5);
    }
    
    public TUser findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}