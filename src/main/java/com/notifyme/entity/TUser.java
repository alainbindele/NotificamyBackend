package com.notifyme.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "discord_webhook", columnDefinition = "TEXT")
    private String discordWebhook = "";
    
    @Column(name = "slack_webhook", columnDefinition = "TEXT")
    private String slackWebhook = "";
    
    @Column(name = "whatsapp_phone", length = 30)
    private String phone = "";
    
    @OneToMany(mappedBy = "TUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TQuery> queries;
    
    @OneToMany(mappedBy = "TUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TNotification> TNotifications;
    
    public TUser(String email) {
        this.email = email;
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