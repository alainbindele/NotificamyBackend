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
    private TUser user; // Cambiato da TUser a user per Spring Data JPA
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private TQuery query; // Cambiato da TQuery a query per Spring Data JPA
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    public TNotification(TUser user, TQuery query, String subject, String content) {
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