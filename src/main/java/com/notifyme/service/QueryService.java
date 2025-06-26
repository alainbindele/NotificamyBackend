package com.notifyme.service;

import com.notifyme.dto.ChatGptValidationResponse;
import com.notifyme.entity.Query;
import com.notifyme.entity.User;
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
    
    public Query createQuery(User user, String prompt, ChatGptValidationResponse validationResponse) {
        Query query = new Query(user, prompt);
        
        // Map validation response to query entity
        mapValidationResponseToQuery(query, validationResponse);
        
        Query savedQuery = queryRepository.save(query);
        logger.info("Created new query with ID: {} for user: {} (valid: {})", 
                   savedQuery.getId(), user.getEmail(), savedQuery.getIsValid());
        
        return savedQuery;
    }
    
    private void mapValidationResponseToQuery(Query query, ChatGptValidationResponse response) {
        if (response == null) {
            query.setIsValid(false);
            return;
        }
        
        // Map validity fields
        if (response.getValidity() != null) {
            ChatGptValidationResponse.Validity validity = response.getValidity();
            
            query.setIsValid(Boolean.TRUE.equals(validity.getValidPrompt()));
            query.setOutOfBoundsPromptLength(validity.getOutOfBoundsPromptLength());
            query.setOffensiveLanguageDetected(validity.getOffensiveLanguageDetected());
            query.setNastyInstructionDetected(validity.getNastyInstructionDetected());
            query.setPurposeValid(validity.getPurposeValid());
            query.setReasonableUsage(validity.getReasonableUsage());
            query.setSelfEnforcing(validity.getSelfEnforcing());
            query.setInvalidReason(validity.getInvalidReason());
        }
        
        // Map when_notify fields
        if (response.getWhenNotify() != null) {
            ChatGptValidationResponse.WhenNotify whenNotify = response.getWhenNotify();
            
            // Map type flags
            if (whenNotify.getType() != null) {
                query.setCron(Boolean.TRUE.equals(whenNotify.getType().getCron()));
                query.setDateSpecific(Boolean.TRUE.equals(whenNotify.getType().getSpecific()));
                query.setToCheck(Boolean.TRUE.equals(whenNotify.getType().getCheck()));
            }
            
            // Map cron expression
            if (whenNotify.getCronExpression() != null && !whenNotify.getCronExpression().trim().isEmpty()) {
                query.setCronParams(whenNotify.getCronExpression().trim());
                query.setNextExecution(calculateNextCronExecution(whenNotify.getCronExpression()));
            }
            
            // Map specific datetime
            if (whenNotify.getDateTime() != null && !whenNotify.getDateTime().trim().isEmpty()) {
                try {
                    LocalDateTime specificDateTime = LocalDateTime.parse(
                        whenNotify.getDateTime().trim(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );
                    query.setSpecificDatetime(specificDateTime);
                    query.setNextExecution(specificDateTime);
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
                    query.setValidFrom(startDate);
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
                    query.setValidTo(endDate);
                } catch (DateTimeParseException e) {
                    logger.warn("Failed to parse end date: {}", whenNotify.getEndDate(), e);
                }
            }
        }
        
        // Map summary fields
        if (response.getSummary() != null) {
            ChatGptValidationResponse.Summary summary = response.getSummary();
            query.setSummaryText(summary.getText());
            query.setLanguage(summary.getLanguage());
            query.setCategory(summary.getCategory());
        }
        
        // Map metadata fields
        if (response.getMetadata() != null) {
            ChatGptValidationResponse.Metadata metadata = response.getMetadata();
            query.setModelVersion(metadata.getModelVersion());
            query.setConfidenceScore(metadata.getConfidenceScore());
            query.setPolicyEnforced(metadata.getPolicyEnforced());
            
            // Convert tags array to JSON string
            if (metadata.getTags() != null && metadata.getTags().length > 0) {
                try {
                    query.setTags(objectMapper.writeValueAsString(metadata.getTags()));
                } catch (Exception e) {
                    logger.warn("Failed to serialize tags: {}", e.getMessage());
                }
            }
        }
        
        logger.info("Mapped ChatGPT validation response to query: cron={}, specific={}, check={}, valid={}", 
                   query.getCron(), query.getDateSpecific(), query.getToCheck(), query.getIsValid());
    }
    
    public Query createFallbackQuery(User user, String prompt) {
        Query query = new Query(user, prompt);
        query.setIsValid(false);
        query.setInvalidReason("Failed to parse ChatGPT validation response");
        
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
            return LocalDateTime.now().plusHours(1);
        } catch (Exception e) {
            logger.warn("Failed to calculate next cron execution for: {}", cronExpression, e);
            return LocalDateTime.now().plusHours(1); // Fallback
        }
    }
}