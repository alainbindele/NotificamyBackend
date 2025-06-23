package com.notifyme.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SqsNotificationMessage {
    
    @JsonProperty("query_id")
    private Long queryId;
    
    @JsonProperty("user_email")
    private String userEmail;
    
    @JsonProperty("user_phone")
    private String userPhone;
    
    @JsonProperty("user_slack_webhook")
    private String userSlackWebhook;
    
    @JsonProperty("user_discord_webhook")
    private String userDiscordWebhook;
    
    private String prompt;

    public SqsNotificationMessage() {}

    public SqsNotificationMessage(Long queryId, String userEmail, String prompt) {
        this.queryId = queryId;
        this.userEmail = userEmail;
        this.prompt = prompt;
    }

    // Getters and setters
    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    
    public String getUserSlackWebhook() { return userSlackWebhook; }
    public void setUserSlackWebhook(String userSlackWebhook) { this.userSlackWebhook = userSlackWebhook; }
    
    public String getUserDiscordWebhook() { return userDiscordWebhook; }
    public void setUserDiscordWebhook(String userDiscordWebhook) { this.userDiscordWebhook = userDiscordWebhook; }
    
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}