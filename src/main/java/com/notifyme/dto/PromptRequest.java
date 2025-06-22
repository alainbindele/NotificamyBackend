package com.notifyme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

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

    public PromptRequest() {}

    public PromptRequest(String prompt, String email) {
        this.prompt = prompt;
        this.email = email;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public Map<String, String> getChannelConfigs() {
        return channelConfigs;
    }

    public void setChannelConfigs(Map<String, String> channelConfigs) {
        this.channelConfigs = channelConfigs;
    }
}