package com.notifyme.controller;

import com.notifyme.dto.ApiResponse;
import com.notifyme.dto.ChatGptResponse;
import com.notifyme.dto.ChatGptValidationResponse;
import com.notifyme.dto.PromptRequest;
import com.notifyme.entity.TUser;
import com.notifyme.service.ChatGptService;
import com.notifyme.service.SecurityService;
import com.notifyme.service.UserService;
import com.notifyme.service.QueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = {"https://notificamy.com", "https://www.notificamy.com", "http://localhost:3000", "http://localhost:5173"}, 
             allowCredentials = "true", maxAge = 3600)
public class PromptController {

    private static final Logger logger = LoggerFactory.getLogger(PromptController.class);

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ChatGptService chatGptService;

    @Autowired
    private UserService userService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/validate-prompt")
    public ResponseEntity<ApiResponse<String>> processPrompt(@Valid @RequestBody PromptRequest request, 
                                                           HttpServletRequest httpRequest) {
        
        // Get authenticated user information
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication != null ? authentication.getName() : "anonymous";
        String userEmail = (String) httpRequest.getAttribute("userEmail");
        
        // Use email from request if provided, otherwise use the one from JWT
        String emailToSave = (request.getEmail() != null && !request.getEmail().trim().isEmpty()) 
                            ? request.getEmail().trim() 
                            : userEmail;
        
        logger.info("Received prompt request from authenticated user: {} ({}) with channels: {}", 
                   emailToSave, userId, request.getChannels());

        try {
            // SECURITY: Validate prompt for security
            if (!securityService.isValidPrompt(request.getPrompt())) {
                logger.warn("Invalid or potentially malicious prompt detected from user {}: {}", userId, request.getPrompt());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid prompt. Please check your input for security violations."));
            }

            // SECURITY: Validate channels and channel configurations
            if (!securityService.areValidChannels(request.getChannels())) {
                logger.warn("Invalid notification channels detected from user {}: {}", userId, request.getChannels());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid notification channels specified."));
            }

            if (!securityService.validateChannelConfigs(request.getChannels(), request.getChannelConfigs())) {
                logger.warn("Invalid or potentially malicious channel configurations detected from user {}: {}", 
                           userId, request.getChannels());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid channel configurations. Please check webhook URLs, phone numbers, and email addresses."));
            }

            // SECURITY: Sanitize all inputs
            String sanitizedPrompt = securityService.sanitizeInput(request.getPrompt());
            var sanitizedChannelConfigs = securityService.sanitizeChannelConfigs(request.getChannelConfigs());
            
            logger.info("Processing sanitized prompt for user {}: {}", userId, sanitizedPrompt);

            // Save or find user in database with notification channels
            TUser user = null;
            if (emailToSave != null && !emailToSave.isEmpty()) {
                user = userService.findOrCreateUserWithChannels(
                    emailToSave, 
                    request.getChannels(), 
                    sanitizedChannelConfigs
                );
                logger.info("User found/created with ID: {} and email: {} with notification channels updated", 
                           user.getId(), user.getEmail());
            }

            // Send to ChatGPT synchronously
            ChatGptResponse chatGptResponse = chatGptService.sendPromptToChatGptSync(sanitizedPrompt, request.getTimezone());
            
            if (chatGptResponse != null && chatGptResponse.getChoices() != null && !chatGptResponse.getChoices().isEmpty()) {
                String content = chatGptResponse.getChoices().get(0).getMessage().getContent();
                logger.info("ChatGPT response received successfully for user: {}", userId);
                logger.info("Raw ChatGPT response content: {}", content);
                
                // Parse ChatGPT response to extract validation data
                try {
                    // Pulisci il contenuto prima del parsing
                    String cleanedContent = cleanJsonResponse(content);
                    logger.debug("Cleaned ChatGPT response: {}", cleanedContent);
                    
                    ChatGptValidationResponse validationResponse = objectMapper.readValue(cleanedContent, ChatGptValidationResponse.class);
                    
                    // Verifica che la risposta sia valida
                    if (validationResponse == null) {
                        throw new Exception("Parsed response is null");
                    }
                    
                    logger.info("Successfully parsed ChatGPT validation response for user: {}", userId);
                    
                    // Save query to database with complete validation data
                    if (user != null) {
                        var savedQuery = queryService.createQuery(user, sanitizedPrompt, validationResponse, request.getTimezone());
                        
                        // Update query with enabled channels
                        if (request.getChannels() != null && !request.getChannels().isEmpty()) {
                            try {
                                savedQuery.setEnabledChannels(objectMapper.writeValueAsString(request.getChannels()));
                                logger.info("Updated query {} with enabled channels: {}", savedQuery.getId(), request.getChannels());
                            } catch (Exception e) {
                                logger.warn("Failed to serialize enabled channels: {}", e.getMessage());
                            }
                        }
                        
                        logger.info("Query saved successfully for user: {} with full ChatGPT validation data", userId);
                        
                        // MIGLIORAMENTO: Se la query non è valida, restituisci errore al frontend
                        if (!Boolean.TRUE.equals(savedQuery.getIsValid())) {
                            String errorMessage = savedQuery.getInvalidReason() != null ? 
                                                 savedQuery.getInvalidReason() : 
                                                 "Il prompt non è valido secondo le policy di sistema";
                            
                            logger.warn("Query marked as invalid for user {}: {}", userId, errorMessage);
                            return ResponseEntity.badRequest()
                                    .body(ApiResponse.error(errorMessage));
                        }
                    }
                    
                    // Log validation results for monitoring
                    if (validationResponse.getValidity() != null) {
                        logger.info("Validation results for user {}: valid={}, cron={}, specific={}, check={}, reason={}", 
                                   userId, 
                                   validationResponse.getValidity().getValidPrompt(),
                                   validationResponse.getWhenNotify() != null && validationResponse.getWhenNotify().getTimeType() != null ?
                                       validationResponse.getWhenNotify().getTimeType().getCron() : false,
                                   validationResponse.getWhenNotify() != null && validationResponse.getWhenNotify().getTimeType() != null ?
                                       validationResponse.getWhenNotify().getTimeType().getSpecific() : false,
                                   validationResponse.getWhenNotify() != null && validationResponse.getWhenNotify().getTimeType() != null ?
                                       validationResponse.getWhenNotify().getTimeType().getCheck() : false,
                                   validationResponse.getValidity().getInvalidReason());
                    }
                    
                } catch (Exception parseException) {
                    logger.error("Failed to parse ChatGPT validation response: {}", parseException.getMessage());
                    logger.error("Raw response that failed to parse: {}", content);
                    logger.error("Parse exception details: ", parseException);
                    
                    // Save query anyway, but mark as invalid due to parsing error
                    if (user != null) {
                        var fallbackQuery = queryService.createFallbackQuery(user, sanitizedPrompt, request.getTimezone());
                        logger.info("Fallback query saved for user: {}", userId);
                        
                        // Restituisci errore al frontend anche per fallback query
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.error("Errore nell'elaborazione del prompt. Riprova con una formulazione diversa."));
                    }
                    
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Errore nel parsing della risposta AI. Riprova più tardi."));
                }
                
                // Return simple string response like commit 96e0d594
                return ResponseEntity.ok(ApiResponse.success("Prompt processed successfully", content));
            } else {
                logger.error("Empty or invalid response from ChatGPT for user: {}", userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.<String>error("No response from AI service"));
            }
                            
        } catch (Exception e) {
            logger.error("Error processing prompt for user {}: ", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<String>error("Error processing your request: " + e.getMessage()));
        }
    }

    /**
     * Pulisce la risposta JSON da ChatGPT rimuovendo caratteri non validi
     */
    private String cleanJsonResponse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // Rimuovi eventuali caratteri di controllo o non stampabili
        String cleaned = content.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        // Rimuovi eventuali prefissi/suffissi non JSON
        cleaned = cleaned.trim();
        
        // Se la risposta inizia con ```json, rimuovilo
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        // Trova l'inizio e la fine del JSON
        int startIndex = cleaned.indexOf('{');
        int endIndex = cleaned.lastIndexOf('}');
        
        if (startIndex >= 0 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1);
        }
        
        return cleaned.trim();
    }

    @GetMapping("/user-info")
    public ResponseEntity<ApiResponse<Object>> getUserInfo(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication != null ? authentication.getName() : "anonymous";
        String userEmail = (String) request.getAttribute("userEmail");
        
        Object userInfo = new Object() {
            public final String id = userId;
            public final String email = userEmail != null ? userEmail : userId;
            public final String[] roles = authentication != null ? 
                authentication.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toArray(String[]::new) : new String[]{"ROLE_ANONYMOUS"};
        };
        
        return ResponseEntity.ok(ApiResponse.success("User information retrieved", userInfo));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Service is running", "OK"));
    }
}