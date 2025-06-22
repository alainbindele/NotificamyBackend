package com.notifyme.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "executions")
public class Execution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;
    
    @Column(name = "executed_at")
    private LocalDateTime executedAt;
    
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status = ExecutionStatus.SUCCESS;
    
    @Column(columnDefinition = "TEXT")
    private String response;
    
    public enum ExecutionStatus {
        SUCCESS, FAILED
    }
    
    public Execution() {
        this.executedAt = LocalDateTime.now();
    }
    
    public Execution(Query query, ExecutionStatus status, String response) {
        this();
        this.query = query;
        this.status = status;
        this.response = response;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Query getQuery() { return query; }
    public void setQuery(Query query) { this.query = query; }
    
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
    
    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }
    
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
}