package com.notifyme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    
    private Long id;
    private String email;
    private String displayName;
    private LocalDateTime createdAt;
    private String discordWebhook;
    private String slackWebhook;
    private String phone;
    
    // Costruttore per mascherare i webhook sensibili
    public UserProfileResponse(Long id, String email, String displayName, LocalDateTime createdAt, 
                              String discordWebhook, String slackWebhook, String phone, boolean maskSensitive) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
        
        if (maskSensitive) {
            this.discordWebhook = maskWebhook(discordWebhook);
            this.slackWebhook = maskWebhook(slackWebhook);
            this.phone = maskPhone(phone);
        } else {
            this.discordWebhook = discordWebhook;
            this.slackWebhook = slackWebhook;
            this.phone = phone;
        }
    }
    
    private String maskWebhook(String webhook) {
        if (webhook == null || webhook.trim().isEmpty() || webhook.length() < 10) {
            return webhook;
        }
        return webhook.substring(0, 10) + "***" + webhook.substring(webhook.length() - 5);
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty() || phone.length() < 6) {
            return phone;
        }
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 3);
    }
}