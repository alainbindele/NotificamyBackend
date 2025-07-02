package com.notifyme.service;

import com.notifyme.entity.TQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(CheckService.class);
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Pattern generici per rilevare condizioni
    private static final Pattern CONDITION_PATTERN = Pattern.compile(
        "(?i)\\b(se|if|when|quando)\\b.*?\\b(scende|sale|raggiunge|supera|sotto|sopra|above|below|cambia|change|update|aggiorna|nuovo|new|diventa|becomes|è|is|sono|are)\\b"
    );
    
    // Pattern per valori numerici con unità
    private static final Pattern NUMERIC_CONDITION_PATTERN = Pattern.compile(
        "(?i)\\b(scende|sale|raggiunge|supera|sotto|sopra|above|below)\\b.*?\\b(\\d+(?:[.,]\\d+)?)\\s*([€$£%]|euro|dollar|usd|percent|percento)?\\b"
    );
    
    // Pattern per condizioni temporali
    private static final Pattern TEMPORAL_CONDITION_PATTERN = Pattern.compile(
        "(?i)\\b(dopo|before|prima|entro|within|by)\\b.*?\\b(\\d+)\\s*(minuti|ore|giorni|settimane|mesi|minutes|hours|days|weeks|months)\\b"
    );
    
    // Pattern per condizioni di stato
    private static final Pattern STATUS_CONDITION_PATTERN = Pattern.compile(
        "(?i)\\b(diventa|becomes|è|is|sono|are)\\b.*?\\b(disponibile|available|online|offline|attivo|active|inattivo|inactive|aperto|open|chiuso|closed)\\b"
    );
    
    /**
     * Valuta se una condizione è soddisfatta
     */
    public boolean evaluateCondition(TQuery query) {
        logger.debug("Evaluating condition for query {}: {}", query.getId(), query.getPrompt());
        
        try {
            String prompt = query.getPrompt().toLowerCase();
            
            // Controlla se è una condizione riconosciuta
            if (!isRecognizedCondition(prompt)) {
                logger.warn("Unrecognized condition type for query {}, assuming satisfied", query.getId());
                return true;
            }
            
            // Determina il tipo di condizione e valuta
            ConditionType type = determineConditionType(prompt);
            
            switch (type) {
                case NUMERIC:
                    return evaluateNumericCondition(query);
                case TEMPORAL:
                    return evaluateTemporalCondition(query);
                case STATUS:
                    return evaluateStatusCondition(query);
                case CONTENT:
                    return evaluateContentCondition(query);
                case GENERIC:
                default:
                    return evaluateGenericCondition(query);
            }
            
        } catch (Exception e) {
            logger.error("Error evaluating condition for query {}: {}", query.getId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Ottiene il risultato della condizione per la notifica
     */
    public String getConditionResult(TQuery query) {
        logger.debug("Getting condition result for query {}", query.getId());
        
        try {
            String prompt = query.getPrompt().toLowerCase();
            ConditionType type = determineConditionType(prompt);
            
            switch (type) {
                case NUMERIC:
                    return getNumericConditionResult(query);
                case TEMPORAL:
                    return getTemporalConditionResult(query);
                case STATUS:
                    return getStatusConditionResult(query);
                case CONTENT:
                    return getContentConditionResult(query);
                case GENERIC:
                default:
                    return getGenericConditionResult(query);
            }
            
        } catch (Exception e) {
            logger.error("Error getting condition result for query {}: {}", query.getId(), e.getMessage(), e);
            return "Errore nel controllo della condizione";
        }
    }
    
    /**
     * Verifica se è una condizione riconosciuta
     */
    private boolean isRecognizedCondition(String prompt) {
        return CONDITION_PATTERN.matcher(prompt).find();
    }
    
    /**
     * Determina il tipo di condizione
     */
    private ConditionType determineConditionType(String prompt) {
        if (NUMERIC_CONDITION_PATTERN.matcher(prompt).find()) {
            return ConditionType.NUMERIC;
        }
        if (TEMPORAL_CONDITION_PATTERN.matcher(prompt).find()) {
            return ConditionType.TEMPORAL;
        }
        if (STATUS_CONDITION_PATTERN.matcher(prompt).find()) {
            return ConditionType.STATUS;
        }
        if (prompt.contains("notizie") || prompt.contains("news") || prompt.contains("aggiornament") || 
            prompt.contains("update") || prompt.contains("articol") || prompt.contains("article")) {
            return ConditionType.CONTENT;
        }
        return ConditionType.GENERIC;
    }
    
    /**
     * Valuta condizioni numeriche (prezzi, percentuali, quantità)
     */
    private boolean evaluateNumericCondition(TQuery query) {
        logger.info("Evaluating numeric condition for query {}", query.getId());
        
        // TODO: Implementare controlli reali basati sul contenuto del prompt
        // Es: API per prezzi, metriche, statistiche, etc.
        
        // Simulazione: 25% di probabilità che la condizione sia soddisfatta
        boolean conditionMet = Math.random() < 0.25;
        
        logger.info("Numeric condition for query {} - Condition met: {}", query.getId(), conditionMet);
        return conditionMet;
    }
    
    /**
     * Valuta condizioni temporali
     */
    private boolean evaluateTemporalCondition(TQuery query) {
        logger.info("Evaluating temporal condition for query {}", query.getId());
        
        // TODO: Implementare controlli temporali reali
        // Es: scadenze, eventi programmati, etc.
        
        // Simulazione: 30% di probabilità
        boolean conditionMet = Math.random() < 0.30;
        
        logger.info("Temporal condition for query {} - Condition met: {}", query.getId(), conditionMet);
        return conditionMet;
    }
    
    /**
     * Valuta condizioni di stato
     */
    private boolean evaluateStatusCondition(TQuery query) {
        logger.info("Evaluating status condition for query {}", query.getId());
        
        // TODO: Implementare controlli di stato reali
        // Es: servizi online/offline, disponibilità, etc.
        
        // Simulazione: 20% di probabilità
        boolean conditionMet = Math.random() < 0.20;
        
        logger.info("Status condition for query {} - Condition met: {}", query.getId(), conditionMet);
        return conditionMet;
    }
    
    /**
     * Valuta condizioni di contenuto (notizie, aggiornamenti)
     */
    private boolean evaluateContentCondition(TQuery query) {
        logger.info("Evaluating content condition for query {}", query.getId());
        
        // TODO: Implementare controlli di contenuto reali
        // Es: RSS feeds, API news, social media, etc.
        
        // Simulazione: 40% di probabilità (contenuti cambiano spesso)
        boolean conditionMet = Math.random() < 0.40;
        
        logger.info("Content condition for query {} - Condition met: {}", query.getId(), conditionMet);
        return conditionMet;
    }
    
    /**
     * Valuta condizioni generiche
     */
    private boolean evaluateGenericCondition(TQuery query) {
        logger.info("Evaluating generic condition for query {}", query.getId());
        
        // TODO: Implementare controlli generici o AI-based
        
        // Simulazione: 35% di probabilità
        boolean conditionMet = Math.random() < 0.35;
        
        logger.info("Generic condition for query {} - Condition met: {}", query.getId(), conditionMet);
        return conditionMet;
    }
    
    /**
     * Genera risultato per condizioni numeriche
     */
    private String getNumericConditionResult(TQuery query) {
        Matcher matcher = NUMERIC_CONDITION_PATTERN.matcher(query.getPrompt().toLowerCase());
        if (matcher.find()) {
            String condition = matcher.group(1);
            String value = matcher.group(2);
            String unit = matcher.group(3);
            
            return String.format("🔢 Condizione numerica soddisfatta!\n\n" +
                                "Condizione: %s %s%s\n" +
                                "Stato: Verificata\n\n" +
                                "Prompt originale: %s", 
                                condition, value, unit != null ? " " + unit : "", 
                                query.getPrompt());
        }
        
        return "🔢 Condizione numerica verificata!\n\nLa condizione specificata è stata soddisfatta.\n\nPrompt: " + query.getPrompt();
    }
    
    /**
     * Genera risultato per condizioni temporali
     */
    private String getTemporalConditionResult(TQuery query) {
        return String.format("⏰ Condizione temporale soddisfatta!\n\n" +
                            "La condizione di tempo specificata è stata raggiunta.\n\n" +
                            "Prompt originale: %s", 
                            query.getPrompt());
    }
    
    /**
     * Genera risultato per condizioni di stato
     */
    private String getStatusConditionResult(TQuery query) {
        return String.format("🔄 Cambio di stato rilevato!\n\n" +
                            "Lo stato monitorato ha subito una modifica.\n\n" +
                            "Prompt originale: %s", 
                            query.getPrompt());
    }
    
    /**
     * Genera risultato per condizioni di contenuto
     */
    private String getContentConditionResult(TQuery query) {
        return String.format("📰 Nuovo contenuto disponibile!\n\n" +
                            "Sono stati rilevati nuovi aggiornamenti per l'argomento richiesto.\n\n" +
                            "Prompt originale: %s", 
                            query.getPrompt());
    }
    
    /**
     * Genera risultato per condizioni generiche
     */
    private String getGenericConditionResult(TQuery query) {
        return String.format("✅ Condizione verificata!\n\n" +
                            "La condizione specificata nel tuo prompt è stata soddisfatta.\n\n" +
                            "Prompt originale: %s\n\n" +
                            "Dettagli: %s", 
                            query.getPrompt(), 
                            query.getSummaryText() != null ? query.getSummaryText() : "Nessun dettaglio aggiuntivo");
    }
    
    /**
     * Metodo per testare una condizione manualmente
     */
    public String testCondition(String prompt) {
        logger.info("Testing condition: {}", prompt);
        
        if (!isRecognizedCondition(prompt.toLowerCase())) {
            return "❌ Condizione non riconosciuta: " + prompt;
        }
        
        ConditionType type = determineConditionType(prompt.toLowerCase());
        
        return String.format("✅ Condizione riconosciuta!\n" +
                            "Tipo: %s\n" +
                            "Prompt: %s", 
                            type.getDescription(), prompt);
    }
    
    /**
     * Enum per i tipi di condizione
     */
    private enum ConditionType {
        NUMERIC("Condizione numerica (prezzi, quantità, percentuali)"),
        TEMPORAL("Condizione temporale (scadenze, timing)"),
        STATUS("Condizione di stato (online/offline, disponibilità)"),
        CONTENT("Condizione di contenuto (notizie, aggiornamenti)"),
        GENERIC("Condizione generica");
        
        private final String description;
        
        ConditionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}