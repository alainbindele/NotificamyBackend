package com.notifyme.dto;

import com.notifyme.entity.TQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {
    
    private Long id;
    private String prompt;
    private Boolean closed;
    private Boolean isValid;
    private Boolean cron;
    private Boolean dateSpecific;
    private Boolean toCheck;
    private String cronParams;
    private LocalDateTime nextExecution;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String enabledChannels;
    private String timezone;
    private String summaryText;
    private String language;
    private String category;
    private String modelVersion;
    private Double confidenceScore;
    private String invalidReason;
    private LocalDateTime createdAt;
    
    // Costruttore da TQuery entity
    public QueryResponse(TQuery query) {
        this.id = query.getId();
        this.prompt = query.getPrompt();
        this.closed = query.getClosed();
        this.isValid = query.getIsValid();
        this.cron = query.getCron();
        this.dateSpecific = query.getDateSpecific();
        this.toCheck = query.getToCheck();
        this.cronParams = query.getCronParams();
        this.nextExecution = query.getNextExecution();
        this.validFrom = query.getValidFrom();
        this.validTo = query.getValidTo();
        this.enabledChannels = query.getEnabledChannels();
        this.timezone = query.getTimezone();
        this.summaryText = query.getSummaryText();
        this.language = query.getLanguage();
        this.category = query.getCategory();
        this.modelVersion = query.getModelVersion();
        this.confidenceScore = query.getConfidenceScore();
        this.invalidReason = query.getInvalidReason();
        this.createdAt = query.getCreatedAt();
    }
    
    // Metodo statico per conversione
    public static QueryResponse fromEntity(TQuery query) {
        return new QueryResponse(query);
    }
}