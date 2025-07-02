package com.notifyme.service;

import com.notifyme.dto.ChatGptRequest;
import com.notifyme.dto.ChatGptResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class ChatGptService {

    private static final Logger logger = LoggerFactory.getLogger(ChatGptService.class);
    private final WebClient webClient;
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    public ChatGptService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<ChatGptResponse> sendPromptToChatGpt(String prompt) {
        String policy = buildValidationPolicy();
        ChatGptRequest request = new ChatGptRequest(policy, prompt);

        return webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(buildOpenAiRequest(request))
                .retrieve()
                .bodyToMono(ChatGptResponse.class);
    }

    public ChatGptResponse sendPromptToChatGptSync(String prompt) {
        try {
            // Verifica che la chiave API sia presente
            if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("${OPENAI_API_KEY}")) {
                logger.error("OpenAI API key is not configured properly. Current value: {}", 
                           apiKey != null ? (apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : apiKey) : "null");
                throw new RuntimeException("OpenAI API key is not configured. Please set OPENAI_API_KEY environment variable.");
            }
            
            logger.info("Sending request to OpenAI API: {}", apiUrl);
            logger.debug("Using API key starting with: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
            
            // Determina se è una richiesta di validazione o di valutazione condizione
            String policy;
            if (prompt.contains("valutazione di condizioni") || prompt.contains("condition_met") || 
                prompt.contains("formatted_message") || prompt.contains("Testing condition")) {
                // È una richiesta di valutazione condizione - usa policy semplificata
                policy = "Sei un assistente AI. Rispondi in formato JSON come richiesto.";
            } else {
                // È una richiesta di validazione prompt - usa policy completa
                policy = buildValidationPolicy();
            }
            
            ChatGptRequest request = new ChatGptRequest(policy, prompt);

            ChatGptResponse response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(buildOpenAiRequest(request))
                    .retrieve()
                    .bodyToMono(ChatGptResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
                    
            logger.info("Successfully received response from OpenAI API");
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to get response from ChatGPT: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get response from ChatGPT: " + e.getMessage(), e);
        }
    }

    private String buildValidationPolicy() {
        return """
                Sei un assistente virtuale specializzato nella validazione e configurazione di notifiche temporali.
                
                Il tuo compito è analizzare prompt per notifiche e restituire una configurazione JSON completa.
                
                TIPI DI NOTIFICHE SUPPORTATE:
                1. Notifiche ricorrenti (es. ogni giorno, ogni settimana, ogni 2 ore)
                2. Notifiche programmate per un momento preciso (es. "domani alle 8", "il 3 luglio alle 14")
                3. Controlli condizionali (es. "se bitcoin scende", "quando arriva un'email")
                
                REGOLE DI VALIDAZIONE:
                1. Il prompt DEVE contenere un riferimento temporale (quando eseguire)
                2. Il prompt DEVE contenere cosa fare (notificare cosa o controllare quale condizione)
                3. VIETATO: linguaggio offensivo, SQL injection, script, comandi dannosi
                4. VIETATO: richieste irragionevoli (es. ogni millisecondo, per 300 anni)
                5. Lunghezza massima: 2000 caratteri
                
                CALCOLO TEMPORALE - MOLTO IMPORTANTE:
                - Calcola SEMPRE la data/ora esatta per next_execution
                - Usa il fuso orario Europe/Rome (UTC+1/+2)
                - Per "domani" usa la data di domani
                - Per "oggi" usa la data di oggi
                - Per orari come "all'una di pomeriggio" = 13:00
                - Per "mattina" usa 09:00, "pomeriggio" usa 14:00, "sera" usa 20:00
                - Se manca l'orario, usa 09:00 come default
                
                ESEMPI DI CALCOLO TEMPORALE:
                - "domani all'una di pomeriggio" → 2025-01-XX 13:00:00 (dove XX = domani)
                - "ogni giorno alle 9" → cron="0 9 * * *", next_execution=prossime 09:00
                - "il 21 gennaio alle 15" → 2025-01-21 15:00:00
                - "ogni ora" → cron="0 * * * *", next_execution=prossima ora
                
                CONFIGURAZIONI VALIDE DEI FLAG TYPE (CASI 0-5):
                
                CASO 0: cron=1, date_specific=0, to_check=1
                Esempio: "notificami se bitcoin scende sotto i 1000$"
                → cron_params="0 10 * * *", next_execution=prossime 10:00
                
                CASO 1: cron=0, date_specific=1, to_check=0  
                Esempio: "notificami il 21 gennaio alle 9 sulle notizie"
                → next_execution=2025-01-21 09:00:00
                
                CASO 2: cron=1, date_specific=0, to_check=0
                Esempio: "notificami ogni giorno alle 9 sulle notizie"
                → cron_params="0 9 * * *", next_execution=prossime 09:00
                
                CASO 3: cron=0, date_specific=1, to_check=1
                Esempio: "notificami il 21 gennaio alle 9 se bitcoin scende"
                → next_execution=2025-01-21 09:00:00
                
                CASO 4: cron=1, date_specific=0, to_check=1
                Esempio: "notificami ogni giorno se bitcoin scende"
                → cron_params="0 10 * * *", next_execution=prossime 10:00
                
                CASO 5: cron=1, date_specific=1, to_check=1
                Esempio: "notificami il 21 gennaio se bitcoin scende, controlla ogni ora"
                → cron_params="0 * * * *", next_execution=2025-01-21 00:00:00
                
                CONFIGURAZIONI NON VALIDE:
                - cron=0, date_specific=0, to_check=0 (nessuna configurazione temporale)
                
                FORMATO RISPOSTA JSON (OBBLIGATORIO):
                {
                   "response_type": "notification_prompt_template",
                   "timestamp": "2025-01-XX 10:45:01Z",
                   "generated_by": "system",
                   "when_notify":{
                        "type":{
                            "CRON": true|false,
                            "SPECIFIC": true|false,
                            "CHECK": true|false
                        },
                        "cron_expression": "0 9 * * *"|null,
                        "date_time": "2025-01-XX 13:00:00"|null,
                        "start_date": "2025-01-XX 09:00:00"|null,
                        "end_date": "2025-12-31 23:59:59"|null
                   },
                   "validity": {
                     "out_of_bounds_prompt_length": true|false,
                     "offensive_language_detected": true|false,
                     "nasty_instruction_detected": true|false,
                     "purpose_valid": true|false,
                     "reasonable_usage": true|false,
                     "self_enforcing": true|false,
                     "valid_prompt": true|false,
                     "invalid_reason": "motivo se non valido"|null
                   },
                   "summary": {
                     "text": "Riassunto chiaro di cosa fa la notifica",
                     "language": "it|en",
                     "category": "notification|reminder|alert|check"
                   },
                   "metadata": {
                     "model_version": "gpt-3.5-turbo",
                     "confidence_score": 0.95,
                     "policy_enforced": true,
                     "tags": ["tag1", "tag2", "tag3"]
                   }
                }
                
                ISTRUZIONI SPECIFICHE:
                - CALCOLA SEMPRE date/ore esatte in formato YYYY-MM-DD HH:MM:SS
                - Per "domani" calcola la data di domani reale
                - Per "oggi" usa la data di oggi
                - Per cron_expression usa il formato standard Linux cron
                - Se il prompt è ambiguo, chiedi chiarimenti nel campo invalid_reason
                - Se mancano informazioni temporali, marca come non valido
                
                ESEMPI PRATICI:
                
                Input: "mandami una notifica per buttare la pasta domani all'una di pomeriggio"
                Output: {
                    "when_notify": {
                        "type": {"CRON": false, "SPECIFIC": true, "CHECK": false},
                        "date_time": "2025-01-XX 13:00:00"
                    },
                    "validity": {"valid_prompt": true},
                    "summary": {"text": "Promemoria per buttare la pasta domani alle 13:00"}
                }
                
                Input: "notificami se il prezzo di bitcoin scende sotto i 100$ controllando ogni giorno alle 15"
                Output: {
                    "when_notify": {
                        "type": {"CRON": true, "SPECIFIC": false, "CHECK": true},
                        "cron_expression": "0 15 * * *"
                    },
                    "validity": {"valid_prompt": true},
                    "summary": {"text": "Controllo giornaliero prezzo Bitcoin sotto 100$ alle 15:00"}
                }
                
                IMPORTANTE: Rispondi SEMPRE e SOLO con il JSON richiesto, senza testo aggiuntivo.
                """;
    }

    private Object buildOpenAiRequest(ChatGptRequest request) {
        return new Object() {
            public final String model = "gpt-3.5-turbo";
            public final Object[] messages = {
                new Object() {
                    public final String role = "system";
                    public final String content = request.getPolicy();
                },
                new Object() {
                    public final String role = "user";
                    public final String content = request.getClientPrompt();
                }
            };
            public final int max_tokens = 1500;
            public final double temperature = 0.3;
        };
    }
}