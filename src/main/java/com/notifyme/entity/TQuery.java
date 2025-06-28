package com.notifyme.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "queries")
@Data
@NoArgsConstructor
public class TQuery {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private TUser user; // Cambiato da TUser a user per Spring Data JPA
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String prompt;

    // Status fields
    @Column(name = "closed")
    private Boolean closed = false;

    @Column(name = "is_valid")
    private Boolean isValid = false;

    // Type flags from ChatGPT response
    @Column(name = "cron")
    private Boolean cron = false;

    @Column(name = "date_specific")
    private Boolean dateSpecific = false;

    @Column(name = "to_check")
    private Boolean toCheck = false;
    
    // Scheduling fields
    @Column(name = "cron_params", length = 100)
    private String cronParams;
    
    @Column(name = "next_execution")
    private LocalDateTime nextExecution;

    // Validity period
    @Column(name = "valid_from")
    private LocalDateTime validFrom;
    
    @Column(name = "valid_to")
    private LocalDateTime validTo;

    // Notification channels
    @Column(name = "enabled_channels", columnDefinition = "TEXT")
    private String enabledChannels;

    // ChatGPT validation fields
    @Column(name = "out_of_bounds_prompt_length")
    private Boolean outOfBoundsPromptLength;
    
    @Column(name = "offensive_language_detected")
    private Boolean offensiveLanguageDetected;
    
    @Column(name = "nasty_instruction_detected")
    private Boolean nastyInstructionDetected;
    
    @Column(name = "purpose_valid")
    private Boolean purposeValid;
    
    @Column(name = "reasonable_usage")
    private Boolean reasonableUsage;
    
    @Column(name = "self_enforcing")
    private Boolean selfEnforcing;
    
    @Column(name = "invalid_reason", columnDefinition = "TEXT")
    private String invalidReason;

    // Summary fields
    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;
    
    @Column(name = "language", length = 10)
    private String language;
    
    @Column(name = "category", length = 50)
    private String category;

    // Metadata fields
    @Column(name = "model_version", length = 50)
    private String modelVersion;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Column(name = "policy_enforced")
    private Boolean policyEnforced;
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON array as string
    
    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "query", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TExecution> executions;
    
    @OneToMany(mappedBy = "query", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TNotification> notifications;
    
    public TQuery(TUser user, String prompt) {
        this.user = user;
        this.prompt = prompt;
        this.createdAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}