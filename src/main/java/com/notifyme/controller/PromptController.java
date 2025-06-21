package com.notifyme.controller;

import com.notifyme.dto.ApiResponse;
import com.notifyme.dto.ChatGptResponse;
import com.notifyme.dto.PromptRequest;
import com.notifyme.service.ChatGptService;
import com.notifyme.service.SecurityService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<ApiResponse<String>>> processPrompt(@Valid @RequestBody PromptRequest request) {
        try {
            logger.info("Received prompt request from email: {}", request.getEmail());

            // Validate prompt for security
            if (!securityService.isValidPrompt(request.getPrompt())) {
                logger.warn("Invalid or potentially malicious prompt detected: {}", request.getPrompt());
                return Mono.just(ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid prompt. Please check your input for security violations.")));
            }

            // Sanitize input
            String sanitizedPrompt = securityService.sanitizeInput(request.getPrompt());
            
            logger.info("Processing sanitized prompt: {}", sanitizedPrompt);

            // Send to ChatGPT
            return chatGptService.sendPromptToChatGpt(sanitizedPrompt)
                    .map(response -> {
                        try {
                            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                                String content = response.getChoices().get(0).getMessage().getContent();
                                logger.info("ChatGPT response received successfully");
                                return ResponseEntity.ok(ApiResponse.success("Prompt processed successfully", content));
                            } else {
                                logger.error("Empty response from ChatGPT");
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(ApiResponse.<String>error("No response from AI service"));
                            }
                        } catch (Exception e) {
                            logger.error("Error processing ChatGPT response: ", e);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(ApiResponse.<String>error("Error processing AI response"));
                        }
                    })
                    .onErrorResume(error -> {
                        logger.error("Error processing prompt: ", error);
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.<String>error("Error processing your request: " + error.getMessage())));
                    });
        } catch (Exception e) {
            logger.error("Unexpected error in processPrompt: ", e);
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<String>error("Unexpected error occurred")));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Service is running", "OK"));
    }
}