package com.notifyme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyme.dto.ApiResponse;
import com.notifyme.dto.ChatGptResponse;
import com.notifyme.dto.ChatGptValidationResponse;
import com.notifyme.dto.PromptRequest;
import com.notifyme.entity.Query;
import com.notifyme.entity.User;
import com.notifyme.service.ChatGptService;
import com.notifyme.service.QueryService;
import com.notifyme.service.SecurityService;
import com.notifyme.service.UserService;
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

import java.util.List;

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
    public ResponseEntity<ApiResponse<Object>> processPrompt(@Valid @RequestBody PromptRequest request, 
                                                           HttpServletRequest httpRequest) {
        
        // Get authenticated user information
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication != null ? authentication.getName() : "anonymous";
        String userEmail = (String) httpRequest.getAttribute("userEmail");
        
        logger.info("Received prompt request from authenticated user: {} ({})", userEmail, userId);

        try {
            // Validate prompt for security
            if (!securityService.isValidPrompt(request.getPrompt())) {
                logger.warn("Invalid or potentially malicious prompt detected from user {}: {}", userId, request.getPrompt());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid prompt. Please check your input for security violations."));
            }

            // Sanitize input
            String sanitizedPrompt = securityService.sanitizeInput(request.getPrompt());
            
            logger.info("Processing sanitized prompt for user {}: {}", userId, sanitizedPrompt);

            // Send to ChatGPT synchronously
            ChatGptResponse chatGptResponse = chatGptService.sendPromptToChatGptSync(sanitizedPrompt);
            
            if (chatGptResponse != null && chatGptResponse.getChoices() != null && !chatGptResponse.getChoices().isEmpty()) {
                String content = chatGptResponse.getChoices().get(0).getMessage().getContent();
                logger.info("ChatGPT response received successfully for user: {}", userId);
                
                try {
                    // Parse the ChatGPT response as validation response
                    ChatGptValidationResponse validationResponse = objectMapper.readValue(content, ChatGptValidationResponse.class);
                    
                    // Find or create user
                    User user = userService.findOrCreateUser(userEmail != null ? userEmail : userId);
                    
                    // Create and save query with validation results
                    Query savedQuery = queryService.createQuery(user, sanitizedPrompt, validationResponse);
                    
                    // Prepare response data
                    Object responseData = new Object() {
                        public final Long queryId = savedQuery.getId();
                        public final Boolean isValid = savedQuery.getIsValid();
                        public final String cronParams = savedQuery.getCronParams();
                        public final String nextExecution = savedQuery.getNextExecution() != null ? 
                            savedQuery.getNextExecution().toString() : null;
                        public final ChatGptValidationResponse validation = validationResponse;
                    };
                    
                    if (savedQuery.getIsValid()) {
                        logger.info("Valid prompt processed and saved with ID: {} for user: {}", savedQuery.getId(), userId);
                        return ResponseEntity.ok(ApiResponse.success("Prompt processed and scheduled successfully", responseData));
                    } else {
                        logger.info("Invalid prompt processed and saved with ID: {} for user: {} - Reason: {}", 
                                   savedQuery.getId(), userId, 
                                   validationResponse.getValidity() != null ? validationResponse.getValidity().getInvalidReason() : "Unknown");
                        return ResponseEntity.ok(ApiResponse.success("Prompt processed but not valid for scheduling", responseData));
                    }
                    
                } catch (Exception parseException) {
                    logger.error("Failed to parse ChatGPT validation response for user {}: {}", userId, parseException.getMessage());
                    
                    // Fallback: save as invalid query with raw response
                    User user = userService.findOrCreateUser(userEmail != null ? userEmail : userId);
                    Query fallbackQuery = queryService.createFallbackQuery(user, sanitizedPrompt);
                    
                    Object responseData = new Object() {
                        public final Long queryId = fallbackQuery.getId();
                        public final Boolean isValid = false;
                        public final String rawResponse = content;
                        public final String error = "Failed to parse validation response";
                    };
                    
                    return ResponseEntity.ok(ApiResponse.success("Prompt processed but validation parsing failed", responseData));
                }
                
            } else {
                logger.error("Empty or invalid response from ChatGPT for user: {}", userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.<Object>error("No response from AI service"));
            }
                            
        } catch (Exception e) {
            logger.error("Error processing prompt for user {}: ", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Object>error("Error processing your request: " + e.getMessage()));
        }
    }

    @GetMapping("/user-queries")
    public ResponseEntity<ApiResponse<List<Query>>> getUserQueries(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication != null ? authentication.getName() : "anonymous";
        String userEmail = (String) request.getAttribute("userEmail");
        
        try {
            User user = userService.findByEmail(userEmail != null ? userEmail : userId);
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.success("No queries found", List.of()));
            }
            
            List<Query> queries = queryService.findByUser(user);
            return ResponseEntity.ok(ApiResponse.success("User queries retrieved", queries));
            
        } catch (Exception e) {
            logger.error("Error retrieving queries for user {}: ", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<Query>>error("Error retrieving queries: " + e.getMessage()));
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