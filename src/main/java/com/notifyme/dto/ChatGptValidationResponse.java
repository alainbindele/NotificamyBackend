package com.notifyme.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatGptValidationResponse {
    
    @JsonProperty("response_type")
    private String responseType;
    
    private String timestamp;
    
    @JsonProperty("generated_by")
    private String generatedBy;
    
    @JsonProperty("when_notify")
    private WhenNotify whenNotify;
    
    private Validity validity;
    
    private Summary summary;
    
    private Metadata metadata;
    
    @Data
    public static class WhenNotify {
        private TimeType timeType;
        
        @JsonProperty("cron_expression")
        private String cronExpression;
        
        @JsonProperty("date_time")
        private String dateTime;
        
        @JsonProperty("start_date")
        private String startDate;
        
        @JsonProperty("end_date")
        private String endDate;
        
        @Data
        public static class TimeType {
            @JsonProperty("CRON")
            private Boolean cron;
            
            @JsonProperty("SPECIFIC")
            private Boolean specific;
            
            @JsonProperty("CHECK")
            private Boolean check;
        }
    }
    
    @Data
    public static class Validity {
        @JsonProperty("out_of_bounds_prompt_length")
        private Boolean outOfBoundsPromptLength;
        
        @JsonProperty("offensive_language_detected")
        private Boolean offensiveLanguageDetected;
        
        @JsonProperty("nasty_instruction_detected")
        private Boolean nastyInstructionDetected;
        
        @JsonProperty("purpose_valid")
        private Boolean purposeValid;
        
        @JsonProperty("reasonable_usage")
        private Boolean reasonableUsage;
        
        @JsonProperty("self_enforcing")
        private Boolean selfEnforcing;
        
        @JsonProperty("valid_prompt")
        private Boolean validPrompt;
        
        @JsonProperty("invalid_reason")
        private String invalidReason;
    }
    
    @Data
    public static class Summary {
        private String text;
        private String language;
        private String category;
    }
    
    @Data
    public static class Metadata {
        @JsonProperty("model_version")
        private String modelVersion;
        
        @JsonProperty("confidence_score")
        private Double confidenceScore;
        
        @JsonProperty("policy_enforced")
        private Boolean policyEnforced;
        
        private String[] tags;
    }
}