package com.notifyme.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Pattern;

@Service
public class CronExpressionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CronExpressionService.class);
    
    // Pattern per validare espressioni cron (formato: minuto ora giorno mese giorno_settimana)
    private static final Pattern CRON_PATTERN = Pattern.compile(
        "^\\s*([0-9*,/-]+)\\s+([0-9*,/-]+)\\s+([0-9*,/-]+)\\s+([0-9*,/-]+)\\s+([0-9*,/-]+)\\s*$"
    );
    
    /**
     * Calcola la prossima esecuzione basata su un'espressione cron
     */
    public LocalDateTime getNextExecution(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            logger.warn("Empty cron expression provided");
            return LocalDateTime.now().plusHours(1); // Fallback
        }
        
        cronExpression = cronExpression.trim();
        
        if (!CRON_PATTERN.matcher(cronExpression).matches()) {
            logger.warn("Invalid cron expression format: {}", cronExpression);
            return LocalDateTime.now().plusHours(1); // Fallback
        }
        
        try {
            String[] parts = cronExpression.split("\\s+");
            
            if (parts.length != 5) {
                logger.warn("Cron expression must have 5 parts: {}", cronExpression);
                return LocalDateTime.now().plusHours(1);
            }
            
            String minute = parts[0];
            String hour = parts[1];
            String dayOfMonth = parts[2];
            String month = parts[3];
            String dayOfWeek = parts[4];
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextExecution = calculateNextExecution(now, minute, hour, dayOfMonth, month, dayOfWeek);
            
            logger.debug("Calculated next execution for cron '{}': {}", cronExpression, nextExecution);
            return nextExecution;
            
        } catch (Exception e) {
            logger.error("Error calculating next execution for cron '{}': {}", cronExpression, e.getMessage(), e);
            return LocalDateTime.now().plusHours(1); // Fallback
        }
    }
    
    /**
     * Calcola la prossima esecuzione basata sui componenti cron - MIGLIORATO per giorni della settimana
     */
    private LocalDateTime calculateNextExecution(LocalDateTime now, String minute, String hour, 
                                               String dayOfMonth, String month, String dayOfWeek) {
        
        LocalDateTime candidate = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        
        // NUOVO: Gestione specifica per giorni della settimana
        if (isNumeric(minute) && isNumeric(hour) && "*".equals(dayOfMonth) && 
            "*".equals(month) && isNumeric(dayOfWeek)) {
            
            int targetMinute = Integer.parseInt(minute);
            int targetHour = Integer.parseInt(hour);
            int targetDayOfWeek = Integer.parseInt(dayOfWeek);
            
            // Converti da formato cron (1=lunedì, 7=domenica) a DayOfWeek Java
            DayOfWeek javaDayOfWeek = convertCronDayToJavaDayOfWeek(targetDayOfWeek);
            
            LocalDateTime target = now.with(TemporalAdjusters.nextOrSame(javaDayOfWeek))
                                     .withHour(targetHour)
                                     .withMinute(targetMinute)
                                     .withSecond(0)
                                     .withNano(0);
            
            // Se l'orario di oggi è già passato, vai alla prossima settimana
            if (target.isBefore(now) || target.equals(now)) {
                target = target.with(TemporalAdjusters.next(javaDayOfWeek));
            }
            
            logger.debug("Calculated next weekday execution: {} (day {}) at {}:{} -> {}", 
                        javaDayOfWeek, targetDayOfWeek, targetHour, targetMinute, target);
            
            return target;
        }
        
        // Caso: ogni minuto (* * * * *)
        if ("*".equals(minute) && "*".equals(hour) && "*".equals(dayOfMonth) && 
            "*".equals(month) && "*".equals(dayOfWeek)) {
            return candidate;
        }
        
        // Caso: orario fisso ogni giorno (M H * * *)
        if (isNumeric(minute) && isNumeric(hour) && "*".equals(dayOfMonth) && 
            "*".equals(month) && "*".equals(dayOfWeek)) {
            
            int targetMinute = Integer.parseInt(minute);
            int targetHour = Integer.parseInt(hour);
            
            LocalDateTime target = now.toLocalDate().atTime(targetHour, targetMinute);
            
            if (target.isBefore(now) || target.equals(now)) {
                target = target.plusDays(1);
            }
            
            return target;
        }
        
        // Caso: ogni ora al minuto specificato (M * * * *)
        if (isNumeric(minute) && "*".equals(hour) && "*".equals(dayOfMonth) && 
            "*".equals(month) && "*".equals(dayOfWeek)) {
            
            int targetMinute = Integer.parseInt(minute);
            
            LocalDateTime target = now.withMinute(targetMinute).withSecond(0).withNano(0);
            
            if (target.isBefore(now) || target.equals(now)) {
                target = target.plusHours(1);
            }
            
            return target;
        }
        
        // Caso: intervalli con */N
        if (minute.startsWith("*/")) {
            int interval = Integer.parseInt(minute.substring(2));
            int nextMinute = ((now.getMinute() / interval) + 1) * interval;
            
            if (nextMinute >= 60) {
                return now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
            } else {
                return now.withMinute(nextMinute).withSecond(0).withNano(0);
            }
        }
        
        if (hour.startsWith("*/")) {
            int interval = Integer.parseInt(hour.substring(2));
            int nextHour = ((now.getHour() / interval) + 1) * interval;
            
            int targetMinute = isNumeric(minute) ? Integer.parseInt(minute) : 0;
            
            if (nextHour >= 24) {
                return now.plusDays(1).withHour(0).withMinute(targetMinute).withSecond(0).withNano(0);
            } else {
                return now.withHour(nextHour).withMinute(targetMinute).withSecond(0).withNano(0);
            }
        }
        
        // Fallback: aggiungi 1 ora
        logger.warn("Unsupported cron pattern, using 1-hour fallback: {} {} {} {} {}", 
                   minute, hour, dayOfMonth, month, dayOfWeek);
        return now.plusHours(1);
    }
    
    /**
     * NUOVO: Converte giorno della settimana da formato cron a DayOfWeek Java
     */
    private DayOfWeek convertCronDayToJavaDayOfWeek(int cronDay) {
        switch (cronDay) {
            case 1: return DayOfWeek.MONDAY;
            case 2: return DayOfWeek.TUESDAY;
            case 3: return DayOfWeek.WEDNESDAY;
            case 4: return DayOfWeek.THURSDAY;
            case 5: return DayOfWeek.FRIDAY;
            case 6: return DayOfWeek.SATURDAY;
            case 7: return DayOfWeek.SUNDAY;
            default: 
                logger.warn("Invalid cron day of week: {}, defaulting to Monday", cronDay);
                return DayOfWeek.MONDAY;
        }
    }
    
    /**
     * Verifica se una stringa è numerica
     */
    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Valida un'espressione cron
     */
    public boolean isValidCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }
        
        return CRON_PATTERN.matcher(cronExpression.trim()).matches();
    }
    
    /**
     * Genera una descrizione leggibile dell'espressione cron - MIGLIORATO per giorni della settimana
     */
    public String describeCronExpression(String cronExpression) {
        if (!isValidCronExpression(cronExpression)) {
            return "Espressione cron non valida";
        }
        
        String[] parts = cronExpression.trim().split("\\s+");
        String minute = parts[0];
        String hour = parts[1];
        String dayOfMonth = parts[2];
        String month = parts[3];
        String dayOfWeek = parts[4];
        
        // Caso specifico per giorni della settimana
        if (isNumeric(minute) && isNumeric(hour) && "*".equals(dayOfMonth) && 
            "*".equals(month) && isNumeric(dayOfWeek)) {
            
            String dayName = getDayName(Integer.parseInt(dayOfWeek));
            return String.format("Ogni %s alle %s:%02d", dayName, hour, Integer.parseInt(minute));
        }
        
        // Implementazione semplificata per i casi più comuni
        if ("*".equals(minute) && "*".equals(hour)) {
            return "Ogni minuto";
        }
        
        if (isNumeric(minute) && isNumeric(hour) && "*".equals(dayOfMonth)) {
            return String.format("Ogni giorno alle %s:%02d", hour, Integer.parseInt(minute));
        }
        
        if (isNumeric(minute) && "*".equals(hour)) {
            return String.format("Ogni ora al minuto %s", minute);
        }
        
        if (minute.startsWith("*/")) {
            return String.format("Ogni %s minuti", minute.substring(2));
        }
        
        if (hour.startsWith("*/")) {
            return String.format("Ogni %s ore", hour.substring(2));
        }
        
        return "Programmazione personalizzata: " + cronExpression;
    }
    
    /**
     * NUOVO: Ottiene il nome del giorno dalla settimana
     */
    private String getDayName(int cronDay) {
        switch (cronDay) {
            case 1: return "lunedì";
            case 2: return "martedì";
            case 3: return "mercoledì";
            case 4: return "giovedì";
            case 5: return "venerdì";
            case 6: return "sabato";
            case 7: return "domenica";
            default: return "giorno sconosciuto";
        }
    }
}