package com.notifyme.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
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
    
    public Notification() {
        this.sentAt = LocalDateTime.now();
    }
    
    public Notification(User user, Query query, String subject, String content) {
        this();
        this.user = user;
        this.query = query;
        this.subject = subject;
        this.content = content;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Query getQuery() { return query; }
    public void setQuery(Query query) { this.query = query; }
    
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}