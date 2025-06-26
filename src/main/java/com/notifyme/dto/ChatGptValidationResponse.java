package com.notifyme.dto;

import com.fasterxml.jackson.annotation.JsonProperty;


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
    
    public static class WhenNotify {
        private String detected;
        
        @JsonProperty("cron_expression")
        private String cronExpression;
        
        @JsonProperty("date_time")
        private String dateTime;
        
        // Getters and setters
        public String getDetected() { return detected; }
        public void setDetected(String detected) { this.detected = detected; }
        
        public String getCronExpression() { return cronExpression; }
        public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
        
        public String getDateTime() { return dateTime; }
        public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    }
    
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
        
        // Getters and setters
        public Boolean getOutOfBoundsPromptLength() { return outOfBoundsPromptLength; }
        public void setOutOfBoundsPromptLength(Boolean outOfBoundsPromptLength) { this.outOfBoundsPromptLength = outOfBoundsPromptLength; }
        
        public Boolean getOffensiveLanguageDetected() { return offensiveLanguageDetected; }
        public void setOffensiveLanguageDetected(Boolean offensiveLanguageDetected) { this.offensiveLanguageDetected = offensiveLanguageDetected; }
        
        public Boolean getNastyInstructionDetected() { return nastyInstructionDetected; }
        public void setNastyInstructionDetected(Boolean nastyInstructionDetected) { this.nastyInstructionDetected = nastyInstructionDetected; }
        
        public Boolean getPurposeValid() { return purposeValid; }
        public void setPurposeValid(Boolean purposeValid) { this.purposeValid = purposeValid; }
        
        public Boolean getReasonableUsage() { return reasonableUsage; }
        public void setReasonableUsage(Boolean reasonableUsage) { this.reasonableUsage = reasonableUsage; }
        
        public Boolean getSelfEnforcing() { return selfEnforcing; }
        public void setSelfEnforcing(Boolean selfEnforcing) { this.selfEnforcing = selfEnforcing; }
        
        public Boolean getValidPrompt() { return validPrompt; }
        public void setValidPrompt(Boolean validPrompt) { this.validPrompt = validPrompt; }
        
        public String getInvalidReason() { return invalidReason; }
        public void setInvalidReason(String invalidReason) { this.invalidReason = invalidReason; }
    }
    
    public static class Summary {
        private String text;
        private String language;
        private String category;
        
        // Getters and setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
    
    public static class Metadata {
        @JsonProperty("model_version")
        private String modelVersion;
        
        @JsonProperty("confidence_score")
        private Double confidenceScore;
        
        @JsonProperty("policy_enforced")
        private Boolean policyEnforced;
        
        private String[] tags;
        
        // Getters and setters
        public String getModelVersion() { return modelVersion; }
        public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
        
        public Double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
        
        public Boolean getPolicyEnforced() { return policyEnforced; }
        public void setPolicyEnforced(Boolean policyEnforced) { this.policyEnforced = policyEnforced; }
        
        public String[] getTags() { return tags; }
        public void setTags(String[] tags) { this.tags = tags; }
    }
    
    // Main class getters and setters
    public String getResponseType() { return responseType; }
    public void setResponseType(String responseType) { this.responseType = responseType; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    
    public WhenNotify getWhenNotify() { return whenNotify; }
    public void setWhenNotify(WhenNotify whenNotify) { this.whenNotify = whenNotify; }
    
    public Validity getValidity() { return validity; }
    public void setValidity(Validity validity) { this.validity = validity; }
    
    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }
    
    public Metadata getMetadata() { return metadata; }
    public void setMetadata(Metadata metadata) { this.metadata = metadata; }
}