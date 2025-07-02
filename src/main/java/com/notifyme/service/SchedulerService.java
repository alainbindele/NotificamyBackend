package com.notifyme.service;

import com.notifyme.entity.TQuery;
import com.notifyme.entity.TExecution;
import com.notifyme.repository.QueryRepository;
import com.notifyme.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SchedulerService {
    
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    
    @Autowired
    private QueryRepository queryRepository;
    
    @Autowired
    private ExecutionRepository executionRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private CheckService checkService;
    
    @Autowired
    private CronExpressionService cronExpressionService;
    
    /**
     * Esegue ogni minuto per controllare le query pronte per l'esecuzione
     */
    @Scheduled(fixedRate = 60000) // Ogni 60 secondi
    public void executeScheduledQueries() {
        logger.debug("Checking for queries ready for execution...");
        
        try {
            List<TQuery> readyQueries = queryRepository.findQueriesReadyForExecutionWithValidityPeriod(LocalDateTime.now());
            
            if (!readyQueries.isEmpty()) {
                logger.info("Found {} queries ready for execution", readyQueries.size());
                
                for (TQuery query : readyQueries) {
                    try {
                        executeQuery(query);
                    } catch (Exception e) {
                        logger.error("Failed to execute query {}: {}", query.getId(), e.getMessage(), e);
                        recordFailedExecution(query, e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in scheduled query execution: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Esegue una singola query
     */
    private void executeQuery(TQuery query) {
        logger.info("Executing query {} for user {}: {}", 
                   query.getId(), query.getUser().getEmail(), query.getPrompt());
        
        try {
            String notificationContent = null;
            boolean shouldNotify = false;
            
            // Determina il tipo di esecuzione basato sui flag
            if (Boolean.TRUE.equals(query.getToCheck())) {
                // Caso CHECK: verifica condizione esterna
                logger.debug("Executing CHECK query: {}", query.getId());
                shouldNotify = checkService.evaluateCondition(query);
                
                if (shouldNotify) {
                    notificationContent = checkService.getConditionResult(query);
                } else {
                    logger.debug("Condition not met for query {}, skipping notification", query.getId());
                }
                
            } else {
                // Caso normale: notifica diretta
                logger.debug("Executing direct notification query: {}", query.getId());
                shouldNotify = true;
                notificationContent = generateNotificationContent(query);
            }
            
            // Invia notifica se necessario
            if (shouldNotify && notificationContent != null) {
                boolean notificationSent = notificationService.sendNotification(
                    query.getUser(), 
                    query, 
                    notificationContent
                );
                
                if (notificationSent) {
                    recordSuccessfulExecution(query, notificationContent);
                    logger.info("Successfully executed and sent notification for query {}", query.getId());
                } else {
                    recordFailedExecution(query, "Failed to send notification");
                    logger.warn("Query {} executed but notification failed to send", query.getId());
                }
            } else {
                recordSuccessfulExecution(query, "Condition checked - no notification needed");
                logger.debug("Query {} executed successfully - no notification sent", query.getId());
            }
            
            // Aggiorna next_execution per la prossima esecuzione
            updateNextExecution(query);
            
        } catch (Exception e) {
            logger.error("Error executing query {}: {}", query.getId(), e.getMessage(), e);
            recordFailedExecution(query, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Genera il contenuto della notifica
     */
    private String generateNotificationContent(TQuery query) {
        if (query.getSummaryText() != null && !query.getSummaryText().trim().isEmpty()) {
            return query.getSummaryText();
        }
        
        // Fallback al prompt originale
        return "Promemoria: " + query.getPrompt();
    }
    
    /**
     * Aggiorna la prossima esecuzione della query
     */
    private void updateNextExecution(TQuery query) {
        try {
            LocalDateTime nextExecution = null;
            
            if (Boolean.TRUE.equals(query.getCron())) {
                // Query ricorrente: calcola prossima esecuzione da cron
                if (query.getCronParams() != null && !query.getCronParams().trim().isEmpty()) {
                    nextExecution = cronExpressionService.getNextExecution(query.getCronParams());
                    logger.debug("Next cron execution for query {}: {}", query.getId(), nextExecution);
                }
                
            } else if (Boolean.TRUE.equals(query.getDateSpecific())) {
                // Query specifica: chiudi dopo l'esecuzione
                logger.debug("Closing specific date query {}", query.getId());
                query.setClosed(true);
                nextExecution = null;
                
            } else {
                // Caso non previsto: chiudi la query
                logger.warn("Query {} has no valid execution type, closing", query.getId());
                query.setClosed(true);
                nextExecution = null;
            }
            
            // Verifica validità temporale per query ricorrenti
            if (nextExecution != null && query.getValidTo() != null) {
                if (nextExecution.isAfter(query.getValidTo())) {
                    logger.info("Next execution {} is after valid_to {}, closing query {}", 
                               nextExecution, query.getValidTo(), query.getId());
                    query.setClosed(true);
                    nextExecution = null;
                }
            }
            
            query.setNextExecution(nextExecution);
            queryRepository.save(query);
            
            logger.debug("Updated query {} - next_execution: {}, closed: {}", 
                        query.getId(), nextExecution, query.getClosed());
            
        } catch (Exception e) {
            logger.error("Failed to update next execution for query {}: {}", query.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Registra un'esecuzione riuscita
     */
    private void recordSuccessfulExecution(TQuery query, String response) {
        try {
            TExecution execution = new TExecution(query, TExecution.ExecutionStatus.SUCCESS, response);
            executionRepository.save(execution);
            logger.debug("Recorded successful execution for query {}", query.getId());
        } catch (Exception e) {
            logger.error("Failed to record successful execution for query {}: {}", query.getId(), e.getMessage());
        }
    }
    
    /**
     * Registra un'esecuzione fallita
     */
    private void recordFailedExecution(TQuery query, String errorMessage) {
        try {
            TExecution execution = new TExecution(query, TExecution.ExecutionStatus.FAILED, errorMessage);
            executionRepository.save(execution);
            logger.debug("Recorded failed execution for query {}", query.getId());
        } catch (Exception e) {
            logger.error("Failed to record failed execution for query {}: {}", query.getId(), e.getMessage());
        }
    }
    
    /**
     * Chiude query scadute (oltre valid_to)
     */
    @Scheduled(fixedRate = 3600000) // Ogni ora
    public void closeExpiredQueries() {
        logger.debug("Checking for expired queries to close...");
        
        try {
            // Trova query con valid_to nel passato che sono ancora aperte
            List<TQuery> expiredQueries = queryRepository.findByIsValidAndNextExecutionBefore(true, LocalDateTime.now());
            
            int closedCount = 0;
            for (TQuery query : expiredQueries) {
                if (query.getValidTo() != null && query.getValidTo().isBefore(LocalDateTime.now()) && 
                    !Boolean.TRUE.equals(query.getClosed())) {
                    
                    query.setClosed(true);
                    query.setNextExecution(null);
                    queryRepository.save(query);
                    closedCount++;
                    
                    logger.info("Closed expired query {} (valid_to: {})", query.getId(), query.getValidTo());
                }
            }
            
            if (closedCount > 0) {
                logger.info("Closed {} expired queries", closedCount);
            }
            
        } catch (Exception e) {
            logger.error("Error closing expired queries: {}", e.getMessage(), e);
        }
    }
}