package com.notifyme.controller;

import com.notifyme.dto.ApiResponse;
import com.notifyme.dto.UserProfileResponse;
import com.notifyme.dto.UserUpdateRequest;
import com.notifyme.entity.TUser;
import com.notifyme.service.UserService;
import com.notifyme.service.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin(origins = {"https://notificamy.com", "https://www.notificamy.com", "http://localhost:3000", "http://localhost:5173"}, 
             allowCredentials = "true", maxAge = 3600)
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private SecurityService securityService;

    /**
     * Ottiene il profilo completo dell'utente autenticato
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            TUser user = userService.findOrCreateUser(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            // Crea la risposta con i dati sensibili mascherati
            UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getCreatedAt(),
                user.getDiscordWebhook(),
                user.getSlackWebhook(),
                user.getPhone(),
                true // maschera i dati sensibili
            );
            
            logger.info("Retrieved profile for user: {}", userEmail);
            return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", profile));
            
        } catch (Exception e) {
            logger.error("Error retrieving user profile: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving user profile: " + e.getMessage()));
        }
    }

    /**
     * Aggiorna il profilo dell'utente (nome e/o email)
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserProfile(
            @Valid @RequestBody UserUpdateRequest updateRequest,
            HttpServletRequest request) {
        try {
            String currentUserEmail = (String) request.getAttribute("userEmail");
            TUser user = userService.findOrCreateUser(currentUserEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            // Validazione sicurezza
            if (updateRequest.getDisplayName() != null && 
                !securityService.isValidPrompt(updateRequest.getDisplayName())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid display name"));
            }
            
            if (updateRequest.getEmail() != null && 
                !securityService.isValidPrompt(updateRequest.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid email"));
            }
            
            // Aggiorna i campi se forniti
            boolean updated = false;
            
            if (updateRequest.getDisplayName() != null) {
                String sanitizedName = securityService.sanitizeInput(updateRequest.getDisplayName());
                user.setDisplayName(sanitizedName);
                updated = true;
                logger.info("Updated display name for user: {}", currentUserEmail);
            }
            
            if (updateRequest.getEmail() != null && 
                !updateRequest.getEmail().equals(currentUserEmail)) {
                
                // Verifica che la nuova email non sia già in uso
                if (userService.existsByEmail(updateRequest.getEmail())) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Email already in use"));
                }
                
                String sanitizedEmail = securityService.sanitizeInput(updateRequest.getEmail());
                user.setEmail(sanitizedEmail);
                updated = true;
                logger.info("Updated email for user: {} -> {}", currentUserEmail, sanitizedEmail);
            }
            
            if (!updated) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("No valid fields to update"));
            }
            
            // Salva le modifiche
            TUser updatedUser = userService.saveUser(user);
            
            // Crea la risposta
            UserProfileResponse profile = new UserProfileResponse(
                updatedUser.getId(),
                updatedUser.getEmail(),
                updatedUser.getDisplayName(),
                updatedUser.getCreatedAt(),
                updatedUser.getDiscordWebhook(),
                updatedUser.getSlackWebhook(),
                updatedUser.getPhone(),
                true // maschera i dati sensibili
            );
            
            return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", profile));
            
        } catch (Exception e) {
            logger.error("Error updating user profile: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error updating user profile: " + e.getMessage()));
        }
    }

    /**
     * Elimina l'account dell'utente e tutti i dati associati
     */
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<String>> deleteUserAccount(HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication != null ? authentication.getName() : "unknown";
            
            TUser user = userService.findOrCreateUser(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            // Elimina l'utente e tutti i dati associati (cascade)
            boolean deleted = userService.deleteUser(user);
            
            if (deleted) {
                logger.warn("User account deleted: {} (ID: {})", userEmail, userId);
                return ResponseEntity.ok(ApiResponse.success("User account deleted successfully", "OK"));
            } else {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("Failed to delete user account"));
            }
            
        } catch (Exception e) {
            logger.error("Error deleting user account: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error deleting user account: " + e.getMessage()));
        }
    }

    /**
     * Aggiorna le configurazioni dei canali di notifica
     */
    @PutMapping("/notification-channels")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateNotificationChannels(
            @RequestBody java.util.Map<String, String> channelConfigs,
            HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            TUser user = userService.findOrCreateUser(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            // Validazione sicurezza per i canali
            java.util.List<String> channels = new java.util.ArrayList<>(channelConfigs.keySet());
            
            if (!securityService.areValidChannels(channels)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid notification channels"));
            }
            
            if (!securityService.validateChannelConfigs(channels, channelConfigs)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid channel configurations"));
            }
            
            // Sanitizza le configurazioni
            java.util.Map<String, String> sanitizedConfigs = securityService.sanitizeChannelConfigs(channelConfigs);
            
            // Aggiorna i canali
            boolean updated = userService.updateUserChannels(user, channels, sanitizedConfigs);
            
            if (!updated) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("No valid channels to update"));
            }
            
            // Salva le modifiche
            TUser updatedUser = userService.saveUser(user);
            
            // Crea la risposta
            UserProfileResponse profile = new UserProfileResponse(
                updatedUser.getId(),
                updatedUser.getEmail(),
                updatedUser.getDisplayName(),
                updatedUser.getCreatedAt(),
                updatedUser.getDiscordWebhook(),
                updatedUser.getSlackWebhook(),
                updatedUser.getPhone(),
                true // maschera i dati sensibili
            );
            
            logger.info("Updated notification channels for user: {}", userEmail);
            return ResponseEntity.ok(ApiResponse.success("Notification channels updated successfully", profile));
            
        } catch (Exception e) {
            logger.error("Error updating notification channels: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error updating notification channels: " + e.getMessage()));
        }
    }

    /**
     * Ottiene le statistiche dell'utente
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getUserStatistics(HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            TUser user = userService.findOrCreateUser(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            java.util.Map<String, Object> statistics = userService.getUserStatistics(user);
            
            logger.info("Retrieved statistics for user: {}", userEmail);
            return ResponseEntity.ok(ApiResponse.success("User statistics retrieved successfully", statistics));
            
        } catch (Exception e) {
            logger.error("Error retrieving user statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving user statistics: " + e.getMessage()));
        }
    }
}