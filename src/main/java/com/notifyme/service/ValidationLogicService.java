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
    
    @Autowired
    private TemporalReferenceService temporalReferenceService;
    
    /**
     * Applica la logica di validazione e calcolo secondo l'algoritmo specificato
     */
    public TQuery applyValidationLogic(TUser user, String prompt, ChatGptValidationResponse validationResponse) {
        logger.info("Applying validation logic for user: {} with prompt: {}", user.getEmail(), prompt);
        
        TQuery query = new TQuery(user, prompt);
        
        // 1. Mappa i dati base dalla risposta ChatGPT
        mapBasicValidationData(query, validationResponse);
        
        // 2. Se non è valido, ritorna subito
        if (!Boolean.TRUE.equals(query.getIsValid())) {
            logger.info("Query marked as invalid by ChatGPT, skipping further processing");
            return query;
        }
        
        // 3. Estrai riferimenti temporali dal prompt usando il servizio dedicato
        TemporalReference temporalRef = temporalReferenceService.extractTemporalReference(prompt);
        
        // 4. Valida presenza di [qualcosa] o [evento]
        if (!hasContentOrEvent(prompt)) {
            logger.warn("Query rejected: no content or event specified");
            query.setIsValid(false);
            query.setInvalidReason("Manca la specifica di cosa notificare o quale evento controllare");
            return query;
        }
        
        // 5. Applica valutazione preliminare dell'intervallo di validità
        if (!applyPreliminaryValidation(query, temporalRef)) {
            return query; // Query chiusa o invalidata
        }
        
        // 6. Classifica e applica la logica dei casi 0-5
        applyCaseLogic(query, prompt, temporalRef, validationResponse);
        
        logger.info("Validation logic applied - Query valid: {}, cron: {}, specific: {}, check: {}", 
                   query.getIsValid(), query.getCron(), query.getDateSpecific(), query.getToCheck());
        
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
        }
    }
    
    /**
     * Verifica se il prompt contiene [qualcosa] o [evento]
     */
    private boolean hasContentOrEvent(String prompt) {
        // Il prompt deve contenere almeno una di queste cose:
        // 1. Un'azione da ricordare (buttare la pasta, chiamare, etc.)
        // 2. Un evento da controllare (se bitcoin scende, se arriva email, etc.)
        // 3. Contenuto informativo (notizie, aggiornamenti, etc.)
        
        String lowerPrompt = prompt.toLowerCase();
        
        // Azioni/contenuti
        if (lowerPrompt.contains("buttare") || lowerPrompt.contains("chiamare") || 
            lowerPrompt.contains("ricorda") || lowerPrompt.contains("remind") ||
            lowerPrompt.contains("notizie") || lowerPrompt.contains("news") ||
            lowerPrompt.contains("aggiornament") || lowerPrompt.contains("update") ||
            lowerPrompt.contains("promemoria") || lowerPrompt.contains("notifica")) {
            return true;
        }
        
        // Eventi condizionali
        if (lowerPrompt.contains("se ") || lowerPrompt.contains("if ") ||
            lowerPrompt.contains("quando ") || lowerPrompt.contains("when ") ||
            lowerPrompt.contains("scende") || lowerPrompt.contains("sale") ||
            lowerPrompt.contains("cambia") || lowerPrompt.contains("raggiunge")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Applica la valutazione preliminare dell'intervallo di validità
     */
    private boolean applyPreliminaryValidation(TQuery query, TemporalReference temporalRef) {
        LocalDateTime now = LocalDateTime.now();
        
        LocalDateTime validFrom = temporalRef.getValidFrom();
        LocalDateTime validTo = temporalRef.getValidTo();
        
        // Caso: solo valid_from
        if (validFrom != null && validTo == null) {
            if (now.isBefore(validFrom)) {
                query.setValidFrom(validFrom);
                query.setNextExecution(validFrom);
                logger.debug("Set next_execution to valid_from: {}", validFrom);
            }
        }
        
        // Caso: solo valid_to
        else if (validFrom == null && validTo != null) {
            if (now.isAfter(validTo)) {
                query.setClosed(true);
                query.setValidTo(validTo);
                logger.info("Query closed: current time is after valid_to ({})", validTo);
                return false;
            }
            query.setValidTo(validTo);
        }
        
        // Caso: entrambi presenti
        else if (validFrom != null && validTo != null) {
            // 1. Controlla che l'intervallo sia almeno di 1 ora
            if (validFrom.plusHours(1).isAfter(validTo)) {
                query.setIsValid(false);
                query.setInvalidReason("L'intervallo di validità deve essere di almeno 1 ora");
                logger.warn("Invalid time interval: less than 1 hour between valid_from and valid_to");
                return false;
            }
            
            query.setValidFrom(validFrom);
            query.setValidTo(validTo);
            
            // 2. Se ora è prima di valid_from
            if (now.isBefore(validFrom)) {
                query.setNextExecution(validFrom);
                logger.debug("Set next_execution to valid_from: {}", validFrom);
            }
            
            // 3. Se ora è dopo valid_to
            else if (now.isAfter(validTo)) {
                query.setClosed(true);
                logger.info("Query closed: current time is after valid_to ({})", validTo);
                return false;
            }
        }
        
        return true; // Continua con la validazione
    }
    
    /**
     * Applica la logica dei casi 0-5 usando i flag di ChatGPT + parsing temporale
     */
    private void applyCaseLogic(TQuery query, String prompt, TemporalReference temporalRef, ChatGptValidationResponse validationResponse) {
        // Estrai i flag da ChatGPT
        boolean chatGptCron = false;
        boolean chatGptSpecific = false;
        boolean chatGptCheck = false;
        
        if (validationResponse.getWhenNotify() != null && validationResponse.getWhenNotify().getTimeType() != null) {
            chatGptCron = Boolean.TRUE.equals(validationResponse.getWhenNotify().getTimeType().getCron());
            chatGptSpecific = Boolean.TRUE.equals(validationResponse.getWhenNotify().getTimeType().getSpecific());
            chatGptCheck = Boolean.TRUE.equals(validationResponse.getWhenNotify().getTimeType().getCheck());
        }
        
        // Combina con il parsing temporale locale per maggiore robustezza
        boolean hasEvent = hasEventCondition(prompt);
        boolean hasSpecificTime = temporalRef.hasSpecificDateTime();
        boolean hasRecurringTime = temporalRef.hasRecurringPattern();
        boolean hasCheckInterval = temporalRef.hasCheckInterval();
        
        logger.debug("ChatGPT flags - cron: {}, specific: {}, check: {}", chatGptCron, chatGptSpecific, chatGptCheck);
        logger.debug("Local parsing - hasEvent: {}, hasSpecificTime: {}, hasRecurringTime: {}, hasCheckInterval: {}", 
                    hasEvent, hasSpecificTime, hasRecurringTime, hasCheckInterval);
        
        // Usa i flag di ChatGPT come base, ma integra con il parsing locale
        boolean finalCron = chatGptCron || hasRecurringTime || hasCheckInterval;
        boolean finalSpecific = chatGptSpecific || hasSpecificTime;
        boolean finalCheck = chatGptCheck || hasEvent;
        
        // Applica i flag finali
        query.setCron(finalCron);
        query.setDateSpecific(finalSpecific);
        query.setToCheck(finalCheck);
        
        // Calcola next_execution e cron_params basandosi sui dati temporali estratti
        calculateTemporalConfiguration(query, temporalRef, finalCron, finalSpecific, finalCheck);
        
        // Valida la configurazione finale
        validateFinalConfiguration(query, finalCron, finalSpecific, finalCheck);
    }
    
    /**
     * Calcola la configurazione temporale (next_execution e cron_params)
     */
    private void calculateTemporalConfiguration(TQuery query, TemporalReference temporalRef, 
                                              boolean cron, boolean specific, boolean check) {
        
        // 1. Se abbiamo una data/ora specifica, usala per next_execution
        if (specific && temporalRef.hasSpecificDateTime()) {
            query.setNextExecution(temporalRef.getSpecificDateTime());
            logger.debug("Set next_execution from specific datetime: {}", temporalRef.getSpecificDateTime());
        }
        
        // 2. Se abbiamo un pattern ricorrente, imposta cron_params
        if (cron && temporalRef.hasRecurringPattern()) {
            query.setCronParams(temporalRef.getCronExpression());
            
            // Se non abbiamo già next_execution, calcolalo dal cron
            if (query.getNextExecution() == null) {
                LocalDateTime nextExecution = cronExpressionService.getNextExecution(temporalRef.getCronExpression());
                query.setNextExecution(nextExecution);
                logger.debug("Calculated next_execution from cron: {}", nextExecution);
            }
        }
        
        // 3. Se abbiamo un intervallo di controllo personalizzato, usalo
        if (check && temporalRef.hasCheckInterval()) {
            query.setCronParams(temporalRef.getCheckIntervalCron());
            
            if (query.getNextExecution() == null) {
                LocalDateTime nextExecution = cronExpressionService.getNextExecution(temporalRef.getCheckIntervalCron());
                query.setNextExecution(nextExecution);
                logger.debug("Calculated next_execution from check interval: {}", nextExecution);
            }
        }
        
        // 4. Fallback per casi senza configurazione temporale esplicita
        if (query.getNextExecution() == null && query.getCronParams() == null) {
            if (check) {
                // Per controlli senza orario specifico, usa default giornaliero alle 10:00
                query.setCronParams("0 10 * * *");
                query.setNextExecution(cronExpressionService.getNextExecution("0 10 * * *"));
                logger.debug("Applied default check schedule: daily at 10:00");
            } else {
                // Per altri casi, usa un'esecuzione immediata
                query.setNextExecution(LocalDateTime.now().plusMinutes(1));
                logger.debug("Applied immediate execution fallback");
            }
        }
    }
    
    /**
     * Valida la configurazione finale
     */
    private void validateFinalConfiguration(TQuery query, boolean cron, boolean specific, boolean check) {
        // Verifica configurazioni non valide secondo la tabella dei casi
        if (!cron && !specific && !check) {
            query.setIsValid(false);
            query.setInvalidReason("Configurazione temporale non riconosciuta o non supportata");
            logger.warn("Invalid configuration: no temporal flags set");
            return;
        }
        
        // Verifica che ci sia almeno una configurazione temporale
        if (query.getNextExecution() == null && 
            (query.getCronParams() == null || query.getCronParams().trim().isEmpty())) {
            query.setIsValid(false);
            query.setInvalidReason("Manca la configurazione temporale (next_execution o cron_params)");
            logger.warn("Final validation failed: no temporal configuration");
            return;
        }
        
        // Verifica che next_execution non sia nel passato per query specifiche
        if (specific && query.getNextExecution() != null) {
            if (query.getNextExecution().isBefore(LocalDateTime.now())) {
                query.setIsValid(false);
                query.setInvalidReason("La data/ora specificata è nel passato");
                logger.warn("Final validation failed: next_execution is in the past");
                return;
            }
        }
        
        logger.debug("Final validation passed - cron: {}, specific: {}, check: {}, next_execution: {}, cron_params: {}", 
                    cron, specific, check, query.getNextExecution(), query.getCronParams());
    }
    
    /**
     * Verifica se il prompt contiene una condizione di evento
     */
    private boolean hasEventCondition(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        return lowerPrompt.contains("se ") || lowerPrompt.contains("if ") ||
               lowerPrompt.contains("quando ") || lowerPrompt.contains("when ") ||
               lowerPrompt.contains("scende") || lowerPrompt.contains("sale") ||
               lowerPrompt.contains("cambia") || lowerPrompt.contains("raggiunge") ||
               lowerPrompt.contains("supera") || lowerPrompt.contains("controllando");
    }
}