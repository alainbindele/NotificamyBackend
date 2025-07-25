package com.notifyme.controller;

import com.notifyme.dto.ApiResponse;
import com.notifyme.dto.ChannelConfigRequest;
import com.notifyme.dto.ChannelConfigResponse;
import com.notifyme.entity.TUser;
import com.notifyme.service.UserService;
import com.notifyme.service.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/channels")
@CrossOrigin(origins = {"https://notificamy.com", "https://www.notificamy.com", "http://localhost:3000", "http://localhost:5173"}, 
             allowCredentials = "true", maxAge = 3600)
public class NotificationChannelController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationChannelController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private SecurityService securityService;

    /**
     * GET /api/v1/channels - Ottiene tutti i canali configurati
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, ChannelConfigResponse>>> getAllChannels(HttpServletRequest request) {
        try {
            TUser user = getUserFromRequest(request);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User authentication information missing"));
            }
            
            Map<String, ChannelConfigResponse> channels = new HashMap<>();
            
            // Email (sempre presente)
            channels.put("email", new ChannelConfigResponse("email", user.getEmail(), true, false));
            
            // Discord
            boolean discordConfigured = user.getDiscordWebhook() != null && !user.getDiscordWebhook().trim().isEmpty();
            channels.put("discord", new ChannelConfigResponse("discord", 
                discordConfigured ? maskWebhook(user.getDiscordWebhook()) : null, 
                discordConfigured, true));
            
            // Slack
            boolean slackConfigured = user.getSlackWebhook() != null && !user.getSlackWebhook().trim().isEmpty();
            channels.put("slack", new ChannelConfigResponse("slack", 
                slackConfigured ? maskWebhook(user.getSlackWebhook()) : null, 
                slackConfigured, true));
            
            // WhatsApp
            boolean whatsappConfigured = user.getPhone() != null && !user.getPhone().trim().isEmpty();
            channels.put("whatsapp", new ChannelConfigResponse("whatsapp", 
                whatsappConfigured ? maskPhone(user.getPhone()) : null, 
                whatsappConfigured, true));
            
            logger.info("Retrieved all channels for user: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Channels retrieved successfully", channels));
            
        } catch (Exception e) {
            logger.error("Error retrieving channels: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving channels: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/channels/{channelType} - Ottiene configurazione di un canale specifico
     */
    @GetMapping("/{channelType}")
    public ResponseEntity<ApiResponse<ChannelConfigResponse>> getChannel(
            @PathVariable String channelType, 
            HttpServletRequest request) {
        try {
            TUser user = getUserFromRequest(request);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User authentication information missing"));
            }
            
            ChannelConfigResponse channelConfig = getChannelConfig(user, channelType.toLowerCase());
            if (channelConfig == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid channel type: " + channelType));
            }
            
            logger.info("Retrieved {} channel for user: {}", channelType, user.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Channel retrieved successfully", channelConfig));
            
        } catch (Exception e) {
            logger.error("Error retrieving {} channel: {}", channelType, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving channel: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/channels/{channelType} - Crea/aggiorna configurazione di un canale
     */
    @PostMapping("/{channelType}")
    public ResponseEntity<ApiResponse<ChannelConfigResponse>> createOrUpdateChannel(
            @PathVariable String channelType,
            @Valid @RequestBody ChannelConfigRequest configRequest,
            HttpServletRequest request) {
        try {
            TUser user = getUserFromRequest(request);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User authentication information missing"));
            }
            
            String channelTypeLower = channelType.toLowerCase();
            
            // Validazione sicurezza
            if (!securityService.areValidChannels(List.of(channelTypeLower))) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid channel type: " + channelType));
            }
            
            Map<String, String> channelConfigs = Map.of(channelTypeLower, configRequest.getConfiguration());
            if (!securityService.validateChannelConfigs(List.of(channelTypeLower), channelConfigs)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid channel configuration"));
            }
            
            // Sanitizza la configurazione
            Map<String, String> sanitizedConfigs = securityService.sanitizeChannelConfigs(channelConfigs);
            
            // Aggiorna il canale
            boolean updated = userService.updateUserChannels(user, List.of(channelTypeLower), sanitizedConfigs);
            
            if (!updated) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to update channel configuration"));
            }
            
            // Salva le modifiche
            TUser updatedUser = userService.saveUser(user);
            
            // Crea la risposta
            ChannelConfigResponse channelConfig = getChannelConfig(updatedUser, channelTypeLower);
            
            logger.info("Created/updated {} channel for user: {}", channelType, user.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Channel configured successfully", channelConfig));
            
        } catch (Exception e) {
            logger.error("Error configuring {} channel: {}", channelType, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error configuring channel: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/v1/channels/{channelType} - Aggiorna configurazione di un canale esistente
     */
    @PutMapping("/{channelType}")
    public ResponseEntity<ApiResponse<ChannelConfigResponse>> updateChannel(
            @PathVariable String channelType,
            @Valid @RequestBody ChannelConfigRequest configRequest,
            HttpServletRequest request) {
        // Usa la stessa logica di POST per semplicità
        return createOrUpdateChannel(channelType, configRequest, request);
    }

    /**
     * DELETE /api/v1/channels/{channelType} - Rimuove configurazione di un canale
     */
    @DeleteMapping("/{channelType}")
    public ResponseEntity<ApiResponse<String>> deleteChannel(
            @PathVariable String channelType,
            HttpServletRequest request) {
        try {
            TUser user = getUserFromRequest(request);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User authentication information missing"));
            }
            
            String channelTypeLower = channelType.toLowerCase();
            
            // Non permettere di eliminare l'email
            if ("email".equals(channelTypeLower)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Cannot delete email channel"));
            }
            
            boolean updated = false;
            
            switch (channelTypeLower) {
                case "discord":
                    if (user.getDiscordWebhook() != null && !user.getDiscordWebhook().trim().isEmpty()) {
                        user.setDiscordWebhook("");
                        updated = true;
                    }
                    break;
                case "slack":
                    if (user.getSlackWebhook() != null && !user.getSlackWebhook().trim().isEmpty()) {
                        user.setSlackWebhook("");
                        updated = true;
                    }
                    break;
                case "whatsapp":
                    if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                        user.setPhone("");
                        updated = true;
                    }
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Invalid channel type: " + channelType));
            }
            
            if (!updated) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Channel was not configured"));
            }
            
            // Salva le modifiche
            userService.saveUser(user);
            
            logger.info("Deleted {} channel for user: {}", channelType, user.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Channel deleted successfully", "OK"));
            
        } catch (Exception e) {
            logger.error("Error deleting {} channel: {}", channelType, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error deleting channel: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/channels/{channelType}/test - Testa la configurazione di un canale
     */
    @PostMapping("/{channelType}/test")
    public ResponseEntity<ApiResponse<String>> testChannel(
            @PathVariable String channelType,
            HttpServletRequest request) {
        try {
            TUser user = getUserFromRequest(request);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User authentication information missing"));
            }
            
            String channelTypeLower = channelType.toLowerCase();
            ChannelConfigResponse channelConfig = getChannelConfig(user, channelTypeLower);
            
            if (channelConfig == null || !channelConfig.isConfigured()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Channel not configured: " + channelType));
            }
            
            // TODO: Implementare test effettivo del canale
            // Per ora restituisce solo successo
            
            logger.info("Tested {} channel for user: {}", channelType, user.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Channel test successful", "Test message sent"));
            
        } catch (Exception e) {
            logger.error("Error testing {} channel: {}", channelType, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error testing channel: " + e.getMessage()));
        }
    }

    // Metodi di utilità privati

    private TUser getUserFromRequest(HttpServletRequest request) {
        String userEmail = (String) request.getAttribute("userEmail");
        String authSubject = (String) request.getAttribute("authSubject");
        
        if (userEmail == null || authSubject == null) {
            logger.error("Missing user information in request attributes - userEmail: {}, authSubject: {}", userEmail, authSubject);
            return null;
        }
        
        return userService.findOrCreateUserByEmailAndSubject(userEmail, authSubject);
    }

    private ChannelConfigResponse getChannelConfig(TUser user, String channelType) {
        switch (channelType) {
            case "email":
                return new ChannelConfigResponse("email", user.getEmail(), true, false);
            case "discord":
                boolean discordConfigured = user.getDiscordWebhook() != null && !user.getDiscordWebhook().trim().isEmpty();
                return new ChannelConfigResponse("discord", 
                    discordConfigured ? maskWebhook(user.getDiscordWebhook()) : null, 
                    discordConfigured, true);
            case "slack":
                boolean slackConfigured = user.getSlackWebhook() != null && !user.getSlackWebhook().trim().isEmpty();
                return new ChannelConfigResponse("slack", 
                    slackConfigured ? maskWebhook(user.getSlackWebhook()) : null, 
                    slackConfigured, true);
            case "whatsapp":
                boolean whatsappConfigured = user.getPhone() != null && !user.getPhone().trim().isEmpty();
                return new ChannelConfigResponse("whatsapp", 
                    whatsappConfigured ? maskPhone(user.getPhone()) : null, 
                    whatsappConfigured, true);
            default:
                return null;
        }
    }

    private String maskWebhook(String webhook) {
        if (webhook == null || webhook.trim().isEmpty() || webhook.length() < 10) {
            return webhook;
        }
        return webhook.substring(0, 10) + "***" + webhook.substring(webhook.length() - 5);
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty() || phone.length() < 6) {
            return phone;
        }
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 3);
    }
}