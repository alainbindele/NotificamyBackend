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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

@Service
public class ValidationLogicService {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationLogicService.class);
    
    @Autowired
    private CronExpressionService cronExpressionService;
    
    /**
     * Applica la logica di validazione e calcolo usando SOLO i dati di ChatGPT
     */
    public TQuery applyValidationLogic(TUser user, String prompt, ChatGptValidationResponse validationResponse, String userTimezone) {
        logger.info("Applying validation logic for user: {} with prompt: {}", user.getEmail(), prompt);
        
        TQuery query = new TQuery(user, prompt);
        
        // Imposta la timezone dell'utente
        query.setTimezone(userTimezone);
        
        // 1. Mappa i dati base dalla risposta ChatGPT
        mapBasicValidationData(query, validationResponse);
        
        // 2. Se non è valido, ritorna subito
        if (!Boolean.TRUE.equals(query.getIsValid())) {
            logger.info("Query marked as invalid by ChatGPT, skipping further processing");
            return query;
        }
        
        // 3. Estrai e applica i dati temporali SOLO dalla risposta ChatGPT
        extractAndApplyTemporalDataFromChatGPT(query, validationResponse, userTimezone);
        
        // 4. Applica valutazione preliminare dell'intervallo di validità
        if (!applyPreliminaryValidation(query, validationResponse)) {
            return query; // Query chiusa o invalidata
        }
        
        // 5. Valida la configurazione finale
        validateFinalConfiguration(query);
        
        logger.info("Validation logic applied - Query valid: {}, cron: {}, specific: {}, check: {}, next_execution: {}, cron_params: {}", 
                   query.getIsValid(), query.getCron(), query.getDateSpecific(), query.getToCheck(), 
                   query.getNextExecution(), query.getCronParams());
        
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
            
            // Mappa tags se presenti
            if (response.getMetadata().getTags() != null) {
                try {
                    String tagsJson = String.join(",", response.getMetadata().getTags());
                    query.setTags(tagsJson);
                } catch (Exception e) {
                    logger.warn("Failed to serialize tags: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Estrae e applica i dati temporali ESCLUSIVAMENTE dalla risposta ChatGPT
     */
    private void extractAndApplyTemporalDataFromChatGPT(TQuery query, ChatGptValidationResponse validationResponse, String userTimezone) {
        if (validationResponse.getWhenNotify() == null) {
            logger.warn("No when_notify data in ChatGPT response");
            query.setIsValid(false);
            query.setInvalidReason("Mancano i dati temporali nella risposta di ChatGPT");
            return;
        }
        
        ChatGptValidationResponse.WhenNotify whenNotify = validationResponse.getWhenNotify();
        
        // 1. Estrai i flag di tipo
        boolean cron = false;
        boolean specific = false;
        boolean check = false;
        
        if (whenNotify.getTimeType() != null) {
            cron = Boolean.TRUE.equals(whenNotify.getTimeType().getCron());
            specific = Boolean.TRUE.equals(whenNotify.getTimeType().getSpecific());
            check = Boolean.TRUE.equals(whenNotify.getTimeType().getCheck());
        }
        
        query.setCron(cron);
        query.setDateSpecific(specific);
        query.setToCheck(check);
        
        logger.debug("ChatGPT temporal flags - cron: {}, specific: {}, check: {}", cron, specific, check);
        
        // 2. Estrai e applica cron_expression
        if (cron && whenNotify.getCronExpression() != null && !whenNotify.getCronExpression().trim().isEmpty()) {
            String cronExpression = whenNotify.getCronExpression().trim();
            query.setCronParams(cronExpression);
            
            // Calcola next_execution dal cron
            try {
                LocalDateTime nextExecution = cronExpressionService.getNextExecution(cronExpression);
                query.setNextExecution(nextExecution);
                logger.debug("Set next_execution from cron '{}': {}", cronExpression, nextExecution);
            } catch (Exception e) {
                logger.warn("Failed to calculate next execution from cron '{}': {}", cronExpression, e.getMessage());
            }
        }
        
        // 3. Estrai e applica date_time specifico
        if (specific && whenNotify.getDateTime() != null && !whenNotify.getDateTime().trim().isEmpty()) {
            String dateTimeStr = whenNotify.getDateTime().trim();
            
            try {
                LocalDateTime specificDateTime = parseDateTime(dateTimeStr, userTimezone);
                
                // Se non abbiamo già next_execution dal cron, usa la data specifica
                if (query.getNextExecution() == null) {
                    query.setNextExecution(specificDateTime);
                    logger.debug("Set next_execution from specific datetime '{}': {}", dateTimeStr, specificDateTime);
                }
                
            } catch (Exception e) {
                logger.warn("Failed to parse specific datetime '{}': {}", dateTimeStr, e.getMessage());
                query.setIsValid(false);
                query.setInvalidReason("Formato data/ora non valido: " + dateTimeStr);
                return;
            }
        }
        
        // 4. Estrai periodo di validità
        if (whenNotify.getStartDate() != null && !whenNotify.getStartDate().trim().isEmpty()) {
            try {
                LocalDateTime validFrom = parseDateTime(whenNotify.getStartDate().trim(), userTimezone);
                query.setValidFrom(validFrom);
                logger.debug("Set valid_from: {}", validFrom);
            } catch (Exception e) {
                logger.warn("Failed to parse start_date '{}': {}", whenNotify.getStartDate(), e.getMessage());
            }
        }
        
        if (whenNotify.getEndDate() != null && !whenNotify.getEndDate().trim().isEmpty()) {
            try {
                LocalDateTime validTo = parseDateTime(whenNotify.getEndDate().trim(), userTimezone);
                query.setValidTo(validTo);
                logger.debug("Set valid_to: {}", validTo);
            } catch (Exception e) {
                logger.warn("Failed to parse end_date '{}': {}", whenNotify.getEndDate(), e.getMessage());
            }
        }
        
        // 5. Fallback per casi senza configurazione temporale esplicita
        if (query.getNextExecution() == null && (query.getCronParams() == null || query.getCronParams().trim().isEmpty())) {
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
     * Parsa una stringa datetime nel formato "YYYY-MM-DD HH:MM:SS [TIMEZONE]" e converte in UTC
     */
    private LocalDateTime parseDateTime(String dateTimeStr, String userTimezone) {
        try {
            logger.debug("Parsing datetime: '{}' with user timezone: '{}'", dateTimeStr, userTimezone);
            
            // Controlla se la stringa contiene già una timezone
            if (dateTimeStr.contains(" ") && (dateTimeStr.contains("/") || dateTimeStr.contains("_"))) {
                // Formato: "2025-01-21 08:00:00 Asia/Tokyo"
                String[] parts = dateTimeStr.split(" ");
                if (parts.length >= 3) {
                    String datePart = parts[0];
                    String timePart = parts[1];
                    String timezonePart = parts[2];
                    
                    LocalDateTime localDateTime = LocalDateTime.parse(datePart + " " + timePart, 
                                                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    
                    // Converte dalla timezone specificata a UTC
                    ZoneId sourceZone = ZoneId.of(timezonePart);
                    ZonedDateTime zonedDateTime = localDateTime.atZone(sourceZone);
                    LocalDateTime utcDateTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
                    
                    logger.debug("Converted '{}' from {} to UTC: {}", dateTimeStr, timezonePart, utcDateTime);
                    return utcDateTime;
                }
            }
            
            // Formato senza timezone esplicita - usa la timezone dell'utente se disponibile
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
            };
            
            LocalDateTime localDateTime = null;
            for (DateTimeFormatter formatter : formatters) {
                try {
                    if (dateTimeStr.contains(":")) {
                        localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
                        break;
                    } else {
                        // Se non c'è orario, aggiungi 00:00:00
                        localDateTime = LocalDateTime.parse(dateTimeStr + " 00:00:00", 
                                                 DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        break;
                    }
                } catch (DateTimeParseException ignored) {
                    // Prova il prossimo formato
                }
            }
            
            if (localDateTime == null) {
                throw new DateTimeParseException("Unable to parse date", dateTimeStr, 0);
            }
            
            // Se abbiamo una timezone utente, converte da quella timezone a UTC
            if (userTimezone != null && !userTimezone.trim().isEmpty()) {
                try {
                    ZoneId userZone = ZoneId.of(userTimezone);
                    ZonedDateTime zonedDateTime = localDateTime.atZone(userZone);
                    LocalDateTime utcDateTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
                    
                    logger.debug("Converted '{}' from user timezone {} to UTC: {}", dateTimeStr, userTimezone, utcDateTime);
                    return utcDateTime;
                } catch (Exception e) {
                    logger.warn("Failed to convert from user timezone '{}', using as UTC: {}", userTimezone, e.getMessage());
                }
            }
            
            // Fallback: tratta come UTC
            logger.debug("Treating '{}' as UTC (no timezone conversion)", dateTimeStr);
            return localDateTime;
            
        } catch (Exception e) {
            logger.error("Failed to parse datetime '{}': {}", dateTimeStr, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Applica la valutazione preliminare dell'intervallo di validità
     */
    private boolean applyPreliminaryValidation(TQuery query, ChatGptValidationResponse validationResponse) {
        LocalDateTime now = LocalDateTime.now();
        
        LocalDateTime validFrom = query.getValidFrom();
        LocalDateTime validTo = query.getValidTo();
        
        // Caso: solo valid_from
        if (validFrom != null && validTo == null) {
            if (now.isBefore(validFrom)) {
                // Se next_execution non è già impostato, usalo come valid_from
                if (query.getNextExecution() == null || query.getNextExecution().isBefore(validFrom)) {
                    query.setNextExecution(validFrom);
                    logger.debug("Set next_execution to valid_from: {}", validFrom);
                }
            }
        }
        
        // Caso: solo valid_to
        else if (validFrom == null && validTo != null) {
            if (now.isAfter(validTo)) {
                query.setClosed(true);
                logger.info("Query closed: current time is after valid_to ({})", validTo);
                return false;
            }
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
            
            // 2. Se ora è prima di valid_from
            if (now.isBefore(validFrom)) {
                if (query.getNextExecution() == null || query.getNextExecution().isBefore(validFrom)) {
                    query.setNextExecution(validFrom);
                    logger.debug("Set next_execution to valid_from: {}", validFrom);
                }
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
     * Valida la configurazione finale
     */
    private void validateFinalConfiguration(TQuery query) {
        boolean cron = Boolean.TRUE.equals(query.getCron());
        boolean specific = Boolean.TRUE.equals(query.getDateSpecific());
        boolean check = Boolean.TRUE.equals(query.getToCheck());
        
        // Verifica configurazioni non valide secondo la tabella dei casi
        if (!cron && !specific && !check) {
            query.setIsValid(false);
            query.setInvalidReason("Configurazione temporale non riconosciuta: tutti i flag sono false");
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
        
        // Verifica configurazioni non valide (cron=1, specific=1, check=0)
        if (cron && specific && !check) {
            query.setIsValid(false);
            query.setInvalidReason("Configurazione non valida: non può essere sia ricorrente che specifica senza controllo");
            logger.warn("Invalid configuration: cron=true, specific=true, check=false");
            return;
        }
        
        logger.debug("Final validation passed - cron: {}, specific: {}, check: {}, next_execution: {}, cron_params: {}", 
                    cron, specific, check, query.getNextExecution(), query.getCronParams());
    }
}