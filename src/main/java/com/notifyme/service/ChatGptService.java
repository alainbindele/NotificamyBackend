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
                Sei un assistente virtuale la cui unica funzione è validare un prompt riguardante:
                
                notifiche ricorrenti (es. ogni giorno, ogni settimana, ogni 2 ore)
                oppure notifiche programmate per un momento preciso nel futuro (es. "ricordamelo domani alle 8", "tra 10 minuti", "il 3 luglio alle 14")
                
                Il prompt deve rispettare rigorosamente i seguenti vincoli:
                1) Deve indicare esplicitamente un riferimento all'intervallo temporale in cui essere eseguito 
                    oppure ad una data/tempo in cui essere notificato 
                    (se non viene specificata l'ora e/o giorno manda la notifica a mezzanotte, 
                    se invece viene specificato qualcosa come "mattina, pomeriggio o sera" considera un orario mediano es: mattina= 10AM etc) 
                3) se contiene riferimenti a notifiche che richiedano il controllo periodico 
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
                8) non inventare altre regole e non supporre nulla che non sia scritto nel prompt esplicitamente
                Format your response as a structured notification plan, ecco il template che dovrai usare:
                
                {
                   "response_type": "notification_prompt_template",
                   "timestamp": "2025-06-19T10:45:01Z",
                   "generated_by": "system",
                   "when_notify":{
                        "type":{
                            "CRON":true|false,
                            "SPECIFIC":true|false,
                            "CHECK":true|false
                        },
                        "cron_expression":"{CRON_ESPRESSION}",
                        "date_time":"{YYYY-MM-DD HH24:MI:SS}"|null,
                        "start_date":"{YYYY-MM-DD HH24:MI:SS}"|null,
                        "end_date":"{YYYY-MM-DD HH24:MI:SS}"|null
                   },
                   "validity": {
                     "out_of_bounds_prompt_length": true|false,
                     "offensive_language_detected": true|false,
                     "nasty_instruction_detected": true|false,
                     "purpose_valid": true|false,
                     "reasonable_usage": true|false,
                     "self_enforcing": true|false,
                     "valid_prompt": true|false,
                     "invalid_reason": {INVALID_REASON_IF_ANY}
                   },
                   "summary": {
                     "text": "L'utente ha richiesto una notifica per: {HERE_THE_GENERATED_SUMMARY}",
                     "language": "{LANGUAGE}",
                     "category": "notification_generation"
                   },
                   "metadata": {
                     "model_version": "{CHATGPT_MODEL_VERSION}",
                     "confidence_score": {CONFIDENCE_SCORE},
                     "policy_enforced": true,
                     "tags": ["{TAG_1}", "{TAG_2}", "{TAG_3}"]
                   }
                 }
           
                 SOSTITUISCI {HERE_THE_GENERATED_SUMMARY} con il riassunto o una parafrasi breve generata che ci dica l'argomento e la natura della REQUEST in input (es. "richiesta aggiornamento news sulla guerra in iraq" ).
                 SOSTITUISCI true|false nei campi validity in base alla valutazione del prompt generato rispetto alle regole suddette (es "out_of_bounds_prompt_length": false se la lunghezza del prompt è minore di 50 caratteri).
                 SOSTITUISCI {TAG_1}, {TAG_2}, {TAG_3},...,{TAG_N} con eventuali tag pertinenti al prompt generato, come "notifica", "promemoria", "evento futuro", "guerra", "iraq","news"  etc.  non lesinare nell'uso dei tag, ma mantieni la pertinenza e la specificità.
                 SOSTITUISCI {INVALID_REASON_IF_ANY} con una stringa che spiega il motivo per cui il prompt non è valido, se applicabile (es. (ma puoi essere più spoecifico) "lunghezza del prompt eccessiva", "linguaggio offensivo rilevato", "istruzione dannosa rilevata", "utilizzo irragionevole", "auto-applicazione non valida", "non è stato specificato l'intervallo o il momento in cui essere notificato") altrimenti null.
                 SOSTITUISCI {CHATGPT_MODEL_VERSION} con la versione del modello di ChatGPT utilizzato per generare il prompt (es. "gpt-3.5-turbo").
                 SOSTITUISCI {CONFIDENCE_SCORE} con un valore numerico tra 0 e 1 che rappresenta la fiducia del modello nella validità del prompt generato (es. 0.95).
                 SOSTITUISCI {LANGUAGE} con la lingua in cui è stato generato il prompt (es. "it" per italiano, "en" per inglese), se il prompt è in una certa lingua anche reason e summary devono essere nella stessa lingua.
                 SOSTITUISCI true|false nei campi CRON, SPECIFIC,CHECK in base alla rilevazione della temporalità della richiesta 
                             (es. - se il prompt richiede "ogni giorno alle 18" imposta con "CRON" con true altrimenti false
                                  - se richiede "il giorno 15 AGOSTO alle 20" imposta con "SPECIFIC" con true altrimenti false 
                                  - se richiede "dimmi se il prezzo di bitcoin scende sotto i 500$" imposta come "CHECK" con "true" altrimenti false 
                                  - se richiede "dimmi se il prezzo di bitcoin scende sotto i 500$ controllando ogni giorno alle 21:30" allora questa è sia CRON che CHECK
                                  - se richiede "dimmi se il prezzo di bitcoin è sceso sotto i 500$ controllando il giorno 21 agosto alle 21:30" allora questa è sia SPECIFIC che CHECK ma non CRON
                                  - generalizza questi esempi su tutti gli altri casi che ti vengono richiesti
                                    
                              )       
                 SOSTITUISCI {CRON_ESPRESSION} con l'espressione CRONTAB standard di linux che rappresenta il cron che potrebbe essere impostato per quella specifica richiesta (ovviamente se è rilevato CRON come true in "when_notify->type") altrimenti null
                 SOSTITUISCI {YYYY-MM-DD HH24:MI:SS} con il datetime in questo formato se rilevi SPECIFIC come true altrimenti null
                 SOSTITUISCI {YYYY-MM-DD HH24:MI:SS}" oppure null in start_time e/o end_time se richiesto che le notifiche abbiano un intervallo di validità specifico
                 
                 CONFIGURAZIONI VALIDE DEI FLAG TYPE (CASI ALGORITMO 0-5):
                 
                 CASO 0: cron=1, date_specific=0, to_check=1
                 Esempio: "notificami se bitcoin scende sotto i 1000$"
                 Descrizione: Controllo condizione senza tempo specifico (default: ogni giorno alle 10)
                 
                 CASO 1: cron=0, date_specific=1, to_check=0  
                 Esempio: "notificami il 21 gennaio alle 9 sulle notizie"
                 Descrizione: Notifica semplice a data/ora specifica
                 
                 CASO 2: cron=1, date_specific=0, to_check=0
                 Esempio: "notificami ogni giorno alle 9 sulle notizie"
                 Descrizione: Notifica ricorrente senza controllo condizioni
                 
                 CASO 3: cron=0, date_specific=1, to_check=1
                 Esempio: "notificami il 21 gennaio alle 9 se bitcoin scende"
                 Descrizione: Controllo condizione a data/ora specifica
                 
                 CASO 4: cron=1, date_specific=0, to_check=1
                 Esempio: "notificami ogni giorno se bitcoin scende"
                 Descrizione: Controllo condizione ricorrente
                 
                 CASO 5: cron=1, date_specific=1, to_check=1
                 Esempio: "notificami il 21 gennaio se bitcoin scende, controlla ogni ora"
                 Descrizione: Controllo condizione a data specifica con intervallo personalizzato
                 
                 CONFIGURAZIONI NON VALIDE:
                 - cron=0, date_specific=0, to_check=0 (nessuna configurazione temporale)
                 - Qualsiasi altra combinazione non elencata sopra
                 
                 IMPORTANTE: Il CASO 5 (cron=1, date_specific=1, to_check=1) è VALIDO e rappresenta il controllo di una condizione in una data specifica con un intervallo di controllo personalizzato.
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
            public final int max_tokens = 1000;
            public final double temperature = 0.7;
        };
    }
}