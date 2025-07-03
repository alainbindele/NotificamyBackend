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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

@Service
public class ChatGptService {

    private static final Logger logger = LoggerFactory.getLogger(ChatGptService.class);
    private final WebClient webClient;
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${openai.model:gpt-4o}")
    private String model;

    public ChatGptService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<ChatGptResponse> sendPromptToChatGpt(String prompt) {
        String policy = buildPolicy();
        String currentTimestamp = getCurrentUtcTimestamp();
        ChatGptRequest request = new ChatGptRequest(policy, prompt);

        return webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(buildOpenAiRequest(request, currentTimestamp))
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
            
            String currentTimestamp = getCurrentUtcTimestamp();
            logger.info("Sending request to OpenAI API: {} using model: {} at UTC time: {}", apiUrl, model, currentTimestamp);
            logger.debug("Using API key starting with: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
            
            String policy = buildPolicy();
            ChatGptRequest request = new ChatGptRequest(policy, prompt);
            
            // Costruisci la richiesta e logga per debugging
            Map<String, Object> requestBody = buildOpenAiRequest(request, currentTimestamp);
            logger.debug("OpenAI request body: {}", requestBody);

            ChatGptResponse response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                             clientResponse -> {
                                 return clientResponse.bodyToMono(String.class)
                                     .map(errorBody -> {
                                         logger.error("OpenAI API error response: {}", errorBody);
                                         return new RuntimeException("OpenAI API error: " + errorBody);
                                     });
                             })
                    .bodyToMono(ChatGptResponse.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
                    
            logger.info("Successfully received response from OpenAI API using {}", model);
            return response;
            
        } catch (WebClientResponseException e) {
            logger.error("OpenAI API HTTP error - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get response from ChatGPT: " + e.getStatusCode() + " " + e.getStatusText() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("Failed to get response from ChatGPT ({}): {}", model, e.getMessage(), e);
            throw new RuntimeException("Failed to get response from ChatGPT: " + e.getMessage(), e);
        }
    }

    /**
     * Ottiene il timestamp UTC corrente nel formato ISO 8601
     */
    private String getCurrentUtcTimestamp() {
        return ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }

    private String buildPolicy() {
        return """
                Sei un assistente virtuale la cui unica funzione è validare un prompt riguardante:
                
                notifiche ricorrenti (es. ogni giorno, ogni settimana, ogni 2 ore, ogni mercoledì)
                oppure notifiche programmate per un momento preciso nel futuro (es. "ricordamelo domani alle 8", "tra 10 minuti", "il 3 luglio alle 14")
                
                🚨 REGOLA FONDAMENTALE PER LE DATE: 
                TUTTE le date specificate dall'utente si riferiscono SEMPRE alla PROSSIMA occorrenza utile di quella data.
                
                ESEMPI CRITICI:
                - "notificami il 21 gennaio" = il PROSSIMO 21 gennaio (2025 se siamo nel 2024, o 2026 se siamo già a febbraio 2025)
                - "ricordami il 15 marzo" = il PROSSIMO 15 marzo disponibile
                - "avvisami il 31 dicembre" = il PROSSIMO 31 dicembre
                
                IMPORTANTE: Usa la data/ora UTC corrente fornita nel messaggio come riferimento temporale per tutte le valutazioni e calcoli di date/orari.
                
                Il prompt deve rispettare rigorosamente i seguenti vincoli:
                1) Deve indicare esplicitamente un riferimento all'intervallo temporale in cui essere eseguito 
                    oppure ad una data/tempo in cui essere notificato 
                    (se non viene specificata l'ora e/o giorno manda la notifica a mezzanotte, 
                    se invece viene specificato qualcosa come "mattina, pomeriggio o sera" considera un orario mediano es: mattina= 10AM, pomeriggio=16PM, sera=20PM) 
                3) può contenere riferimenti a notifiche che richiedano il controllo periodico 
                    e condizionale di un certo evento (es. "cambio di prezzo di un prodotto", "cambio di prezzo di un prodotto in un certo periodo di tempo")
                    
                2) Divieto assoluto di linguaggio offensivo, discriminatorio, volgare o anche solo potenzialmente inappropriato.
                
                3) Nessuna istruzione dannosa o "nasty instruction", incluse ma non limitate a:
                    SQL injection,
                    comandi per ottenere accesso non autorizzato, 
                    codice sorgente malintenzionato,
                    codice sorgente di qualsiasi tipo e linguaggio di programmazione o markup, 
                    exploit di sistema, 
                    manipolazione di dati, 
                    bypass di restrizioni, 
                    automazioni illecite, 
                    manipolazioni esterne all'applicazione
                    crawling di altri siti.
                4) Lunghezza del prompt: il prompt generato non deve superare i 100 caratteri.
                5) Il contenuto deve restare vincolato allo scopo specifico dell'app: notificare l'utente. È vietato includere richieste che esulano da questa funzione.
                6) Ogni richiesta deve riflettere il buon senso e un utilizzo ragionevole: ad esempio, niente notifiche ogni millisecondo, o richieste assurde come "notificami ogni volta che respiri", o "ricordamelo per 300 anni".
                7) Il prompt non può generare o ispirare un prompt che violerebbe questa stessa policy.
                8) non inventare altre regole e non supporre nulla che non sia scritto nel prompt esplicitamente a parte il selezionare un orario congruo per riferimenti generici (es. mattina = 10AM, pomeriggio=16PM e sera = 20PM)
                
                REGOLE PER ORARI CONGRUI - QUANDO INVENTARE E QUANDO NO:
                
                ✅ DEVI INVENTARE un orario congruo quando il prompt contiene riferimenti temporali generici:
                - "notificami domani mattina" → DEVI inventare "10:00" (mattina = 10AM)
                - "ricordami stasera" → DEVI inventare "20:00" (sera = 20PM)  
                - "avvisami nel pomeriggio" → DEVI inventare "16:00" (pomeriggio = 16PM)
                - "notificami la mattina del 15 gennaio" → DEVI inventare "2025-01-15 10:00:00"
                
                ❌ NON DEVI INVENTARE orari quando il prompt non ha riferimenti temporali specifici:
                - "notificami quando piove" → NON inventare orario, usa CHECK=true con default giornaliero
                - "dimmi se bitcoin scende" → NON inventare orario, usa CHECK=true con default giornaliero
                - "avvisami se arriva una email" → NON inventare orario, usa CHECK=true con default giornaliero
                
                ✅ ORARI ESPLICITI - usa quelli forniti:
                - "notificami alle 14:39" → usa "14:39" esatto
                - "ricordami il 21 gennaio alle 9" → usa "2025-01-21 09:00:00" esatto
                
                IMPORTANTE: Per i giorni della settimana (lunedì, martedì, mercoledì, etc.) considera sempre CRON=true, non SPECIFIC=true.
                
                PARSING TEMPORALE DETTAGLIATO - REGOLE PER DATE FUTURE:
                Devi analizzare con precisione i riferimenti temporali usando la data/ora UTC corrente come riferimento e compilare correttamente i campi:

                🎯 ALGORITMO PER DATE SPECIFICHE:
                1. Estrai giorno e mese dal prompt (es. "21 gennaio")
                2. Determina l'anno: 
                   - Se la data è nel futuro rispetto alla data UTC corrente → usa l'anno corrente
                   - Se la data è nel passato rispetto alla data UTC corrente → usa l'anno successivo
                3. Se non viene specificato l'orario → usa 00:00:00 (mezzanotte)
                4. Se viene specificato un orario generico (mattina/pomeriggio/sera) → usa gli orari standard
                
                ESEMPI PRATICI (assumendo data UTC corrente = 2024-07-03):
                - "notificami il 21 gennaio" → "2025-01-21 00:00:00" (prossimo gennaio)
                - "ricordami il 15 marzo" → "2025-03-15 00:00:00" (prossimo marzo)
                - "avvisami il 1 giugno" → "2025-06-01 00:00:00" (prossimo giugno)
                - "notificami il 21 gennaio alle 9" → "2025-01-21 09:00:00"
                - "ricordami il 15 marzo mattina" → "2025-03-15 10:00:00"
                
                CALCOLI TEMPORALI BASATI SU UTC:
                - "domani" = data UTC corrente + 1 giorno
                - "oggi" = data UTC corrente
                - "stasera" = data UTC corrente alle 20:00:00
                - "domani mattina" = data UTC corrente + 1 giorno alle 10:00:00
                - "tra 2 ore" = ora UTC corrente + 2 ore
                - "tra 30 minuti" = ora UTC corrente + 30 minuti
                - "il prossimo lunedì" = prossimo lunedì dalla data UTC corrente
                
                1) CRON_EXPRESSION: Usa il formato standard cron "minuto ora giorno mese giorno_settimana"
                   - Per "ogni mercoledì alle 14:39" → "39 14 * * 3" (3=mercoledì)
                   - Per "ogni giorno alle 9" → "0 9 * * *"
                   - Per "ogni ora" → "0 * * * *"
                   - Per "ogni 2 ore" → "0 */2 * * *"
                   - Giorni settimana: 1=lunedì, 2=martedì, 3=mercoledì, 4=giovedì, 5=venerdì, 6=sabato, 7=domenica
                
                2) DATE_TIME: Usa il formato "YYYY-MM-DD HH:MM:SS" per date/orari specifici CALCOLATI dalla data UTC corrente
                   - Per "domani all'una di pomeriggio" → calcola la data di domani dalla data UTC e imposta "YYYY-MM-DD 13:00:00"
                   - Per "il 21 gennaio alle 9" → "2025-01-21 09:00:00" (SEMPRE prossima occorrenza)
                   - Per "stasera" → data UTC corrente con orario 20:00:00
                   - Per "domani mattina" → data UTC corrente + 1 giorno con orario 10:00:00
                   - Per "tra 2 ore" → ora UTC corrente + 2 ore nel formato YYYY-MM-DD HH:MM:SS
                
                3) START_DATE/END_DATE: Solo se il prompt specifica un periodo di validità
                   - Per "dal 1 gennaio al 31 marzo" → start_date="2025-01-01 00:00:00", end_date="2025-03-31 23:59:59"
                
                ESEMPI DI PARSING CORRETTO CON RIFERIMENTO UTC:
                - "notificami ogni mercoledì alle 14:39" → CRON=true, cron_expression="39 14 * * 3"
                - "buttare la pasta domani all'una di pomeriggio" → SPECIFIC=true, date_time="[DATA_UTC_CORRENTE+1_GIORNO] 13:00:00"
                - "controllare se piove ogni giorno alle 8" → CRON=true, CHECK=true, cron_expression="0 8 * * *"
                - "ricordami di chiamare il 15 febbraio alle 10:30" → SPECIFIC=true, date_time="2025-02-15 10:30:00"
                - "notificami domani mattina" → SPECIFIC=true, date_time="[DATA_UTC_CORRENTE+1_GIORNO] 10:00:00" (DEVI inventare 10:00)
                - "avvisami stasera" → SPECIFIC=true, date_time="[DATA_UTC_CORRENTE] 20:00:00" (DEVI inventare 20:00)
                - "dimmi quando piove" → CHECK=true, CRON=true, cron_expression="0 10 * * *" (NON inventare orario specifico)
                - "tra 30 minuti ricordami di chiamare" → SPECIFIC=true, date_time="[ORA_UTC_CORRENTE+30_MINUTI]"
                - "notificami il 21 gennaio sulle notizie" → SPECIFIC=true, date_time="2025-01-21 00:00:00" (PROSSIMO 21 gennaio)
                      
                IMPORTANTE: Rispondi SEMPRE E SOLO con un JSON valido nel formato specificato. Non aggiungere testo prima o dopo il JSON.
                
                Format your response as a structured notification plan, ecco il template che dovrai usare:
                
                {
                   "response_type": "notification_prompt_template",
                   "timestamp": "2025-06-19T10:45:01Z",
                   "generated_by": "system",
                   "when_notify":{
                        "type":{
                            "CRON":false,
                            "SPECIFIC":true,
                            "CHECK":false
                        },
                        "cron_expression":null,
                        "date_time":"2025-01-21 00:00:00",
                        "start_date":null,
                        "end_date":null
                   },
                   "validity": {
                     "out_of_bounds_prompt_length": false,
                     "offensive_language_detected": false,
                     "nasty_instruction_detected": false,
                     "purpose_valid": true,
                     "reasonable_usage": true,
                     "self_enforcing": true,
                     "valid_prompt": true,
                     "invalid_reason": null
                   },
                   "summary": {
                     "text": "L'utente ha richiesto una notifica per: notizie di economia il 21 gennaio",
                     "language": "it",
                     "category": "notification_generation"
                   },
                   "metadata": {
                     "model_version": "gpt-4o",
                     "confidence_score": 0.95,
                     "policy_enforced": true,
                     "tags": ["notifica", "notizie", "economia", "data_specifica"]
                   }
                 }
           
                 SOSTITUISCI i valori nell'esempio con quelli appropriati per il prompt specifico:
                 - {HERE_THE_GENERATED_SUMMARY} con il riassunto del prompt
                 - true|false nei campi validity in base alla valutazione
                 - {TAG_1}, {TAG_2}, {TAG_3} con tag pertinenti
                 - {INVALID_REASON_IF_ANY} con motivo se non valido, altrimenti null
                 - {CHATGPT_MODEL_VERSION} con "gpt-4o"
                 - {CONFIDENCE_SCORE} con valore 0.00-1.00
                 - {LANGUAGE} con "it" o "en" etc.
                 - true|false nei campi CRON, SPECIFIC, CHECK in base al tipo di richiesta
                 - {CRON_ESPRESSION} con espressione cron se CRON=true, altrimenti null
                 - {YYYY-MM-DD HH:MM:SS} con datetime se SPECIFIC=true, altrimenti null
                 
                 CONFIGURAZIONI VALIDE:
                 cron=0, specific=0, check=0 → NOT_VALID
                 cron=1, specific=0, check=0 → Simply recurrent
                 cron=1, specific=1, check=0 → NOT_VALID
                 cron=0, specific=1, check=0 → Simply in a certain date/datetime
                 cron=0, specific=0, check=1 → Check condition (default daily at 10AM)
                 cron=1, specific=0, check=1 → Check condition with specified frequency
                 cron=1, specific=1, check=1 → Check condition at specific date with frequency
                 cron=0, specific=1, check=1 → Check condition at specific date/datetime
                """;
    }

    private Map<String, Object> buildOpenAiRequest(ChatGptRequest request, String currentTimestamp) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // Modello - IMPORTANTE: Verifica che sia supportato
        requestBody.put("model", model);
        
        // Messaggi
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", request.getPolicy());
        
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", String.format(
            "DATA/ORA UTC CORRENTE: %s\n\nPROMPT UTENTE: %s", 
            currentTimestamp, 
            request.getClientPrompt()
        ));
        
        requestBody.put("messages", new Object[]{systemMessage, userMessage});
        
        // Parametri - CORRETTI per evitare errori 400
        requestBody.put("max_tokens", 1500);
        requestBody.put("temperature", 0.1);
        requestBody.put("top_p", 0.9);
        
        // IMPORTANTE: response_format per GPT-4o deve essere un oggetto
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        requestBody.put("response_format", responseFormat);
        
        return requestBody;
    }
}