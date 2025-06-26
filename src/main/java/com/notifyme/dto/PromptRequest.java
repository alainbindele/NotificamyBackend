package com.notifyme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptRequest {
    
    @NotBlank(message = "Prompt cannot be empty")
    @Size(max = 2000, message = "Prompt cannot exceed 2000 characters")
    private String prompt;
    
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;
    
    @Size(max = 10, message = "Too many notification channels")
    private List<@Size(max = 20, message = "Channel name too long") String> channels;
    
    @Valid
    private Map<@Size(max = 20, message = "Channel key too long") String, 
               @Size(max = 500, message = "Channel configuration too long") String> channelConfigs;

    public PromptRequest(String prompt, String email) {
        this.prompt = prompt;
        this.email = email;
    }
}