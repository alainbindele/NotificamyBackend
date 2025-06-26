package com.notifyme.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private Query query;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    public Notification(User user, Query query, String subject, String content) {
        this.user = user;
        this.query = query;
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