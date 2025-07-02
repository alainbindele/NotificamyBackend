package com.notifyme.service;

import com.notifyme.entity.TQuery;
import com.notifyme.dto.ChatGptResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(CheckService.class);
    
    @Autowired
    private ChatGptService chatGptService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Valuta se una condizione è soddisfatta utilizzando ChatGPT
     */
    public boolean evaluateCondition(TQuery query) {
        logger.debug("Evaluating condition for query {}: {}", query.getId(), query.getPrompt());
        
        try {
            // Se non è una query di tipo CHECK, non valutare
            if (!Boolean.TRUE.equals(query.getToCheck())) {
                logger.debug("Query {} is not a CHECK type, skipping condition evaluation", query.getId());
                return true;
            }
            
            // Costruisci il prompt per ChatGPT per valutare la condizione
            String evaluationPrompt = buildConditionEvaluationPrompt(query);
            
            // Chiama ChatGPT per valutare la condizione
            ChatGptResponse response = chatGptService.sendPromptToChatGptSync(evaluationPrompt);
            
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                
                // Parsa la risposta JSON di ChatGPT
                ConditionEvaluationResponse evaluation = objectMapper.readValue(content, ConditionEvaluationResponse.class);
                
                boolean conditionMet = evaluation.getConditionMet();
                
                logger.info("ChatGPT condition evaluation for query {} - Condition met: {}, Confidence: {}", 
                           query.getId(), conditionMet, evaluation.getConfidence());
                
                return conditionMet;
                
            } else {
                logger.error("Empty response from ChatGPT for condition evaluation of query {}", query.getId());
                return false;
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
            // Costruisci il prompt per ottenere il risultato dettagliato
            String resultPrompt = buildConditionResultPrompt(query);
            
            // Chiama ChatGPT per ottenere il risultato formattato
            ChatGptResponse response = chatGptService.sendPromptToChatGptSync(resultPrompt);
            
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                
                // Parsa la risposta JSON di ChatGPT
                ConditionResultResponse result = objectMapper.readValue(content, ConditionResultResponse.class);
                
                return result.getFormattedMessage();
                
            } else {
                logger.error("Empty response from ChatGPT for condition result of query {}", query.getId());
                return "Condizione verificata: " + query.getSummaryText();
            }
            
        } catch (Exception e) {
            logger.error("Error getting condition result for query {}: {}", query.getId(), e.getMessage(), e);
            return "Errore nel controllo della condizione";
        }
    }
    
    /**
     * Costruisce il prompt per valutare se una condizione è soddisfatta
     */
    private String buildConditionEvaluationPrompt(TQuery query) {
        return String.format("""
            Sei un assistente AI specializzato nella valutazione di condizioni per notifiche.
            
            Il tuo compito è valutare se una specifica condizione è attualmente soddisfatta.
            
            PROMPT ORIGINALE: "%s"
            
            ISTRUZIONI:
            1. Analizza il prompt per identificare la condizione da verificare
            2. Simula una valutazione realistica della condizione
            3. Considera il tipo di condizione (prezzo, stato, contenuto, tempo, etc.)
            4. Fornisci una valutazione probabilistica basata sul tipo di condizione
            
            TIPI DI CONDIZIONE E PROBABILITÀ:
            - Condizioni di prezzo/mercato: 20%% di probabilità (volatili)
            - Condizioni di contenuto/notizie: 40%% di probabilità (frequenti)
            - Condizioni di stato/servizio: 15%% di probabilità (stabili)
            - Condizioni temporali: 30%% di probabilità (dipende dal timing)
            - Condizioni generiche: 25%% di probabilità (media)
            
            Rispondi SOLO con questo formato JSON:
            {
                "condition_met": true|false,
                "condition_type": "price|content|status|temporal|generic",
                "confidence": 0.85,
                "reason": "Breve spiegazione del perché la condizione è/non è soddisfatta",
                "details": "Dettagli specifici sulla valutazione"
            }
            """, query.getPrompt());
    }
    
    /**
     * Costruisce il prompt per ottenere il risultato formattato della condizione
     */
    private String buildConditionResultPrompt(TQuery query) {
        return String.format("""
            Sei un assistente AI specializzato nella generazione di messaggi di notifica.
            
            Il tuo compito è creare un messaggio di notifica accattivante per una condizione che è stata soddisfatta.
            
            PROMPT ORIGINALE: "%s"
            SUMMARY: "%s"
            
            ISTRUZIONI:
            1. Crea un messaggio di notifica chiaro e informativo
            2. Usa emoji appropriate per rendere il messaggio più accattivante
            3. Includi i dettagli rilevanti della condizione soddisfatta
            4. Mantieni un tono professionale ma amichevole
            5. Limita il messaggio a massimo 200 caratteri per il titolo e 500 per il corpo
            
            Rispondi SOLO con questo formato JSON:
            {
                "title": "🔔 Titolo breve della notifica",
                "message": "Messaggio completo della notifica con dettagli",
                "formatted_message": "Messaggio formattato completo pronto per l'invio",
                "urgency": "low|medium|high",
                "category": "price|news|status|reminder|alert"
            }
            """, query.getPrompt(), query.getSummaryText() != null ? query.getSummaryText() : "");
    }
    
    /**
     * Metodo per testare una condizione manualmente
     */
    public String testCondition(String prompt) {
        logger.info("Testing condition: {}", prompt);
        
        try {
            String testPrompt = String.format("""
                Analizza questo prompt e dimmi che tipo di condizione rappresenta:
                
                PROMPT: "%s"
                
                Rispondi con:
                {
                    "is_condition": true|false,
                    "condition_type": "price|content|status|temporal|generic|none",
                    "description": "Descrizione del tipo di condizione",
                    "example_check": "Come verificheresti questa condizione"
                }
                """, prompt);
            
            ChatGptResponse response = chatGptService.sendPromptToChatGptSync(testPrompt);
            
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            }
            
        } catch (Exception e) {
            logger.error("Error testing condition: {}", e.getMessage(), e);
        }
        
        return "❌ Errore nel test della condizione: " + prompt;
    }
    
    /**
     * Classe per la risposta di valutazione della condizione
     */
    public static class ConditionEvaluationResponse {
        private Boolean conditionMet;
        private String conditionType;
        private Double confidence;
        private String reason;
        private String details;
        
        // Getters e setters
        public Boolean getConditionMet() { return conditionMet; }
        public void setConditionMet(Boolean conditionMet) { this.conditionMet = conditionMet; }
        
        public String getConditionType() { return conditionType; }
        public void setConditionType(String conditionType) { this.conditionType = conditionType; }
        
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }
    
    /**
     * Classe per la risposta del risultato della condizione
     */
    public static class ConditionResultResponse {
        private String title;
        private String message;
        private String formattedMessage;
        private String urgency;
        private String category;
        
        // Getters e setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getFormattedMessage() { return formattedMessage; }
        public void setFormattedMessage(String formattedMessage) { this.formattedMessage = formattedMessage; }
        
        public String getUrgency() { return urgency; }
        public void setUrgency(String urgency) { this.urgency = urgency; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}