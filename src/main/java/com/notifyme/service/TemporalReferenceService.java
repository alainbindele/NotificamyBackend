package com.notifyme.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemporalReferenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(TemporalReferenceService.class);
    
    // Pattern per date specifiche
    private static final Pattern SPECIFIC_DATE_PATTERN = Pattern.compile(
        "(?i)\\b(il|on)\\s+(\\d{1,2})\\s+(gennaio|febbraio|marzo|aprile|maggio|giugno|luglio|agosto|settembre|ottobre|novembre|dicembre|january|february|march|april|may|june|july|august|september|october|november|december)(?:\\s+(\\d{4}))?(?:\\s+alle?\\s+(\\d{1,2})(?::(\\d{2}))?)?\\b"
    );
    
    // Pattern per orari specifici
    private static final Pattern SPECIFIC_TIME_PATTERN = Pattern.compile(
        "(?i)\\b(alle?|at)\\s+(\\d{1,2})(?::(\\d{2}))?(?:\\s*(am|pm|mattina|pomeriggio|sera))?\\b"
    );
    
    // Pattern per intervalli ricorrenti
    private static final Pattern RECURRING_PATTERN = Pattern.compile(
        "(?i)\\b(ogni|every)\\s+(\\d+)?\\s*(minuto|minuti|minute|minutes|ora|ore|hour|hours|giorno|giorni|day|days|settimana|settimane|week|weeks|mese|mesi|month|months)(?:\\s+alle?\\s+(\\d{1,2})(?::(\\d{2}))?)?\\b"
    );
    
    // Pattern per intervalli di controllo
    private static final Pattern CHECK_INTERVAL_PATTERN = Pattern.compile(
        "(?i)\\bcontrolla\\s+ogni\\s+(\\d+)?\\s*(minuto|minuti|ora|ore|giorno|giorni|settimana|settimane)\\b"
    );
    
    // Pattern per periodi di validità
    private static final Pattern VALIDITY_PERIOD_PATTERN = Pattern.compile(
        "(?i)\\b(dal|from)\\s+(.+?)\\s+(al|to|fino\\s+al|until)\\s+(.+?)\\b"
    );
    
    /**
     * Estrae tutti i riferimenti temporali dal prompt
     */
    public TemporalReference extractTemporalReference(String prompt) {
        logger.debug("Extracting temporal references from prompt: {}", prompt);
        
        TemporalReference reference = new TemporalReference();
        
        // Estrai data/ora specifica
        extractSpecificDateTime(prompt, reference);
        
        // Estrai pattern ricorrente
        extractRecurringPattern(prompt, reference);
        
        // Estrai intervallo di controllo
        extractCheckInterval(prompt, reference);
        
        // Estrai periodo di validità
        extractValidityPeriod(prompt, reference);
        
        logger.debug("Extracted temporal reference: {}", reference);
        return reference;
    }
    
    /**
     * Estrae data e ora specifiche
     */
    private void extractSpecificDateTime(String prompt, TemporalReference reference) {
        Matcher dateMatcher = SPECIFIC_DATE_PATTERN.matcher(prompt);
        Matcher timeMatcher = SPECIFIC_TIME_PATTERN.matcher(prompt);
        
        LocalDateTime specificDateTime = null;
        
        if (dateMatcher.find()) {
            try {
                int day = Integer.parseInt(dateMatcher.group(2));
                String monthStr = dateMatcher.group(3);
                String yearStr = dateMatcher.group(4);
                String hourStr = dateMatcher.group(5);
                String minuteStr = dateMatcher.group(6);
                
                int month = parseMonth(monthStr);
                int year = yearStr != null ? Integer.parseInt(yearStr) : LocalDateTime.now().getYear();
                int hour = hourStr != null ? Integer.parseInt(hourStr) : 0;
                int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
                
                specificDateTime = LocalDateTime.of(year, month, day, hour, minute);
                
                logger.debug("Extracted specific date from pattern: {}", specificDateTime);
                
            } catch (Exception e) {
                logger.warn("Failed to parse specific date: {}", dateMatcher.group(), e);
            }
        }
        
        // Se non abbiamo trovato una data completa, cerca solo l'orario
        if (specificDateTime == null && timeMatcher.find()) {
            try {
                String hourStr = timeMatcher.group(2);
                String minuteStr = timeMatcher.group(3);
                String period = timeMatcher.group(4);
                
                int hour = Integer.parseInt(hourStr);
                int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
                
                // Gestisci AM/PM o indicazioni italiane
                if (period != null) {
                    period = period.toLowerCase();
                    if (period.contains("pm") || period.contains("pomeriggio") || period.contains("sera")) {
                        if (hour < 12) hour += 12;
                    } else if (period.contains("mattina") && hour == 12) {
                        hour = 0;
                    }
                }
                
                // Usa domani se l'orario è già passato oggi
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime timeToday = now.toLocalDate().atTime(hour, minute);
                
                if (timeToday.isBefore(now)) {
                    specificDateTime = timeToday.plusDays(1);
                } else {
                    specificDateTime = timeToday;
                }
                
                logger.debug("Extracted specific time: {}", specificDateTime);
                
            } catch (Exception e) {
                logger.warn("Failed to parse specific time: {}", timeMatcher.group(), e);
            }
        }
        
        reference.setSpecificDateTime(specificDateTime);
    }
    
    /**
     * Estrae pattern ricorrente
     */
    private void extractRecurringPattern(String prompt, TemporalReference reference) {
        Matcher matcher = RECURRING_PATTERN.matcher(prompt);
        
        if (matcher.find()) {
            try {
                String numberStr = matcher.group(2);
                String unit = matcher.group(3).toLowerCase();
                String hourStr = matcher.group(4);
                String minuteStr = matcher.group(5);
                
                int number = numberStr != null ? Integer.parseInt(numberStr) : 1;
                int hour = hourStr != null ? Integer.parseInt(hourStr) : 9; // Default 9 AM
                int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
                
                String cronExpression = buildCronExpression(number, unit, hour, minute);
                reference.setCronExpression(cronExpression);
                
                logger.debug("Extracted recurring pattern: {} {} at {}:{} -> cron: {}", 
                           number, unit, hour, minute, cronExpression);
                
            } catch (Exception e) {
                logger.warn("Failed to parse recurring pattern: {}", matcher.group(), e);
            }
        }
    }
    
    /**
     * Estrae intervallo di controllo personalizzato
     */
    private void extractCheckInterval(String prompt, TemporalReference reference) {
        Matcher matcher = CHECK_INTERVAL_PATTERN.matcher(prompt);
        
        if (matcher.find()) {
            try {
                String numberStr = matcher.group(1);
                String unit = matcher.group(2).toLowerCase();
                
                int number = numberStr != null ? Integer.parseInt(numberStr) : 1;
                
                String cronExpression = buildCheckIntervalCron(number, unit);
                reference.setCheckIntervalCron(cronExpression);
                
                logger.debug("Extracted check interval: {} {} -> cron: {}", number, unit, cronExpression);
                
            } catch (Exception e) {
                logger.warn("Failed to parse check interval: {}", matcher.group(), e);
            }
        }
    }
    
    /**
     * Estrae periodo di validità
     */
    private void extractValidityPeriod(String prompt, TemporalReference reference) {
        Matcher matcher = VALIDITY_PERIOD_PATTERN.matcher(prompt);
        
        if (matcher.find()) {
            try {
                String fromStr = matcher.group(2).trim();
                String toStr = matcher.group(4).trim();
                
                LocalDateTime validFrom = parseFlexibleDateTime(fromStr);
                LocalDateTime validTo = parseFlexibleDateTime(toStr);
                
                reference.setValidFrom(validFrom);
                reference.setValidTo(validTo);
                
                logger.debug("Extracted validity period: {} to {}", validFrom, validTo);
                
            } catch (Exception e) {
                logger.warn("Failed to parse validity period: {}", matcher.group(), e);
            }
        }
    }
    
    /**
     * Costruisce espressione cron per pattern ricorrenti
     */
    private String buildCronExpression(int number, String unit, int hour, int minute) {
        switch (unit) {
            case "minuto":
            case "minuti":
            case "minute":
            case "minutes":
                if (number == 1) {
                    return "* * * * *"; // Ogni minuto
                } else {
                    return String.format("*/%d * * * *", number); // Ogni N minuti
                }
                
            case "ora":
            case "ore":
            case "hour":
            case "hours":
                if (number == 1) {
                    return String.format("%d * * * *", minute); // Ogni ora al minuto specificato
                } else {
                    return String.format("%d */%d * * *", minute, number); // Ogni N ore
                }
                
            case "giorno":
            case "giorni":
            case "day":
            case "days":
                if (number == 1) {
                    return String.format("%d %d * * *", minute, hour); // Ogni giorno
                } else {
                    return String.format("%d %d */%d * *", minute, hour, number); // Ogni N giorni
                }
                
            case "settimana":
            case "settimane":
            case "week":
            case "weeks":
                return String.format("%d %d * * 1", minute, hour); // Ogni lunedì (o ogni N settimane)
                
            case "mese":
            case "mesi":
            case "month":
            case "months":
                return String.format("%d %d 1 * *", minute, hour); // Il primo del mese
                
            default:
                return String.format("%d %d * * *", minute, hour); // Default: ogni giorno
        }
    }
    
    /**
     * Costruisce espressione cron per intervalli di controllo
     */
    private String buildCheckIntervalCron(int number, String unit) {
        switch (unit) {
            case "minuto":
            case "minuti":
                return number == 1 ? "* * * * *" : String.format("*/%d * * * *", number);
                
            case "ora":
            case "ore":
                return number == 1 ? "0 * * * *" : String.format("0 */%d * * *", number);
                
            case "giorno":
            case "giorni":
                return number == 1 ? "0 8 * * *" : String.format("0 8 */%d * *", number);
                
            case "settimana":
            case "settimane":
                return "0 8 * * 1"; // Ogni lunedì alle 8
                
            default:
                return "0 * * * *"; // Default: ogni ora
        }
    }
    
    /**
     * Converte nome mese in numero
     */
    private int parseMonth(String monthStr) {
        monthStr = monthStr.toLowerCase();
        switch (monthStr) {
            case "gennaio": case "january": return 1;
            case "febbraio": case "february": return 2;
            case "marzo": case "march": return 3;
            case "aprile": case "april": return 4;
            case "maggio": case "may": return 5;
            case "giugno": case "june": return 6;
            case "luglio": case "july": return 7;
            case "agosto": case "august": return 8;
            case "settembre": case "september": return 9;
            case "ottobre": case "october": return 10;
            case "novembre": case "november": return 11;
            case "dicembre": case "december": return 12;
            default: throw new IllegalArgumentException("Unknown month: " + monthStr);
        }
    }
    
    /**
     * Parsing flessibile di date/orari
     */
    private LocalDateTime parseFlexibleDateTime(String dateTimeStr) {
        // Implementazione semplificata - in produzione useresti una libreria più robusta
        try {
            // Prova vari formati
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    if (dateTimeStr.contains(":")) {
                        return LocalDateTime.parse(dateTimeStr, formatter);
                    } else {
                        return LocalDateTime.parse(dateTimeStr + " 00:00", 
                                                 DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    }
                } catch (DateTimeParseException ignored) {
                    // Prova il prossimo formato
                }
            }
            
            throw new DateTimeParseException("Unable to parse date", dateTimeStr, 0);
            
        } catch (Exception e) {
            logger.warn("Failed to parse flexible datetime: {}", dateTimeStr, e);
            return null;
        }
    }
}