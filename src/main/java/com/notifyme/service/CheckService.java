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
    
    // Pattern per rilevare condizioni di prezzo
    private static final Pattern PRICE_CONDITION_PATTERN = Pattern.compile(
        "(?i)\\b(bitcoin|btc|ethereum|eth|prezzo|price)\\b.*?\\b(scende|sale|raggiunge|supera|sotto|sopra|above|below)\\b.*?\\b(\\d+(?:[.,]\\d+)?)\\s*([€$£]|euro|dollar|usd)?\\b"
    );
    
    // Pattern per rilevare condizioni generiche
    private static final Pattern GENERIC_CONDITION_PATTERN = Pattern.compile(
        "(?i)\\b(se|if|when|quando)\\b.*?\\b(cambia|change|update|aggiorna|nuovo|new)\\b"
    );
    
    /**
     * Valuta se una condizione è soddisfatta
     */
    public boolean evaluateCondition(TQuery query) {
        logger.debug("Evaluating condition for query {}: {}", query.getId(), query.getPrompt());
        
        try {
            String prompt = query.getPrompt().toLowerCase();
            
            // Controlla condizioni di prezzo
            if (isPriceCondition(prompt)) {
                return evaluatePriceCondition(query);
            }
            
            // Controlla condizioni generiche
            if (isGenericCondition(prompt)) {
                return evaluateGenericCondition(query);
            }
            
            // Se non riconosce la condizione, assume che sia soddisfatta
            logger.warn("Unknown condition type for query {}, assuming satisfied", query.getId());
            return true;
            
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
            
            if (isPriceCondition(prompt)) {
                return getPriceConditionResult(query);
            }
            
            if (isGenericCondition(prompt)) {
                return getGenericConditionResult(query);
            }
            
            // Fallback
            return "Condizione verificata: " + query.getSummaryText();
            
        } catch (Exception e) {
            logger.error("Error getting condition result for query {}: {}", query.getId(), e.getMessage(), e);
            return "Errore nel controllo della condizione";
        }
    }
    
    /**
     * Verifica se è una condizione di prezzo
     */
    private boolean isPriceCondition(String prompt) {
        return PRICE_CONDITION_PATTERN.matcher(prompt).find();
    }
    
    /**
     * Verifica se è una condizione generica
     */
    private boolean isGenericCondition(String prompt) {
        return GENERIC_CONDITION_PATTERN.matcher(prompt).find();
    }
    
    /**
     * Valuta condizioni di prezzo (simulato)
     */
    private boolean evaluatePriceCondition(TQuery query) {
        logger.info("Evaluating price condition for query {}", query.getId());
        
        // TODO: Implementare controllo reale dei prezzi
        // Per ora simuliamo con una probabilità del 20% che la condizione sia soddisfatta
        
        Matcher matcher = PRICE_CONDITION_PATTERN.matcher(query.getPrompt().toLowerCase());
        if (matcher.find()) {
            String asset = matcher.group(1);
            String condition = matcher.group(2);
            String priceStr = matcher.group(3);
            
            logger.debug("Price condition detected - Asset: {}, Condition: {}, Price: {}", 
                        asset, condition, priceStr);
            
            // Simulazione: 20% di probabilità che la condizione sia soddisfatta
            boolean conditionMet = Math.random() < 0.2;
            
            logger.info("Price condition for query {} - Asset: {}, Condition met: {}", 
                       query.getId(), asset, conditionMet);
            
            return conditionMet;
        }
        
        return false;
    }
    
    /**
     * Valuta condizioni generiche (simulato)
     */
    private boolean evaluateGenericCondition(TQuery query) {
        logger.info("Evaluating generic condition for query {}", query.getId());
        
        // TODO: Implementare controlli reali per condizioni generiche
        // Per ora simuliamo con una probabilità del 30% che la condizione sia soddisfatta
        
        boolean conditionMet = Math.random() < 0.3;
        
        logger.info("Generic condition for query {} - Condition met: {}", query.getId(), conditionMet);
        
        return conditionMet;
    }
    
    /**
     * Ottiene il risultato di una condizione di prezzo
     */
    private String getPriceConditionResult(TQuery query) {
        Matcher matcher = PRICE_CONDITION_PATTERN.matcher(query.getPrompt().toLowerCase());
        if (matcher.find()) {
            String asset = matcher.group(1);
            String condition = matcher.group(2);
            String priceStr = matcher.group(3);
            
            // Simula un prezzo attuale
            double targetPrice = Double.parseDouble(priceStr.replace(",", "."));
            double currentPrice = targetPrice * (0.95 + Math.random() * 0.1); // ±5% dal target
            
            return String.format("🚨 Condizione soddisfatta!\n\n" +
                                "Asset: %s\n" +
                                "Condizione: %s %s\n" +
                                "Prezzo attuale: %.2f\n" +
                                "Prezzo target: %s\n\n" +
                                "Prompt originale: %s", 
                                asset.toUpperCase(), condition, priceStr, 
                                currentPrice, priceStr, query.getPrompt());
        }
        
        return "Condizione di prezzo soddisfatta";
    }
    
    /**
     * Ottiene il risultato di una condizione generica
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
        
        if (isPriceCondition(prompt)) {
            return "Condizione di prezzo rilevata: " + prompt;
        }
        
        if (isGenericCondition(prompt)) {
            return "Condizione generica rilevata: " + prompt;
        }
        
        return "Tipo di condizione non riconosciuto: " + prompt;
    }
}