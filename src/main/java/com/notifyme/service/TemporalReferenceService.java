package com.notifyme.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemporalReferenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(TemporalReferenceService.class);
    
    // CORRETTO: Pattern per date specifiche con "domani", "oggi", etc.
    private static final Pattern SPECIFIC_DATE_PATTERN = Pattern.compile(
        "(?i)\\b(domani|oggi|stasera|stamattina|tomorrow|today)\\s*(?:alle?\\s+(\\d{1,2})(?::(\\d{2}))?(?:\\s*(di\\s+)?(mattina|pomeriggio|sera|am|pm))?)?\\b"
    );
    
    // NUOVO: Pattern separato per date con giorno/mese
    private static final Pattern CALENDAR_DATE_PATTERN = Pattern.compile(
        "(?i)\\b(il)\\s+(\\d{1,2})\\s+(gennaio|febbraio|marzo|aprile|maggio|giugno|luglio|agosto|settembre|ottobre|novembre|dicembre|january|february|march|april|may|june|july|august|september|october|november|december)(?:\\s+(\\d{4}))?(?:\\s+alle?\\s+(\\d{1,2})(?::(\\d{2}))?(?:\\s*(di\\s+)?(mattina|pomeriggio|sera|am|pm))?)?\\b"
    );
    
    // CORRETTO: Pattern per orari specifici
    private static final Pattern SPECIFIC_TIME_PATTERN = Pattern.compile(
        "(?i)\\ball[''']?\\s*(\\d{1,2})(?::(\\d{2}))?(?:\\s*(di\\s+)?(am|pm|mattina|pomeriggio|sera))?\\b"
    );
    
    // NUOVO: Pattern per giorni della settimana con orario
    private static final Pattern WEEKDAY_PATTERN = Pattern.compile(
        "(?i)\\b(ogni|every)\\s+(lunedì|martedì|mercoledì|giovedì|venerdì|sabato|domenica|monday|tuesday|wednesday|thursday|friday|saturday|sunday)(?:\\s+alle?\\s+(\\d{1,2})(?::(\\d{2}))?)?\\b"
    );
    
    // Pattern per intervalli ricorrenti - MIGLIORATO per catturare "controllando ogni X"
    private static final Pattern RECURRING_PATTERN = Pattern.compile(
        "(?i)\\b(ogni|every|controllando\\s+ogni|controlla\\s+ogni)\\s+(\\d+)?\\s*(minuto|minuti|minute|minutes|ora|ore|hour|hours|giorno|giorni|day|days|settimana|settimane|week|weeks|mese|mesi|month|months)(?:\\s+alle?\\s+(\\d{1,2})(?::(\\d{2}))?)?\\b"
    );
    
    // Pattern per intervalli di controllo - NUOVO pattern specifico
    private static final Pattern CHECK_INTERVAL_PATTERN = Pattern.compile(
        "(?i)\\b(controllando|controlla|controllo|checking|check)\\s+(ogni|every)\\s+(\\d+)?\\s*(minuto|minuti|ora|ore|giorno|giorni|settimana|settimane)(?:\\s+alle?\\s+(\\d{1,2})(?::(\\d{2}))?)?\\b"
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
        
        // NUOVO: Prima controlla giorni della settimana
        extractWeekdayPattern(prompt, reference);
        
        // Estrai data/ora specifica (solo se non abbiamo già un weekday)
        if (!reference.hasRecurringPattern()) {
            extractSpecificDateTime(prompt, reference);
        }
        
        // Estrai pattern ricorrente (inclusi controlli) - solo se non abbiamo weekday
        if (!reference.hasRecurringPattern()) {
            extractRecurringPattern(prompt, reference);
        }
        
        // Estrai intervallo di controllo specifico
        extractCheckInterval(prompt, reference);
        
        // Estrai periodo di validità
        extractValidityPeriod(prompt, reference);
        
        logger.debug("Extracted temporal reference: {}", reference);
        return reference;
    }
    
    /**
     * NUOVO: Estrae pattern per giorni della settimana
     */
    private void extractWeekdayPattern(String prompt, TemporalReference reference) {
        Matcher matcher = WEEKDAY_PATTERN.matcher(prompt);
        
        if (matcher.find()) {
            try {
                String weekdayStr = matcher.group(2).toLowerCase();
                String hourStr = matcher.group(3);
                String minuteStr = matcher.group(4);
                
                // Converti giorno della settimana
                int dayOfWeek = parseWeekday(weekdayStr);
                
                // Estrai orario
                int hour = 10; // Default
                int minute = 0; // Default
                
                if (hourStr != null) {
                    hour = Integer.parseInt(hourStr);
                    minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
                } else {
                    // Cerca orario altrove nel prompt
                    Matcher timeInPrompt = SPECIFIC_TIME_PATTERN.matcher(prompt);
                    if (timeInPrompt.find()) {
                        hour = Integer.parseInt(timeInPrompt.group(1));
                        minute = timeInPrompt.group(2) != null ? Integer.parseInt(timeInPrompt.group(2)) : 0;
                    }
                }
                
                // Costruisci espressione cron per il giorno della settimana
                String cronExpression = String.format("%d %d * * %d", minute, hour, dayOfWeek);
                reference.setCronExpression(cronExpression);
                
                logger.info("Extracted weekday pattern: '{}' at {}:{} -> cron: {}", 
                           weekdayStr, hour, minute, cronExpression);
                
            } catch (Exception e) {
                logger.warn("Failed to parse weekday pattern: {}", matcher.group(), e);
            }
        }
    }
    
    /**
     * CORRETTO: Estrae data e ora specifiche con supporto per "domani", "oggi", etc.
     */
    private void extractSpecificDateTime(String prompt, TemporalReference reference) {
        LocalDateTime specificDateTime = null;
        
        // Prima prova con date relative (domani, oggi, etc.)
        Matcher relativeDateMatcher = SPECIFIC_DATE_PATTERN.matcher(prompt);
        if (relativeDateMatcher.find()) {
            specificDateTime = parseRelativeDate(relativeDateMatcher, prompt);
        }
        
        // Se non trovato, prova con date del calendario (il 21 gennaio, etc.)
        if (specificDateTime == null) {
            Matcher calendarMatcher = CALENDAR_DATE_PATTERN.matcher(prompt);
            if (calendarMatcher.find()) {
                specificDateTime = parseCalendarDate(calendarMatcher);
            }
        }
        
        // Se ancora non trovato, cerca solo l'orario
        if (specificDateTime == null) {
            Matcher timeMatcher = SPECIFIC_TIME_PATTERN.matcher(prompt);
            if (timeMatcher.find()) {
                specificDateTime = parseTimeOnly(timeMatcher);
            }
        }
        
        reference.setSpecificDateTime(specificDateTime);
        logger.info("Final extracted specific datetime: {}", specificDateTime);
    }
    
    /**
     * NUOVO: Parsa date relative come "domani", "oggi"
     */
    private LocalDateTime parseRelativeDate(Matcher matcher, String fullPrompt) {
        try {
            String dateIndicator = matcher.group(1).toLowerCase(); // "domani", "oggi", etc.
            String hourStr = matcher.group(2);
            String minuteStr = matcher.group(3);
            String periodStr = matcher.group(5); // "mattina", "pomeriggio", "sera"
            
            logger.debug("Parsing relative date - indicator: '{}', hour: '{}', minute: '{}', period: '{}'", 
                        dateIndicator, hourStr, minuteStr, periodStr);
            
            LocalDateTime baseDate = LocalDateTime.now();
            
            // Determina la data base
            switch (dateIndicator) {
                case "domani":
                case "tomorrow":
                    baseDate = baseDate.plusDays(1);
                    break;
                case "oggi":
                case "today":
                    // Mantieni la data di oggi
                    break;
                case "stasera":
                    // Stasera = oggi alle 20:00 di default
                    return baseDate.withHour(20).withMinute(0).withSecond(0).withNano(0);
                case "stamattina":
                    // Stamattina = oggi alle 9:00 di default
                    return baseDate.withHour(9).withMinute(0).withSecond(0).withNano(0);
            }
            
            // Se non c'è orario nel match principale, cerca "all'una" nel prompt completo
            if (hourStr == null) {
                Matcher timeInPrompt = SPECIFIC_TIME_PATTERN.matcher(fullPrompt);
                if (timeInPrompt.find()) {
                    hourStr = timeInPrompt.group(1);
                    minuteStr = timeInPrompt.group(2);
                    periodStr = timeInPrompt.group(4);
                    logger.debug("Found time in full prompt - hour: '{}', minute: '{}', period: '{}'", 
                                hourStr, minuteStr, periodStr);
                }
            }
            
            // Gestisci l'orario
            if (hourStr != null) {
                int hour = Integer.parseInt(hourStr);
                int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
                
                // CORRETTO: Gestisci periodo del giorno
                if (periodStr != null) {
                    periodStr = periodStr.toLowerCase().replace("di ", "").trim();
                    logger.debug("Processing period: '{}'", periodStr);
                    
                    if ("pomeriggio".equals(periodStr) && hour <= 12) {
                        if (hour == 12) {
                            // 12 di pomeriggio = 12:00 (mezzogiorno)
                            hour = 12;
                        } else {
                            // 1-11 di pomeriggio = 13-23
                            hour += 12;
                        }
                    } else if ("sera".equals(periodStr) && hour <= 12) {
                        if (hour == 12) {
                            hour = 0; // 12 di sera = mezzanotte
                        } else {
                            hour += 12;
                        }
                    } else if ("pm".equals(periodStr) && hour < 12) {
                        hour += 12;
                    } else if (("am".equals(periodStr) || "mattina".equals(periodStr)) && hour == 12) {
                        hour = 0;
                    }
                }
                
                LocalDateTime result = baseDate.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                logger.info("Parsed relative date: '{}' -> {}", matcher.group(), result);
                return result;
                
            } else {
                // Se non c'è orario, usa un default ragionevole
                LocalDateTime result = baseDate.withHour(9).withMinute(0).withSecond(0).withNano(0);
                logger.info("Parsed relative date without time: '{}' -> {} (default 9:00)", matcher.group(), result);
                return result;
            }
            
        } catch (Exception e) {
            logger.warn("Failed to parse relative date: {}", matcher.group(), e);
            return null;
        }
    }
    
    /**
     * NUOVO: Parsa date del calendario come "il 21 gennaio"
     */
    private LocalDateTime parseCalendarDate(Matcher matcher) {
        try {
            String dayStr = matcher.group(2);
            String monthStr = matcher.group(3);
            String yearStr = matcher.group(4);
            String hourStr = matcher.group(5);
            String minuteStr = matcher.group(6);
            String periodStr = matcher.group(8);
            
            int day = Integer.parseInt(dayStr);
            int month = parseMonth(monthStr);
            int year = yearStr != null ? Integer.parseInt(yearStr) : LocalDateTime.now().getYear();
            int hour = hourStr != null ? Integer.parseInt(hourStr) : 9;
            int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
            
            // Gestisci periodo del giorno
            if (periodStr != null && hourStr != null) {
                periodStr = periodStr.toLowerCase().replace("di ", "").trim();
                if ("pomeriggio".equals(periodStr) && hour <= 12 && hour != 12) {
                    hour += 12;
                } else if ("sera".equals(periodStr) && hour <= 12) {
                    if (hour != 12) hour += 12;
                }
            }
            
            LocalDateTime result = LocalDateTime.of(year, month, day, hour, minute);
            logger.info("Parsed calendar date: '{}' -> {}", matcher.group(), result);
            return result;
            
        } catch (Exception e) {
            logger.warn("Failed to parse calendar date: {}", matcher.group(), e);
            return null;
        }
    }
    
    /**
     * NUOVO: Parsa solo l'orario
     */
    private LocalDateTime parseTimeOnly(Matcher matcher) {
        try {
            String hourStr = matcher.group(1);
            String minuteStr = matcher.group(2);
            String periodStr = matcher.group(4);
            
            int hour = Integer.parseInt(hourStr);
            int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
            
            // Gestisci AM/PM o indicazioni italiane
            if (periodStr != null) {
                periodStr = periodStr.toLowerCase().replace("di ", "").trim();
                if ("pm".equals(periodStr) || "pomeriggio".equals(periodStr) || "sera".equals(periodStr)) {
                    if (hour < 12) hour += 12;
                } else if (("mattina".equals(periodStr) || "am".equals(periodStr)) && hour == 12) {
                    hour = 0;
                }
            }
            
            // Usa domani se l'orario è già passato oggi
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime timeToday = now.toLocalDate().atTime(hour, minute);
            
            LocalDateTime result;
            if (timeToday.isBefore(now)) {
                result = timeToday.plusDays(1);
            } else {
                result = timeToday;
            }
            
            logger.info("Parsed time only: '{}' -> {}", matcher.group(), result);
            return result;
            
        } catch (Exception e) {
            logger.warn("Failed to parse time only: {}", matcher.group(), e);
            return null;
        }
    }
    
    /**
     * Estrae pattern ricorrente - MIGLIORATO per "controllando ogni"
     */
    private void extractRecurringPattern(String prompt, TemporalReference reference) {
        Matcher matcher = RECURRING_PATTERN.matcher(prompt);
        
        if (matcher.find()) {
            try {
                String trigger = matcher.group(1).toLowerCase(); // "ogni", "controllando ogni", etc.
                String numberStr = matcher.group(2);
                String unit = matcher.group(3).toLowerCase();
                String hourStr = matcher.group(4);
                String minuteStr = matcher.group(5);
                
                int number = numberStr != null ? Integer.parseInt(numberStr) : 1;
                int hour = hourStr != null ? Integer.parseInt(hourStr) : 
                          (trigger.contains("controllando") || trigger.contains("controlla") ? 15 : 9); // Default 15 per controlli
                int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
                
                String cronExpression = buildCronExpression(number, unit, hour, minute);
                reference.setCronExpression(cronExpression);
                
                logger.debug("Extracted recurring pattern from '{}': {} {} at {}:{} -> cron: {}", 
                           trigger, number, unit, hour, minute, cronExpression);
                
            } catch (Exception e) {
                logger.warn("Failed to parse recurring pattern: {}", matcher.group(), e);
            }
        }
    }
    
    /**
     * Estrae intervallo di controllo personalizzato - MIGLIORATO
     */
    private void extractCheckInterval(String prompt, TemporalReference reference) {
        Matcher matcher = CHECK_INTERVAL_PATTERN.matcher(prompt);
        
        if (matcher.find()) {
            try {
                String numberStr = matcher.group(3);
                String unit = matcher.group(4).toLowerCase();
                String hourStr = matcher.group(5);
                String minuteStr = matcher.group(6);
                
                int number = numberStr != null ? Integer.parseInt(numberStr) : 1;
                int hour = hourStr != null ? Integer.parseInt(hourStr) : 0; // Per intervalli di controllo
                int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
                
                String cronExpression = buildCheckIntervalCron(number, unit, hour, minute);
                reference.setCheckIntervalCron(cronExpression);
                
                logger.debug("Extracted check interval: {} {} at {}:{} -> cron: {}", 
                           number, unit, hour, minute, cronExpression);
                
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
     * NUOVO: Converte giorno della settimana in numero cron (1=lunedì, 7=domenica)
     */
    private int parseWeekday(String weekdayStr) {
        weekdayStr = weekdayStr.toLowerCase();
        switch (weekdayStr) {
            case "lunedì": case "monday": return 1;
            case "martedì": case "tuesday": return 2;
            case "mercoledì": case "wednesday": return 3;
            case "giovedì": case "thursday": return 4;
            case "venerdì": case "friday": return 5;
            case "sabato": case "saturday": return 6;
            case "domenica": case "sunday": return 7;
            default: throw new IllegalArgumentException("Unknown weekday: " + weekdayStr);
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
     * Costruisce espressione cron per intervalli di controllo - MIGLIORATO
     */
    private String buildCheckIntervalCron(int number, String unit, int hour, int minute) {
        switch (unit) {
            case "minuto":
            case "minuti":
                return number == 1 ? "* * * * *" : String.format("*/%d * * * *", number);
                
            case "ora":
            case "ore":
                if (hour > 0 || minute > 0) {
                    // Se è specificato un orario, usa quello
                    return number == 1 ? String.format("%d * * * *", minute) : 
                                       String.format("%d */%d * * *", minute, number);
                } else {
                    // Altrimenti usa il default
                    return number == 1 ? "0 * * * *" : String.format("0 */%d * * *", number);
                }
                
            case "giorno":
            case "giorni":
                if (hour > 0 || minute > 0) {
                    // Se è specificato un orario, usa quello
                    return number == 1 ? String.format("%d %d * * *", minute, hour) : 
                                       String.format("%d %d */%d * *", minute, hour, number);
                } else {
                    // Altrimenti usa il default (15:00 per controlli)
                    return number == 1 ? "0 15 * * *" : String.format("0 15 */%d * *", number);
                }
                
            case "settimana":
            case "settimane":
                return String.format("%d %d * * 1", minute, hour > 0 ? hour : 15); // Ogni lunedì
                
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