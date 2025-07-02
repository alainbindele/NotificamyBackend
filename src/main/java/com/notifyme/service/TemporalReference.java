package com.notifyme.service;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TemporalReference {
    
    private LocalDateTime specificDateTime;
    private String cronExpression;
    private String checkIntervalCron;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    
    /**
     * Verifica se ha una data/ora specifica
     */
    public boolean hasSpecificDateTime() {
        return specificDateTime != null;
    }
    
    /**
     * Verifica se ha un pattern ricorrente
     */
    public boolean hasRecurringPattern() {
        return cronExpression != null && !cronExpression.trim().isEmpty();
    }
    
    /**
     * Verifica se ha un intervallo di controllo personalizzato
     */
    public boolean hasCheckInterval() {
        return checkIntervalCron != null && !checkIntervalCron.trim().isEmpty();
    }
    
    /**
     * Verifica se ha un periodo di validità
     */
    public boolean hasValidityPeriod() {
        return validFrom != null || validTo != null;
    }
    
    @Override
    public String toString() {
        return String.format("TemporalReference{specific=%s, cron='%s', checkCron='%s', validFrom=%s, validTo=%s}", 
                           specificDateTime, cronExpression, checkIntervalCron, validFrom, validTo);
    }
}