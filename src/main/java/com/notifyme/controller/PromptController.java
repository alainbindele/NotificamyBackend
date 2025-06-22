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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    
                    // Build response JSON object
                    Map<String, Object> responseData = buildResponseData(savedQuery, validationResponse);
                    
                    if (savedQuery.getIsValid()) {
                        logger.info("Valid prompt processed and saved with ID: {} for user: {}", savedQuery.getId(), userId);
                        return ResponseEntity.ok(ApiResponse.success("Prompt processed successfully", responseData));
                    } else {
                        logger.info("Invalid prompt processed and saved with ID: {} for user: {} - Reason: {}", 
                                   savedQuery.getId(), userId, 
                                   validationResponse.getValidity() != null ? validationResponse.getValidity().getInvalidReason() : "Unknown");
                        
                        return ResponseEntity.ok(ApiResponse.success("Prompt processed but not valid", responseData));
                    }
                    
                } catch (Exception parseException) {
                    logger.error("Failed to parse ChatGPT validation response for user {}: {}", userId, parseException.getMessage());
                    
                    // Fallback: save as invalid query with raw response
                    User user = userService.findOrCreateUser(userEmail != null ? userEmail : userId);
                    Query fallbackQuery = queryService.createFallbackQuery(user, sanitizedPrompt);
                    
                    Map<String, Object> fallbackData = new HashMap<>();
                    fallbackData.put("queryId", fallbackQuery.getId());
                    fallbackData.put("isValid", false);
                    fallbackData.put("error", "Validation parsing failed");
                    fallbackData.put("rawResponse", content);
                    
                    return ResponseEntity.ok(ApiResponse.success("Prompt processed with parsing issues", fallbackData));
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

    private Map<String, Object> buildResponseData(Query savedQuery, ChatGptValidationResponse validationResponse) {
        Map<String, Object> data = new HashMap<>();
        
        // Query information
        data.put("queryId", savedQuery.getId());
        data.put("isValid", savedQuery.getIsValid());
        data.put("prompt", savedQuery.getPrompt());
        data.put("createdAt", savedQuery.getCreatedAt().toString());
        
        // Scheduling information
        if (savedQuery.getCronParams() != null) {
            data.put("cronParams", savedQuery.getCronParams());
            data.put("scheduleType", "recurring");
        }
        if (savedQuery.getNextExecution() != null) {
            data.put("nextExecution", savedQuery.getNextExecution().toString());
            if (savedQuery.getCronParams() == null) {
                data.put("scheduleType", "specific");
            }
        }
        
        // Validation details from ChatGPT
        if (validationResponse != null) {
            Map<String, Object> validation = new HashMap<>();
            
            if (validationResponse.getSummary() != null) {
                validation.put("summary", validationResponse.getSummary().getText());
                validation.put("language", validationResponse.getSummary().getLanguage());
            }
            
            if (validationResponse.getValidity() != null) {
                validation.put("validPrompt", validationResponse.getValidity().getValidPrompt());
                validation.put("invalidReason", validationResponse.getValidity().getInvalidReason());
            }
            
            if (validationResponse.getWhenNotify() != null) {
                Map<String, Object> whenNotify = new HashMap<>();
                whenNotify.put("detected", validationResponse.getWhenNotify().getDetected());
                whenNotify.put("cronExpression", validationResponse.getWhenNotify().getCronExpression());
                whenNotify.put("dateTime", validationResponse.getWhenNotify().getDateTime());
                validation.put("whenNotify", whenNotify);
            }
            
            if (validationResponse.getMetadata() != null) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("confidenceScore", validationResponse.getMetadata().getConfidenceScore());
                metadata.put("tags", validationResponse.getMetadata().getTags());
                validation.put("metadata", metadata);
            }
            
            data.put("validation", validation);
        }
        
        return data;
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