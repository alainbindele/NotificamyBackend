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
}