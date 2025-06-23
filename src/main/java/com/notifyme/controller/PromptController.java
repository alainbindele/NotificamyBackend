package com.notifyme.controller;

import com.notifyme.dto.ApiResponse;
import com.notifyme.dto.ChatGptResponse;
import com.notifyme.dto.ChatGptValidationResponse;
import com.notifyme.dto.PromptRequest;
import com.notifyme.entity.User;
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
            User user = null;
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
            ChatGptResponse chatGptResponse = chatGptService.sendPromptToChatGptSync(sanitizedPrompt);
            
            if (chatGptResponse != null && chatGptResponse.getChoices() != null && !chatGptResponse.getChoices().isEmpty()) {
                String content = chatGptResponse.getChoices().get(0).getMessage().getContent();
                logger.info("ChatGPT response received successfully for user: {}", userId);
                
                // Parse ChatGPT response to extract validation data
                try {
                    ChatGptValidationResponse validationResponse = objectMapper.readValue(content, ChatGptValidationResponse.class);
                    
                    // Save query to database with validation data
                    if (user != null) {
                        queryService.createQuery(user, sanitizedPrompt, validationResponse);
                        logger.info("Query saved successfully for user: {}", userId);
                    }
                    
                } catch (Exception parseException) {
                    logger.warn("Failed to parse ChatGPT validation response, saving as fallback query: {}", parseException.getMessage());
                    
                    // Save query anyway, but mark as invalid due to parsing error
                    if (user != null) {
                        queryService.createFallbackQuery(user, sanitizedPrompt);
                        logger.info("Fallback query saved for user: {}", userId);
                    }
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