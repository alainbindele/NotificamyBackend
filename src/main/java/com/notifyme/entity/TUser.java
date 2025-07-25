package com.notifyme.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class TUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(name = "display_name", length = 100)
    private String displayName;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "discord_webhook", columnDefinition = "TEXT")
    private String discordWebhook = "";
    
    @Column(name = "slack_webhook", columnDefinition = "TEXT")
    private String slackWebhook = "";
    
    @Column(name = "whatsapp_phone", length = 30)
    private String phone = "";
    
    @Column(name = "auth_subject", length = 255)
    private String authSubject; // JWT subject per riconoscimento futuro
    
    @Column(name = "display_name", length = 100)
    private String displayName;
    
    public TUser(String email) {
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }
    
    public TUser(String email, String authSubject) {
        this.email = email;
        this.authSubject = authSubject;
        this.createdAt = LocalDateTime.now();
    }
    
    // Custom setters to handle null values
    public void setDiscordWebhook(String discordWebhook) {
        this.discordWebhook = discordWebhook != null ? discordWebhook : "";
    }
    
    public void setSlackWebhook(String slackWebhook) {
        this.slackWebhook = slackWebhook != null ? slackWebhook : "";
    }
    
    public void setPhone(String phone) {
        this.phone = phone != null ? phone : "";
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}