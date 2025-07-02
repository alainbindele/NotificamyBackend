package com.notifyme.service;

import com.notifyme.dto.ChatGptValidationResponse;
import com.notifyme.entity.TQuery;
import com.notifyme.entity.TUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class ValidationLogicService {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationLogicService.class);
    
    @Autowired
    private CronExpressionService cronExpressionService;
    
    /**
     * Applica la logica di validazione usando SOLO i dati di ChatGPT
     */
    public TQuery applyValidationLogic(TUser user, String prompt, ChatGptValidationResponse validationResponse) {
        logger.info("Applying validation logic for user: {} with ChatGPT response", user.getEmail());
        
        TQuery query = new TQuery(user, prompt);
        
        // 1. Mappa i dati base dalla risposta ChatGPT
        mapBasicValidationData(query, validationResponse);
        
        // 2. Se non è valido, ritorna subito
        if (!Boolean.TRUE.equals(query.getIsValid())) {
            logger.info("Query marked as invalid by ChatGPT: {}", query.getInvalidReason());
            return query;
        }
        
        // 3. Estrai e applica i dati temporali dalla risposta ChatGPT
        applyTemporalDataFromChatGPT(query, validationResponse);
        
        // 4. Applica la logica dei casi 0-5 basata sui flag ChatGPT
        applyCaseLogicFromChatGPT(query, validationResponse);
        
        // 5. Valida la configurazione finale
        validateFinalConfiguration(query);
        
        logger.info("Validation logic applied - Query valid: {}, cron: {}, specific: {}, check: {}, next_execution: {}", 
                   query.getIsValid(), query.getCron(), query.getDateSpecific(), query.getToCheck(), query.getNextExecution());
        
        return query;
    }
    
    /**
     * Mappa i dati base dalla risposta ChatGPT
     */
    private void mapBasicValidationData(TQuery query, ChatGptValidationResponse response) {
        if (response == null) {
            query.setIsValid(false);
            query.setInvalidReason("Nessuna risposta da ChatGPT");
            return;
        }
        
        // Mappa validity
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
        
        // Mappa summary
        if (response.getSummary() != null) {
            query.setSummaryText(response.getSummary().getText());
            query.setLanguage(response.getSummary().getLanguage());
            query.setCategory(response.getSummary().getCategory());
        }
        
        // Mappa metadata
        if (response.getMetadata() != null) {
            query.setModelVersion(response.getMetadata().getModelVersion());
            query.setConfidenceScore(response.getMetadata().getConfidenceScore());
            query.setPolicyEnforced(response.getMetadata().getPolicyEnforced());
            
            // Mappa tags
            if (response.getMetadata().getTags() != null) {
                try {
                    String tagsJson = String.join(",", response.getMetadata().getTags());
                    query.setTags("[\"" + String.join("\",\"", response.getMetadata().getTags()) + "\"]");
                } catch (Exception e) {
                    logger.warn("Failed to serialize tags: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Estrae e applica i dati temporali dalla risposta ChatGPT
     */
    private void applyTemporalDataFromChatGPT(TQuery query, ChatGptValidationResponse response) {
        if (response.getWhenNotify() == null) {
            logger.warn("No when_notify data in ChatGPT response");
            return;
        }
        
        ChatGptValidationResponse.WhenNotify whenNotify = response.getWhenNotify();
        
        // Estrai i flag di tipo
        if (whenNotify.getTimeType() != null) {
            query.setCron(Boolean.TRUE.equals(whenNotify.getTimeType().getCron()));
            query.setDateSpecific(Boolean.TRUE.equals(whenNotify.getTimeType().getSpecific()));
            query.setToCheck(Boolean.TRUE.equals(whenNotify.getTimeType().getCheck()));
        }
        
        // Estrai cron expression
        if (whenNotify.getCronExpression() != null && !whenNotify.getCronExpression().trim().isEmpty()) {
            query.setCronParams(whenNotify.getCronExpression().trim());
        }
        
        // Estrai date/time specifiche
        if (whenNotify.getDateTime() != null && !whenNotify.getDateTime().trim().isEmpty()) {
            LocalDateTime specificDateTime = parseDateTime(whenNotify.getDateTime());
            if (specificDateTime != null) {
                query.setNextExecution(specificDateTime);
            }
        }
        
        // Estrai periodo di validità
        if (whenNotify.getStartDate() != null && !whenNotify.getStartDate().trim().isEmpty()) {
            LocalDateTime validFrom = parseDateTime(whenNotify.getStartDate());
            if (validFrom != null) {
                query.setValidFrom(validFrom);
            }
        }
        
        if (whenNotify.getEndDate() != null && !whenNotify.getEndDate().trim().isEmpty()) {
            LocalDateTime validTo = parseDateTime(whenNotify.getEndDate());
            if (validTo != null) {
                query.setValidTo(validTo);
            }
        }
        
        logger.debug("Applied temporal data from ChatGPT - cron: {}, specific: {}, check: {}, next_execution: {}", 
                    query.getCron(), query.getDateSpecific(), query.getToCheck(), query.getNextExecution());
    }
    
    /**
     * Applica la logica dei casi 0-5 basata sui flag ChatGPT
     */
    private void applyCaseLogicFromChatGPT(TQuery query, ChatGptValidationResponse response) {
        boolean cron = Boolean.TRUE.equals(query.getCron());
        boolean specific = Boolean.TRUE.equals(query.getDateSpecific());
        boolean check = Boolean.TRUE.equals(query.getToCheck());
        
        logger.debug("Applying case logic - cron: {}, specific: {}, check: {}", cron, specific, check);
        
        // Calcola next_execution se non già impostato da ChatGPT
        if (query.getNextExecution() == null) {
            if (cron && query.getCronParams() != null) {
                // Per query ricorrenti, calcola la prossima esecuzione dal cron
                LocalDateTime nextExecution = cronExpressionService.getNextExecution(query.getCronParams());
                query.setNextExecution(nextExecution);
                logger.debug("Calculated next_execution from cron: {}", nextExecution);
            } else if (!cron && !specific) {
                // Caso di fallback: imposta un'esecuzione di default
                LocalDateTime defaultExecution = LocalDateTime.now().plusHours(1);
                query.setNextExecution(defaultExecution);
                logger.warn("No temporal configuration found, using default next_execution: {}", defaultExecution);
            }
        }
        
        // Valida la configurazione dei casi
        validateCaseConfiguration(query, cron, specific, check);
    }
    
    /**
     * Valida la configurazione dei casi 0-5
     */
    private void validateCaseConfiguration(TQuery query, boolean cron, boolean specific, boolean check) {
        // Caso 0: cron=1, specific=0, check=1
        if (cron && !specific && check) {
            logger.debug("Validated as Case 0: recurring condition check");
            return;
        }
        
        // Caso 1: cron=0, specific=1, check=0
        if (!cron && specific && !check) {
            logger.debug("Validated as Case 1: specific time notification");
            return;
        }
        
        // Caso 2: cron=1, specific=0, check=0
        if (cron && !specific && !check) {
            logger.debug("Validated as Case 2: recurring notification");
            return;
        }
        
        // Caso 3: cron=0, specific=1, check=1
        if (!cron && specific && check) {
            logger.debug("Validated as Case 3: specific time condition check");
            return;
        }
        
        // Caso 4: cron=1, specific=0, check=1
        if (cron && !specific && check) {
            logger.debug("Validated as Case 4: recurring condition check");
            return;
        }
        
        // Caso 5: cron=1, specific=1, check=1
        if (cron && specific && check) {
            logger.debug("Validated as Case 5: specific time condition check with custom interval");
            return;
        }
        
        // Configurazione non valida
        query.setIsValid(false);
        query.setInvalidReason(String.format(
            "Configurazione temporale non valida: cron=%s, specific=%s, check=%s", 
            cron, specific, check
        ));
        logger.warn("Invalid case configuration: {}", query.getInvalidReason());
    }
    
    /**
     * Valida la configurazione finale
     */
    private void validateFinalConfiguration(TQuery query) {
        // Verifica che ci sia almeno una configurazione temporale
        if (query.getNextExecution() == null && 
            (query.getCronParams() == null || query.getCronParams().trim().isEmpty())) {
            query.setIsValid(false);
            query.setInvalidReason("Manca la configurazione temporale (next_execution o cron_params)");
            logger.warn("Final validation failed: no temporal configuration");
            return;
        }
        
        // Verifica che next_execution non sia nel passato per query specifiche
        if (Boolean.TRUE.equals(query.getDateSpecific()) && query.getNextExecution() != null) {
            if (query.getNextExecution().isBefore(LocalDateTime.now())) {
                query.setIsValid(false);
                query.setInvalidReason("La data/ora specificata è nel passato");
                logger.warn("Final validation failed: next_execution is in the past");
                return;
            }
        }
        
        // Verifica periodo di validità
        if (query.getValidFrom() != null && query.getValidTo() != null) {
            if (query.getValidFrom().isAfter(query.getValidTo())) {
                query.setIsValid(false);
                query.setInvalidReason("La data di inizio validità è dopo la data di fine");
                logger.warn("Final validation failed: invalid validity period");
                return;
            }
            
            if (query.getValidFrom().plusHours(1).isAfter(query.getValidTo())) {
                query.setIsValid(false);
                query.setInvalidReason("Il periodo di validità deve essere di almeno 1 ora");
                logger.warn("Final validation failed: validity period too short");
                return;
            }
        }
        
        logger.debug("Final validation passed for query");
    }
    
    /**
     * Parsa una stringa datetime in LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Prova vari formati
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    LocalDateTime result = LocalDateTime.parse(dateTimeStr.trim(), formatter);
                    logger.debug("Parsed datetime '{}' as {}", dateTimeStr, result);
                    return result;
                } catch (DateTimeParseException ignored) {
                    // Prova il prossimo formato
                }
            }
            
            logger.warn("Failed to parse datetime: {}", dateTimeStr);
            return null;
            
        } catch (Exception e) {
            logger.error("Error parsing datetime '{}': {}", dateTimeStr, e.getMessage());
            return null;
        }
    }
}