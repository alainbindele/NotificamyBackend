package com.notifyme.service;

import com.notifyme.entity.TUser;
import com.notifyme.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private QueryService queryService;
    
    /**
     * Trova o crea utente usando email come chiave primaria e subject come token di riconoscimento
     */
    public TUser findOrCreateUserByEmailAndSubject(String email, String authSubject) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        if (authSubject == null || authSubject.trim().isEmpty()) {
            throw new IllegalArgumentException("Auth subject cannot be null or empty");
        }
        
        // 1. Prima prova a trovare per email (chiave primaria)
        Optional<TUser> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            TUser user = userByEmail.get();
            // Aggiorna il subject se è cambiato o non era presente
            if (user.getAuthSubject() == null || !user.getAuthSubject().equals(authSubject)) {
                user.setAuthSubject(authSubject);
                user = userRepository.save(user);
                logger.info("Updated auth subject for existing user: {}", email);
            }
            return user;
        }
        
        // 2. Se non trovato per email, prova per subject (per utenti esistenti con vecchio sistema)
        Optional<TUser> userBySubject = userRepository.findByAuthSubject(authSubject);
        if (userBySubject.isPresent()) {
            TUser user = userBySubject.get();
            // Aggiorna l'email se è cambiata
            if (!user.getEmail().equals(email)) {
                user.setEmail(email);
                user = userRepository.save(user);
                logger.info("Updated email for existing user with subject: {} -> {}", authSubject, email);
            }
            return user;
        }
        
        // 3. Crea nuovo utente con email e subject
        logger.info("Creating new user with email: {} and subject: {}", email, authSubject);
        TUser newUser = new TUser(email, authSubject);
        return userRepository.save(newUser);
    }
    
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
    
    /**
     * Aggiorna i canali di notifica dell'utente
     */
    public boolean updateUserChannels(TUser user, List<String> channels, Map<String, String> channelConfigs) {
        if (channels == null || channelConfigs == null) {
            return false;
        }
        
        boolean updated = false;
        
        for (String channel : channels) {
            String config = channelConfigs.get(channel);
            if (config != null && !config.trim().isEmpty()) {
                switch (channel.toLowerCase()) {
                    case "whatsapp":
                        if (!config.equals(user.getPhone())) {
                            user.setPhone(config.trim());
                            updated = true;
                            logger.debug("Updated WhatsApp phone for user {}: {}", user.getEmail(), config);
                        }
                        break;
                    case "slack":
                        if (!config.equals(user.getSlackWebhook())) {
                            user.setSlackWebhook(config.trim());
                            updated = true;
                            logger.debug("Updated Slack webhook for user {}: {}", user.getEmail(), maskWebhook(config));
                        }
                        break;
                    case "discord":
                        if (!config.equals(user.getDiscordWebhook())) {
                            user.setDiscordWebhook(config.trim());
                            updated = true;
                            logger.debug("Updated Discord webhook for user {}: {}", user.getEmail(), maskWebhook(config));
                        }
                        break;
                    case "email":
                        // L'email è già gestita separatamente nel profilo utente
                        logger.debug("Email channel confirmed for user: {}", user.getEmail());
                        break;
                    default:
                        logger.warn("Unknown notification channel: {} for user: {}", channel, user.getEmail());
                        break;
                }
            }
        }
        
        return updated;
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
    
    /**
     * Salva un utente
     */
    public TUser saveUser(TUser user) {
        return userRepository.save(user);
    }
    
    /**
     * Elimina un utente e tutti i dati associati
     */
    public boolean deleteUser(TUser user) {
        try {
            userRepository.delete(user);
            logger.info("Successfully deleted user: {}", user.getEmail());
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete user {}: {}", user.getEmail(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Ottiene statistiche dell'utente
     */
    public Map<String, Object> getUserStatistics(TUser user) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Statistiche base dell'utente
            stats.put("userId", user.getId());
            stats.put("email", user.getEmail());
            stats.put("displayName", user.getDisplayName());
            stats.put("memberSince", user.getCreatedAt());
            
            // Calcola giorni da registrazione
            if (user.getCreatedAt() != null) {
                long daysSinceRegistration = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
                stats.put("daysSinceRegistration", daysSinceRegistration);
            }
            
            // Statistiche canali configurati
            int configuredChannels = 0;
            if (user.getDiscordWebhook() != null && !user.getDiscordWebhook().trim().isEmpty()) {
                configuredChannels++;
            }
            if (user.getSlackWebhook() != null && !user.getSlackWebhook().trim().isEmpty()) {
                configuredChannels++;
            }
            if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                configuredChannels++;
            }
            configuredChannels++; // Email è sempre configurata
            
            stats.put("configuredChannels", configuredChannels);
            
            // Statistiche query (se il servizio è disponibile)
            try {
                QueryService.QueryStatistics queryStats = queryService.getQueryStatistics(user);
                stats.put("totalQueries", queryStats.getTotalQueries());
                stats.put("cronQueries", queryStats.getCronQueries());
                stats.put("specificQueries", queryStats.getSpecificQueries());
                stats.put("checkQueries", queryStats.getCheckQueries());
                
                // Query attive
                List<com.notifyme.entity.TQuery> activeQueries = queryService.findActiveQueriesByUser(user);
                stats.put("activeQueries", activeQueries.size());
                
            } catch (Exception e) {
                logger.warn("Failed to get query statistics for user {}: {}", user.getEmail(), e.getMessage());
                stats.put("totalQueries", 0);
                stats.put("activeQueries", 0);
            }
            
        } catch (Exception e) {
            logger.error("Error calculating user statistics for {}: {}", user.getEmail(), e.getMessage(), e);
        }
        
        return stats;
    }
}