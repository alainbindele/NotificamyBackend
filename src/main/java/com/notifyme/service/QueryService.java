package com.notifyme.service;

import com.notifyme.dto.ChatGptValidationResponse;
import com.notifyme.entity.TQuery;
import com.notifyme.entity.TUser;
import com.notifyme.repository.QueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class QueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);
    
    @Autowired
    private QueryRepository queryRepository;
    
    @Autowired
    private ValidationLogicService validationLogicService;
    
    /**
     * Crea una query applicando la logica di validazione completa
     */
    public TQuery createQuery(TUser user, String prompt, ChatGptValidationResponse validationResponse, String userTimezone) {
        logger.info("Creating query for user: {} with validation logic", user.getEmail());
        
        // Applica la logica di validazione e calcolo
        TQuery query = validationLogicService.applyValidationLogic(user, prompt, validationResponse, userTimezone);
        
        // Salva nel database
        TQuery savedQuery = queryRepository.save(query);
        
        logger.info("Created query with ID: {} for user: {} - valid: {}, cron: {}, specific: {}, check: {}, next_execution: {}", 
                   savedQuery.getId(), user.getEmail(), savedQuery.getIsValid(), 
                   savedQuery.getCron(), savedQuery.getDateSpecific(), savedQuery.getToCheck(),
                   savedQuery.getNextExecution());
        
        return savedQuery;
    }
    
    /**
     * Salva una query esistente
     */
    public TQuery saveQuery(TQuery query) {
        return queryRepository.save(query);
    }
    
    /**
     * Crea una query di fallback quando il parsing della risposta ChatGPT fallisce
     */
    public TQuery createFallbackQuery(TUser user, String prompt, String userTimezone) {
        TQuery query = new TQuery(user, prompt);
        query.setTimezone(userTimezone);
        query.setIsValid(false);
        query.setInvalidReason("Failed to parse ChatGPT validation response");
        
        TQuery savedQuery = queryRepository.save(query);
        logger.info("Created fallback query with ID: {} for user: {} (invalid due to parsing error)", 
                   savedQuery.getId(), user.getEmail());
        
        return savedQuery;
    }
    
    /**
     * Trova query per utente ordinate per data di creazione
     */
    public List<TQuery> findByUser(TUser user) {
        return queryRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    /**
     * Trova solo query valide per utente
     */
    public List<TQuery> findValidQueriesByUser(TUser user) {
        return queryRepository.findByUserAndIsValid(user, true);
    }
    
    /**
     * Trova query attive (valide e non chiuse) per utente
     */
    public List<TQuery> findActiveQueriesByUser(TUser user) {
        return queryRepository.findActiveQueriesByUser(user);
    }
        
    /**
     * Trova query per tipo
     */
    public List<TQuery> findCronQueriesByUser(TUser user) {
        return queryRepository.findByUserAndCronTrue(user);
    }
    
    public List<TQuery> findSpecificQueriesByUser(TUser user) {
        return queryRepository.findByUserAndDateSpecificTrue(user);
    }
    
    public List<TQuery> findCheckQueriesByUser(TUser user) {
        return queryRepository.findByUserAndToCheckTrue(user);
    }
    
    /**
     * Statistiche per utente
     */
    public QueryStatistics getQueryStatistics(TUser user) {
        QueryStatistics stats = new QueryStatistics();
        stats.setTotalQueries(queryRepository.countValidQueriesByUser(user));
        stats.setCronQueries(queryRepository.countCronQueriesByUser(user));
        stats.setSpecificQueries(queryRepository.countSpecificQueriesByUser(user));
        stats.setCheckQueries(queryRepository.countCheckQueriesByUser(user));
        
        return stats;
    }
    
    /**
     * Chiude una query manualmente
     */
    public boolean closeQuery(Long queryId, TUser user) {
        try {
            TQuery query = queryRepository.findById(queryId).orElse(null);
            
            if (query == null) {
                logger.warn("Query not found: {}", queryId);
                return false;
            }
            
            if (!query.getUser().getId().equals(user.getId())) {
                logger.warn("User {} attempted to close query {} owned by {}", 
                           user.getEmail(), queryId, query.getUser().getEmail());
                return false;
            }
            
            query.setClosed(true);
            query.setNextExecution(null);
            queryRepository.save(query);
            
            logger.info("Query {} closed by user {}", queryId, user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Error closing query {}: {}", queryId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Classe per le statistiche delle query
     */
    public static class QueryStatistics {
        private Long totalQueries = 0L;
        private Long cronQueries = 0L;
        private Long specificQueries = 0L;
        private Long checkQueries = 0L;
        
        // Getters e setters
        public Long getTotalQueries() { return totalQueries; }
        public void setTotalQueries(Long totalQueries) { this.totalQueries = totalQueries; }
        
        public Long getCronQueries() { return cronQueries; }
        public void setCronQueries(Long cronQueries) { this.cronQueries = cronQueries; }
        
        public Long getSpecificQueries() { return specificQueries; }
        public void setSpecificQueries(Long specificQueries) { this.specificQueries = specificQueries; }
        
        public Long getCheckQueries() { return checkQueries; }
        public void setCheckQueries(Long checkQueries) { this.checkQueries = checkQueries; }
    }
}