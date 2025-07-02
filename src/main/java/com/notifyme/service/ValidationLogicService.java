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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        
        // 3. Estrai riferimenti temporali dal prompt
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
        applyCaseLogic(query, prompt, temporalRef);
        
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
        // Pattern per rilevare eventi condizionali
        Pattern eventPattern = Pattern.compile("(?i)\\bse\\b|\\bif\\b|\\bwhen\\b|\\bquando\\b|\\bscende\\b|\\bsale\\b|\\bcambia\\b|\\braggiunge\\b");
        
        // Pattern per rilevare contenuti informativi
        Pattern contentPattern = Pattern.compile("(?i)\\bnotizie\\b|\\bnews\\b|\\baggiornament\\b|\\bupdate\\b|\\binfo\\b|\\bpromemoria\\b|\\bricorda\\b|\\bremind\\b");
        
        return eventPattern.matcher(prompt).find() || contentPattern.matcher(prompt).find();
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
     * Applica la logica dei casi 0-5
     */
    private void applyCaseLogic(TQuery query, String prompt, TemporalReference temporalRef) {
        boolean hasEvent = hasEventCondition(prompt);
        boolean hasSpecificTime = temporalRef.hasSpecificDateTime();
        boolean hasRecurringTime = temporalRef.hasRecurringPattern();
        boolean hasCheckInterval = temporalRef.hasCheckInterval();
        
        logger.debug("Prompt analysis - hasEvent: {}, hasSpecificTime: {}, hasRecurringTime: {}, hasCheckInterval: {}", 
                    hasEvent, hasSpecificTime, hasRecurringTime, hasCheckInterval);
        
        if (hasEvent && !hasSpecificTime && !hasRecurringTime) {
            // Caso 0: notificami [se_evento]
            applyCaso0(query, temporalRef);
            
        } else if (!hasEvent && hasSpecificTime && !hasRecurringTime) {
            // Caso 1: notificami il [riferimento_temporale] [qualcosa]
            applyCaso1(query, temporalRef);
            
        } else if (!hasEvent && !hasSpecificTime && hasRecurringTime) {
            // Caso 2: notificami ogni [riferimento_temporale_intervallo] [qualcosa]
            applyCaso2(query, temporalRef);
            
        } else if (hasEvent && hasSpecificTime && !hasRecurringTime && !hasCheckInterval) {
            // Caso 3: notificami il [riferimento_temporale] [se_evento]
            applyCaso3(query, temporalRef);
            
        } else if (hasEvent && !hasSpecificTime && hasRecurringTime && !hasCheckInterval) {
            // Caso 4: notificami ogni [riferimento_temporale_intervallo] [se_evento]
            applyCaso4(query, temporalRef);
            
        } else if (hasEvent && hasSpecificTime && hasCheckInterval) {
            // Caso 5: notificami il [riferimento_temporale] [se_evento] e controlla ogni [intervallo]
            applyCaso5(query, temporalRef);
            
        } else {
            // Caso non riconosciuto
            query.setIsValid(false);
            query.setInvalidReason("Configurazione temporale non riconosciuta o non supportata");
            logger.warn("Unrecognized temporal configuration for prompt: {}", prompt);
        }
    }
    
    /**
     * Caso 0: notificami [se_evento]
     * => next_execution = DateTime.now(), cron = ogni giorno alle 10, to_check = 1, cron = 1, specific = 0
     */
    private void applyCaso0(TQuery query, TemporalReference temporalRef) {
        logger.debug("Applying Caso 0: event check without specific time");
        
        query.setToCheck(true);
        query.setCron(true);
        query.setDateSpecific(false);
        
        // Default: controlla ogni giorno alle 10:00
        query.setCronParams("0 10 * * *");
        query.setNextExecution(cronExpressionService.getNextExecution("0 10 * * *"));
        
        logger.info("Caso 0 applied - daily check at 10:00 AM");
    }
    
    /**
     * Caso 1: notificami il [riferimento_temporale] [qualcosa]
     * => next_execution = [riferimento_temporale], to_check = 0, cron = 0, specific = 1
     */
    private void applyCaso1(TQuery query, TemporalReference temporalRef) {
        logger.debug("Applying Caso 1: specific time notification");
        
        query.setToCheck(false);
        query.setCron(false);
        query.setDateSpecific(true);
        
        query.setNextExecution(temporalRef.getSpecificDateTime());
        query.setCronParams(null);
        
        logger.info("Caso 1 applied - specific time: {}", temporalRef.getSpecificDateTime());
    }
    
    /**
     * Caso 2: notificami ogni [riferimento_temporale_intervallo] [qualcosa]
     * => next_execution = calcolato, to_check = 0, cron = 1, specific = 0
     */
    private void applyCaso2(TQuery query, TemporalReference temporalRef) {
        logger.debug("Applying Caso 2: recurring notification");
        
        query.setToCheck(false);
        query.setCron(true);
        query.setDateSpecific(false);
        
        String cronExpression = temporalRef.getCronExpression();
        query.setCronParams(cronExpression);
        query.setNextExecution(cronExpressionService.getNextExecution(cronExpression));
        
        // Imposta valid_from alla prossima esecuzione se non già impostato
        if (query.getValidFrom() == null) {
            query.setValidFrom(query.getNextExecution());
        }
        
        logger.info("Caso 2 applied - recurring with cron: {}", cronExpression);
    }
    
    /**
     * Caso 3: notificami il [riferimento_temporale] [se_evento]
     * => next_execution = [riferimento_temporale], to_check = 1, cron = 0, specific = 1
     */
    private void applyCaso3(TQuery query, TemporalReference temporalRef) {
        logger.debug("Applying Caso 3: specific time event check");
        
        query.setToCheck(true);
        query.setCron(false);
        query.setDateSpecific(true);
        
        query.setNextExecution(temporalRef.getSpecificDateTime());
        query.setCronParams(null);
        
        logger.info("Caso 3 applied - specific time event check: {}", temporalRef.getSpecificDateTime());
    }
    
    /**
     * Caso 4: notificami ogni [riferimento_temporale_intervallo] [se_evento]
     * => next_execution = calcolato, to_check = 1, cron = 1, specific = 0
     */
    private void applyCaso4(TQuery query, TemporalReference temporalRef) {
        logger.debug("Applying Caso 4: recurring event check");
        
        query.setToCheck(true);
        query.setCron(true);
        query.setDateSpecific(false);
        
        String cronExpression = temporalRef.getCronExpression();
        query.setCronParams(cronExpression);
        query.setNextExecution(cronExpressionService.getNextExecution(cronExpression));
        
        logger.info("Caso 4 applied - recurring event check with cron: {}", cronExpression);
    }
    
    /**
     * Caso 5: notificami il [riferimento_temporale] [se_evento] e controlla ogni [intervallo]
     * => next_execution = [riferimento_temporale], to_check = 1, cron = 1, specific = 1
     */
    private void applyCaso5(TQuery query, TemporalReference temporalRef) {
        logger.debug("Applying Caso 5: specific time event check with custom interval");
        
        query.setToCheck(true);
        query.setCron(true);
        query.setDateSpecific(true);
        
        query.setNextExecution(temporalRef.getSpecificDateTime());
        
        // Usa l'intervallo di controllo personalizzato
        String checkCronExpression = temporalRef.getCheckIntervalCron();
        query.setCronParams(checkCronExpression);
        
        logger.info("Caso 5 applied - specific time: {}, check interval: {}", 
                   temporalRef.getSpecificDateTime(), checkCronExpression);
    }
    
    /**
     * Verifica se il prompt contiene una condizione di evento
     */
    private boolean hasEventCondition(String prompt) {
        Pattern eventPattern = Pattern.compile("(?i)\\bse\\b|\\bif\\b|\\bwhen\\b|\\bquando\\b|\\bscende\\b|\\bsale\\b|\\bcambia\\b|\\braggiunge\\b|\\bsupera\\b");
        return eventPattern.matcher(prompt).find();
    }
}