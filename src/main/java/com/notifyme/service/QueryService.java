package com.notifyme.service;

import com.notifyme.dto.ChatGptValidationResponse;
import com.notifyme.entity.Query;
import com.notifyme.entity.User;
import com.notifyme.repository.QueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@Transactional
public class QueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);
    
    @Autowired
    private QueryRepository queryRepository;
    
    public Query createQuery(User user, String prompt, ChatGptValidationResponse validationResponse) {
        Query query = new Query(user, prompt);
        
        // Imposta la validità basata sulla risposta di ChatGPT
        boolean isValid = validationResponse.getValidity() != null && 
                         Boolean.TRUE.equals(validationResponse.getValidity().getValidPrompt());
        
        query.setIsValid(isValid);
        
        if (isValid && validationResponse.getWhenNotify() != null) {
            ChatGptValidationResponse.WhenNotify whenNotify = validationResponse.getWhenNotify();
            
            // Imposta i parametri cron se è di tipo CRON
            if ("CRON".equals(whenNotify.getDetected()) && whenNotify.getCronExpression() != null) {
                query.setCronParams(whenNotify.getCronExpression());
                // Per i cron job, calcola la prossima esecuzione (implementazione semplificata)
                query.setNextExecution(calculateNextCronExecution(whenNotify.getCronExpression()));
            }
            
            // Imposta la data/ora specifica se è di tipo SPECIFIC
            if ("SPECIFIC".equals(whenNotify.getDetected()) && whenNotify.getDateTime() != null) {
                try {
                    LocalDateTime specificDateTime = LocalDateTime.parse(
                        whenNotify.getDateTime(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );
                    query.setNextExecution(specificDateTime);
                } catch (DateTimeParseException e) {
                    logger.warn("Failed to parse specific datetime: {}", whenNotify.getDateTime(), e);
                }
            }
        }
        
        Query savedQuery = queryRepository.save(query);
        logger.info("Created new query with ID: {} for user: {} (valid: {})", 
                   savedQuery.getId(), user.getEmail(), isValid);
        
        return savedQuery;
    }
    
    public Query createFallbackQuery(User user, String prompt) {
        Query query = new Query(user, prompt);
        query.setIsValid(false);
        
        Query savedQuery = queryRepository.save(query);
        logger.info("Created fallback query with ID: {} for user: {} (invalid due to parsing error)", 
                   savedQuery.getId(), user.getEmail());
        
        return savedQuery;
    }
    
    public List<Query> findByUser(User user) {
        return queryRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public List<Query> findValidQueriesByUser(User user) {
        return queryRepository.findByUserAndIsValid(user, true);
    }
    
    public List<Query> findQueriesReadyForExecution() {
        return queryRepository.findByIsValidAndNextExecutionBefore(true, LocalDateTime.now());
    }
    
    private LocalDateTime calculateNextCronExecution(String cronExpression) {
        // Implementazione semplificata per calcolare la prossima esecuzione
        // In un'implementazione reale, useresti una libreria come Quartz o Spring Scheduler
        
        try {
            // Per ora, aggiungiamo semplicemente 1 ora come esempio
            // Dovresti implementare un parser cron completo qui
            return LocalDateTime.now(ZoneId.of("UTC")).plusHours(1);
        } catch (Exception e) {
            logger.warn("Failed to calculate next cron execution for: {}", cronExpression, e);
            return LocalDateTime.now().plusDays(1); // Fallback
        }
    }
}