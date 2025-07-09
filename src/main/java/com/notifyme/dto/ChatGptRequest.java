package com.notifyme.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatGptRequest {
    
    @JsonProperty("policy")
    private String policy;
    
    @JsonProperty("client_prompt")
    private String clientPrompt;
    
    @JsonProperty("user_timezone")
    private String userTimezone;
    
    @JsonProperty("utc_timestamp")
    private String utcTimestamp;
    
    @JsonProperty("user_timestamp")
    private String userTimestamp;
    
    public ChatGptRequest(String policy, String clientPrompt, String userTimezone, String utcTimestamp, String userTimestamp) {
        this.policy = policy;
        this.clientPrompt = clientPrompt;
        this.userTimezone = userTimezone;
        this.utcTimestamp = utcTimestamp;
        this.userTimestamp = userTimestamp;
    }
}