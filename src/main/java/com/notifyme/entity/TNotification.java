package com.notifyme.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class TNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private TUser TUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private TQuery TQuery;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    public TNotification(TUser TUser, TQuery TQuery, String subject, String content) {
        this.TUser = TUser;
        this.TQuery = TQuery;
        this.subject = subject;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}