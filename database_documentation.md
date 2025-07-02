# 📊 Documentazione Schema Database NotifyMe

## 🏗️ **Struttura Generale**

Il database è composto da **4 tabelle principali** che gestiscono il ciclo completo delle notifiche:

### 1. **`users`** - Gestione Utenti
```sql
- id: Chiave primaria
- email: Email univoca dell'utente  
- discord_webhook: URL webhook Discord (opzionale)
- slack_webhook: URL webhook Slack (opzionale)
- whatsapp_phone: Numero telefono WhatsApp (opzionale)
- created_at: Timestamp creazione
```

### 2. **`queries`** - Configurazioni Notifiche ⭐
```sql
-- CAMPI DI STATO
- id, user_id, prompt, created_at
- closed: Se la query è stata chiusa
- is_valid: Se la query è valida (validata da ChatGPT)

-- FLAG ALGORITMO (CASI 0-5)
- cron: Query ricorrente (TRUE/FALSE)
- date_specific: Query per data specifica (TRUE/FALSE) 
- to_check: Query che controlla condizioni (TRUE/FALSE)

-- SCHEDULING
- cron_params: Espressione cron (es: "0 9 * * *")
- next_execution: Prossima esecuzione programmata
- valid_from/valid_to: Periodo di validità

-- VALIDAZIONE CHATGPT
- out_of_bounds_prompt_length, offensive_language_detected, etc.
- invalid_reason: Motivo se non valida
- summary_text: Riassunto generato
- confidence_score: Score di confidenza (0.00-1.00)
```

### 3. **`executions`** - Storico Esecuzioni
```sql
- id, query_id, executed_at
- status: SUCCESS/FAILED
- response: Risultato dell'esecuzione
```

### 4. **`notifications`** - Storico Notifiche Inviate
```sql
- id, user_id, query_id, sent_at
- subject, content: Contenuto della notifica
```

## 🎯 **Mapping Casi Algoritmo → Database**

| Caso | Descrizione | cron | specific | check | Esempio |
|------|-------------|------|----------|-------|---------|
| **0** | `notificami [se_evento]` | ✅ | ❌ | ✅ | "se bitcoin scende" |
| **1** | `notificami il [data] [cosa]` | ❌ | ✅ | ❌ | "il 21 gennaio alle 9" |
| **2** | `notificami ogni [intervallo] [cosa]` | ✅ | ❌ | ❌ | "ogni giorno alle 9" |
| **3** | `notificami il [data] [se_evento]` | ❌ | ✅ | ✅ | "il 21 gennaio se bitcoin scende" |
| **4** | `notificami ogni [intervallo] [se_evento]` | ✅ | ❌ | ✅ | "ogni giorno se bitcoin scende" |
| **5** | `notificami il [data] [se_evento] ogni [check]` | ✅ | ✅ | ✅ | "il 21 gennaio se bitcoin scende, controlla ogni ora" |

## 🔄 **Flusso di Validazione Preliminare**

```sql
-- 1. Se solo valid_from e siamo prima
IF valid_from IS NOT NULL AND valid_to IS NULL AND NOW() < valid_from THEN
    SET next_execution = valid_from;

-- 2. Se solo valid_to e siamo dopo  
IF valid_from IS NULL AND valid_to IS NOT NULL AND NOW() > valid_to THEN
    SET closed = TRUE;

-- 3. Se entrambi presenti
IF valid_from IS NOT NULL AND valid_to IS NOT NULL THEN
    -- Controlla intervallo minimo 1 ora
    IF valid_from + INTERVAL 1 HOUR > valid_to THEN
        SET is_valid = FALSE, invalid_reason = "Intervallo < 1 ora";
    -- Se siamo prima di valid_from
    ELSEIF NOW() < valid_from THEN
        SET next_execution = valid_from;
    -- Se siamo dopo valid_to
    ELSEIF NOW() > valid_to THEN
        SET closed = TRUE;
    END IF;
```

## 📈 **Indici per Performance**

### Indici Semplici:
- `idx_queries_user_id` - Query per utente
- `idx_queries_next_execution` - Scheduler
- `idx_queries_cron/specific/check` - Filtri per tipo

### Indici Composti:
- `idx_queries_active (is_valid, closed)` - Query attive
- `idx_queries_execution_ready (is_valid, next_execution, closed)` - Scheduler

## 🚀 **Query Principali del Sistema**

### 1. **Trova Query Pronte per Esecuzione** (Scheduler)
```sql
SELECT * FROM queries 
WHERE is_valid = TRUE 
  AND next_execution <= NOW() 
  AND (closed = FALSE OR closed IS NULL)
  AND (valid_from IS NULL OR valid_from <= NOW())
  AND (valid_to IS NULL OR valid_to >= NOW());
```

### 2. **Query Attive per Utente**
```sql
SELECT * FROM queries 
WHERE user_id = ? 
  AND is_valid = TRUE 
  AND (closed = FALSE OR closed IS NULL);
```

### 3. **Statistiche Utente**
```sql
SELECT 
    COUNT(*) as total_queries,
    SUM(CASE WHEN cron = TRUE THEN 1 ELSE 0 END) as cron_queries,
    SUM(CASE WHEN date_specific = TRUE THEN 1 ELSE 0 END) as specific_queries,
    SUM(CASE WHEN to_check = TRUE THEN 1 ELSE 0 END) as check_queries
FROM queries 
WHERE user_id = ? AND is_valid = TRUE;
```

### 4. **Chiusura Query Scadute**
```sql
UPDATE queries 
SET closed = TRUE, next_execution = NULL 
WHERE valid_to IS NOT NULL 
  AND valid_to < NOW() 
  AND (closed = FALSE OR closed IS NULL);
```

## 🔧 **Esempi di Configurazioni**

### Caso 2: "notificami ogni giorno alle 9 sulle notizie"
```sql
INSERT INTO queries (user_id, prompt, is_valid, cron, date_specific, to_check, 
                    cron_params, next_execution, valid_from) 
VALUES (1, 'notificami ogni giorno alle 9 sulle notizie', TRUE, TRUE, FALSE, FALSE,
        '0 9 * * *', '2025-01-20 09:00:00', '2025-01-20 09:00:00');
```

### Caso 5: "notificami il 21 gennaio se bitcoin scende, controlla ogni ora"
```sql
INSERT INTO queries (user_id, prompt, is_valid, cron, date_specific, to_check,
                    cron_params, next_execution)
VALUES (1, 'notificami il 21 gennaio se bitcoin scende, controlla ogni ora', 
        TRUE, TRUE, TRUE, TRUE, '0 * * * *', '2025-01-21 00:00:00');
```

## 🛡️ **Sicurezza e Validazione**

- **Validazione ChatGPT**: Tutti i prompt vengono validati per sicurezza
- **SQL Injection Protection**: Implementata nel SecurityService
- **Webhook Validation**: URL webhook validati con regex specifiche
- **Rate Limiting**: Controllo tramite cron_params per evitare spam

## 📊 **Monitoraggio e Analytics**

- **Executions**: Traccia ogni esecuzione (successo/fallimento)
- **Notifications**: Storico completo delle notifiche inviate
- **Confidence Score**: Metrica di qualità delle validazioni ChatGPT
- **Tags**: Sistema di categorizzazione per analytics avanzate