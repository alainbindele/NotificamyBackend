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
public class Query {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String prompt;
    
    @Column(name = "is_valid")
    private Boolean isValid = false;
    
    @Column(name = "cron_params", length = 30)
    private String cronParams;
    
    @Column(name = "next_execution")
    private LocalDateTime nextExecution;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "query", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Execution> executions;
    
    @OneToMany(mappedBy = "query", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications;
    
    public Query(User user, String prompt) {
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