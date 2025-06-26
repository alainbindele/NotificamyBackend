package com.notifyme.service;

import com.notifyme.dto.ChatGptValidationResponse;
import com.notifyme.entity.TQuery;
import com.notifyme.entity.TUser;
import com.notifyme.repository.QueryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@Transactional
public class QueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);
    
    @Autowired
    private QueryRepository queryRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public TQuery createQuery(TUser TUser, String prompt, ChatGptValidationResponse validationResponse) {
        TQuery TQuery = new TQuery(TUser, prompt);
        
        // Map validation response to query entity
        mapValidationResponseToQuery(TQuery, validationResponse);
        
        TQuery savedTQuery = queryRepository.save(TQuery);
        logger.info("Created new query with ID: {} for user: {} (valid: {})", 
                   savedTQuery.getId(), TUser.getEmail(), savedTQuery.getIsValid());
        
        return savedTQuery;
    }
    
    private void mapValidationResponseToQuery(TQuery TQuery, ChatGptValidationResponse response) {
        if (response == null) {
            TQuery.setIsValid(false);
            return;
        }
        
        // Map validity fields
        if (response.getValidity() != null) {
            ChatGptValidationResponse.Validity validity = response.getValidity();
            
            TQuery.setIsValid(Boolean.TRUE.equals(validity.getValidPrompt()));
            TQuery.setOutOfBoundsPromptLength(validity.getOutOfBoundsPromptLength());
            TQuery.setOffensiveLanguageDetected(validity.getOffensiveLanguageDetected());
            TQuery.setNastyInstructionDetected(validity.getNastyInstructionDetected());
            TQuery.setPurposeValid(validity.getPurposeValid());
            TQuery.setReasonableUsage(validity.getReasonableUsage());
            TQuery.setSelfEnforcing(validity.getSelfEnforcing());
            TQuery.setInvalidReason(validity.getInvalidReason());
        }
        
        // Map when_notify fields
        if (response.getWhenNotify() != null) {
            ChatGptValidationResponse.WhenNotify whenNotify = response.getWhenNotify();
            
            // Map type flags
            if (whenNotify.getTimeType() != null) {
                TQuery.setCron(Boolean.TRUE.equals(whenNotify.getTimeType().getCron()));
                TQuery.setDateSpecific(Boolean.TRUE.equals(whenNotify.getTimeType().getSpecific()));
                TQuery.setToCheck(Boolean.TRUE.equals(whenNotify.getTimeType().getCheck()));
            }
            
            // Map cron expression
            if (whenNotify.getCronExpression() != null && !whenNotify.getCronExpression().trim().isEmpty()) {
                TQuery.setCronParams(whenNotify.getCronExpression().trim());
                TQuery.setNextExecution(calculateNextCronExecution(whenNotify.getCronExpression()));
            }
            
            // Map specific datetime
            if (whenNotify.getDateTime() != null && !whenNotify.getDateTime().trim().isEmpty()) {
                try {
                    LocalDateTime specificDateTime = LocalDateTime.parse(
                        whenNotify.getDateTime().trim(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );
                    TQuery.setSpecificDatetime(specificDateTime);
                    TQuery.setNextExecution(specificDateTime);
                } catch (DateTimeParseException e) {
                    logger.warn("Failed to parse specific datetime: {}", whenNotify.getDateTime(), e);
                }
            }
            
            // Map validity period
            if (whenNotify.getStartDate() != null && !whenNotify.getStartDate().trim().isEmpty()) {
                try {
                    LocalDateTime startDate = LocalDateTime.parse(
                        whenNotify.getStartDate().trim(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );
                    TQuery.setValidFrom(startDate);
                } catch (DateTimeParseException e) {
                    logger.warn("Failed to parse start date: {}", whenNotify.getStartDate(), e);
                }
            }
            
            if (whenNotify.getEndDate() != null && !whenNotify.getEndDate().trim().isEmpty()) {
                try {
                    LocalDateTime endDate = LocalDateTime.parse(
                        whenNotify.getEndDate().trim(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );
                    TQuery.setValidTo(endDate);
                } catch (DateTimeParseException e) {
                    logger.warn("Failed to parse end date: {}", whenNotify.getEndDate(), e);
                }
            }
        }
        
        // Map summary fields
        if (response.getSummary() != null) {
            ChatGptValidationResponse.Summary summary = response.getSummary();
            TQuery.setSummaryText(summary.getText());
            TQuery.setLanguage(summary.getLanguage());
            TQuery.setCategory(summary.getCategory());
        }
        
        // Map metadata fields
        if (response.getMetadata() != null) {
            ChatGptValidationResponse.Metadata metadata = response.getMetadata();
            TQuery.setModelVersion(metadata.getModelVersion());
            TQuery.setConfidenceScore(metadata.getConfidenceScore());
            TQuery.setPolicyEnforced(metadata.getPolicyEnforced());
            
            // Convert tags array to JSON string
            if (metadata.getTags() != null && metadata.getTags().length > 0) {
                try {
                    TQuery.setTags(objectMapper.writeValueAsString(metadata.getTags()));
                } catch (Exception e) {
                    logger.warn("Failed to serialize tags: {}", e.getMessage());
                }
            }
        }
        
        logger.info("Mapped ChatGPT validation response to query: cron={}, specific={}, check={}, valid={}", 
                   TQuery.getCron(), TQuery.getDateSpecific(), TQuery.getToCheck(), TQuery.getIsValid());
    }
    
    public TQuery createFallbackQuery(TUser TUser, String prompt) {
        TQuery TQuery = new TQuery(TUser, prompt);
        TQuery.setIsValid(false);
        TQuery.setInvalidReason("Failed to parse ChatGPT validation response");
        
        TQuery savedTQuery = queryRepository.save(TQuery);
        logger.info("Created fallback query with ID: {} for user: {} (invalid due to parsing error)", 
                   savedTQuery.getId(), TUser.getEmail());
        
        return savedTQuery;
    }
    
    public List<TQuery> findByUser(TUser TUser) {
        return queryRepository.findByUserOrderByCreatedAtDesc(TUser);
    }
    
    public List<TQuery> findValidQueriesByUser(TUser TUser) {
        return queryRepository.findByUserAndIsValid(TUser, true);
    }
    
    public List<TQuery> findQueriesReadyForExecution() {
        return queryRepository.findByIsValidAndNextExecutionBefore(true, LocalDateTime.now());
    }
    
    private LocalDateTime calculateNextCronExecution(String cronExpression) {
        // Implementazione semplificata per calcolare la prossima esecuzione
        // In un'implementazione reale, useresti una libreria come Quartz o Spring Scheduler
        
        try {
            // Per ora, aggiungiamo semplicemente 1 ora come esempio
            // Dovresti implementare un parser cron completo qui
            return LocalDateTime.now().plusHours(1);
        } catch (Exception e) {
            logger.warn("Failed to calculate next cron execution for: {}", cronExpression, e);
            return LocalDateTime.now().plusHours(1); // Fallback
        }
    }
}