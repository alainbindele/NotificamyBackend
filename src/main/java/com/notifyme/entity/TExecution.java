package com.notifyme.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "executions")
@Data
@NoArgsConstructor
public class TExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private TQuery query; // Cambiato da TQuery a query per Spring Data JPA
    
    @Column(name = "executed_at")
    private LocalDateTime executedAt;
    
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status = ExecutionStatus.SUCCESS;
    
    @Column(columnDefinition = "TEXT")
    private String response;
    
    public enum ExecutionStatus {
        SUCCESS, FAILED
    }
    
    public TExecution(TQuery query, ExecutionStatus status, String response) {
        this.query = query;
        this.status = status;
        this.response = response;
        this.executedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
        }
    }
}