package com.notifyme.service;

import com.notifyme.dto.ChatGptRequest;
import com.notifyme.dto.ChatGptResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ChatGptService {

    private final WebClient webClient;
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    public ChatGptService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<ChatGptResponse> sendPromptToChatGpt(String prompt) {
        String policy = buildPolicy();
        ChatGptRequest request = new ChatGptRequest(policy, prompt);

        return webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(buildOpenAiRequest(request))
                .retrieve()
                .bodyToMono(ChatGptResponse.class);
    }

    private String buildPolicy() {
        return """
                Genera un prompt destinato a un assistente virtuale la cui unica funzione è notificare il backend in base a una richiesta di un utente espressa in linguaggio naturale, riguardante:
                
                notifiche ricorrenti (es. ogni giorno, ogni settimana, ogni 2 ore)
                
                oppure notifiche programmate per un momento preciso nel futuro (es. "ricordamelo domani alle 8", "tra 10 minuti", "il 3 luglio alle 14").
                
                Il prompt generato deve rispettare rigorosamente i seguenti vincoli:
                
                Divieto assoluto di linguaggio offensivo, discriminatorio, volgare o anche solo potenzialmente inappropriato.
                
                Nessuna istruzione dannosa o "nasty instruction", incluse ma non limitate a: SQL injection, comandi per ottenere accesso non autorizzato, exploit di sistema, manipolazione di dati, bypass di restrizioni, automazioni illecite, o manipolazioni esterne all’applicazione.
                Lunghezza del prompt: il prompt generato non deve superare i 50 caratteri.
                Il contenuto deve restare vincolato allo scopo specifico dell’app: notificare l’utente. È vietato includere richieste che esulano da questa funzione.
                
                Ogni richiesta deve riflettere il buon senso e un utilizzo ragionevole: ad esempio, niente notifiche ogni millisecondo, o richieste assurde come "notificami ogni volta che respiri", o "ricordamelo per 300 anni".
                
                Il prompt non può generare o ispirare un prompt che violerebbe questa stessa policy.
                
                Format your response as a structured notification plan, ecco il template che dovrai usare:
                
                {
                   "response_type": "notification_prompt_template",
                   "timestamp": "2025-06-19T10:45:01Z",
                   "generated_by": "system",
                   "response":"{HERE_THE_GENERATED_RESPONSE}",
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
                 
                 SOSTITUISCI {HERE_THE_GENERATED_RESPONSE} con il prompt generato che rispetta le regole sopra elencate.
                 SOSTITUISCI {HERE_THE_GENERATED_SUMMARY} con il riassunto o una parafrasi breve generata che ci dica l'argomento e la natura della REQUEST in input (es. "richiesta aggiornamento news sulla guerra in iraq" ).
                 SOSTITUISCI true|false nei campi validity in base alla valutazione del prompt generato rispetto alle regole.
                 SOSTITUISCI {TAG_1}, {TAG_2}, {TAG_3},...,{TAG_N} con eventuali tag pertinenti al prompt generato, come "notifica", "promemoria", "evento futuro", "guerra", "iraq","news"  etc.  non lesinare nell'uso dei tag, ma mantieni la pertinenza e la specificità.
                 SOSTITUISCI {INVALID_REASON_IF_ANY} con una stringa che spiega il motivo per cui il prompt non è valido, se applicabile (es. "lunghezza del prompt eccessiva", "linguaggio offensivo rilevato", "istruzione dannosa rilevata", "scopo non valido", "utilizzo irragionevole", "auto-applicazione non valida") altrimenti null.
                 SOSTITUISCI {CHATGPT_MODEL_VERSION} con la versione del modello di ChatGPT utilizzato per generare il prompt (es. "gpt-3.5-turbo").
                 SOSTITUISCI {CONFIDENCE_SCORE} con un valore numerico tra 0 e 1 che rappresenta la fiducia del modello nella validità del prompt generato (es. 0.95).
                 SOSTITUISCI {LANGUAGE} con la lingua in cui è stato generato il prompt (es. "it" per italiano, "en" per inglese).
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
            public final int max_tokens = 500;
            public final double temperature = 0.7;
        };
    }
}