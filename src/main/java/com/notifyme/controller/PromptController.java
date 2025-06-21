package com.notifyme.controller;

import com.notifyme.dto.ApiResponse;
import com.notifyme.dto.ChatGptResponse;
import com.notifyme.dto.PromptRequest;
import com.notifyme.service.ChatGptService;
import com.notifyme.service.SecurityService;
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
@CrossOrigin(origins = "*", maxAge = 3600)
public class PromptController {

    private static final Logger logger = LoggerFactory.getLogger(PromptController.class);

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ChatGptService chatGptService;

    @PostMapping("/validate-prompt")
    public ResponseEntity<ApiResponse<String>> processPrompt(@Valid @RequestBody PromptRequest request, 
                                                           HttpServletRequest httpRequest) {
        
        // Get authenticated user information
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
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
        String userId = authentication.getName();
        String userEmail = (String) request.getAttribute("userEmail");
        
        Object userInfo = new Object() {
            public final String id = userId;
            public final String email = userEmail;
            public final String[] roles = authentication.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toArray(String[]::new);
        };
        
        return ResponseEntity.ok(ApiResponse.success("User information retrieved", userInfo));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Service is running", "OK"));
    }
}