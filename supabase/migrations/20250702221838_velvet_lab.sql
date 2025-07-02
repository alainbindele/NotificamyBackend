-- =====================================================
-- SCHEMA DATABASE NOTIFYME
-- =====================================================

-- Tabella Users: gestisce gli utenti del sistema
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Canali di notifica configurabili
    discord_webhook TEXT,
    slack_webhook TEXT,
    whatsapp_phone VARCHAR(30),
    
    -- Indici per performance
    INDEX idx_users_email (email)
);

-- Tabella Queries: gestisce le configurazioni delle notifiche
CREATE TABLE queries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    prompt TEXT NOT NULL,
    
    -- =====================================================
    -- CAMPI DI STATO E CONTROLLO
    -- =====================================================
    closed BOOLEAN DEFAULT FALSE,
    is_valid BOOLEAN DEFAULT FALSE,
    
    -- =====================================================
    -- FLAG DI TIPO (CASI 0-5 DELL'ALGORITMO)
    -- =====================================================
    cron BOOLEAN DEFAULT FALSE,           -- Query ricorrente (ogni X tempo)
    date_specific BOOLEAN DEFAULT FALSE,  -- Query per data/ora specifica
    to_check BOOLEAN DEFAULT FALSE,       -- Query che controlla condizioni esterne
    
    -- =====================================================
    -- CAMPI DI SCHEDULING
    -- =====================================================
    cron_params VARCHAR(100),             -- Espressione cron (es: "0 9 * * *")
    next_execution TIMESTAMP NULL,        -- Prossima esecuzione programmata
    
    -- =====================================================
    -- PERIODO DI VALIDITÀ
    -- =====================================================
    valid_from TIMESTAMP NULL,            -- Inizio validità
    valid_to TIMESTAMP NULL,              -- Fine validità
    
    -- =====================================================
    -- CONFIGURAZIONE CANALI
    -- =====================================================
    enabled_channels TEXT,                -- JSON array dei canali abilitati
    
    -- =====================================================
    -- CAMPI DI VALIDAZIONE CHATGPT
    -- =====================================================
    out_of_bounds_prompt_length BOOLEAN,
    offensive_language_detected BOOLEAN,
    nasty_instruction_detected BOOLEAN,
    purpose_valid BOOLEAN,
    reasonable_usage BOOLEAN,
    self_enforcing BOOLEAN,
    invalid_reason TEXT,
    
    -- =====================================================
    -- CAMPI SUMMARY
    -- =====================================================
    summary_text TEXT,                    -- Riassunto generato da ChatGPT
    language VARCHAR(10),                 -- Lingua rilevata (it, en, etc.)
    category VARCHAR(50),                 -- Categoria della notifica
    
    -- =====================================================
    -- CAMPI METADATA
    -- =====================================================
    model_version VARCHAR(50),            -- Versione modello ChatGPT usato
    confidence_score DECIMAL(3,2),        -- Score di confidenza (0.00-1.00)
    policy_enforced BOOLEAN,              -- Se le policy sono state applicate
    tags TEXT,                            -- JSON array di tag
    
    -- =====================================================
    -- TIMESTAMP
    -- =====================================================
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- =====================================================
    -- FOREIGN KEYS E INDICI
    -- =====================================================
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Indici per performance
    INDEX idx_queries_user_id (user_id),
    INDEX idx_queries_is_valid (is_valid),
    INDEX idx_queries_next_execution (next_execution),
    INDEX idx_queries_cron (cron),
    INDEX idx_queries_date_specific (date_specific),
    INDEX idx_queries_to_check (to_check),
    INDEX idx_queries_created_at (created_at),
    INDEX idx_queries_valid_from (valid_from),
    INDEX idx_queries_valid_to (valid_to),
    INDEX idx_queries_closed (closed),
    
    -- Indici composti per query complesse
    INDEX idx_queries_active (is_valid, closed),
    INDEX idx_queries_execution_ready (is_valid, next_execution, closed)
);

-- Tabella Executions: storico delle esecuzioni
CREATE TABLE executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_id BIGINT NOT NULL,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('SUCCESS', 'FAILED') DEFAULT 'SUCCESS',
    response TEXT,                        -- Risultato dell'esecuzione
    
    FOREIGN KEY (query_id) REFERENCES queries(id) ON DELETE CASCADE,
    INDEX idx_executions_query_id (query_id),
    INDEX idx_executions_executed_at (executed_at)
);

-- Tabella Notifications: storico delle notifiche inviate
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    query_id BIGINT,                      -- Può essere NULL se notifica manuale
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    subject VARCHAR(255),
    content TEXT,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (query_id) REFERENCES queries(id) ON DELETE SET NULL,
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_query_id (query_id),
    INDEX idx_notifications_sent_at (sent_at)
);

-- =====================================================
-- ESEMPI DI CONFIGURAZIONI PER I CASI 0-5
-- =====================================================

-- CASO 0: "notificami se bitcoin scende sotto i 1000$"
-- cron=1, date_specific=0, to_check=1
-- cron_params="0 10 * * *" (ogni giorno alle 10)
-- next_execution=prossime 10:00

-- CASO 1: "notificami il 21 gennaio alle 9 sulle notizie"
-- cron=0, date_specific=1, to_check=0
-- next_execution=2025-01-21 09:00:00
-- Dopo l'esecuzione: closed=1

-- CASO 2: "notificami ogni giorno alle 9 sulle notizie"
-- cron=1, date_specific=0, to_check=0
-- cron_params="0 9 * * *"
-- next_execution=prossime 09:00

-- CASO 3: "notificami il 21 gennaio alle 9 se bitcoin scende"
-- cron=0, date_specific=1, to_check=1
-- next_execution=2025-01-21 09:00:00

-- CASO 4: "notificami ogni giorno se bitcoin scende"
-- cron=1, date_specific=0, to_check=1
-- cron_params="0 10 * * *"

-- CASO 5: "notificami il 21 gennaio se bitcoin scende, controlla ogni ora"
-- cron=1, date_specific=1, to_check=1
-- next_execution=2025-01-21 (data specifica)
-- cron_params="0 * * * *" (controlla ogni ora)

-- =====================================================
-- QUERY UTILI PER IL SISTEMA
-- =====================================================

-- Trova query pronte per l'esecuzione
SELECT * FROM queries 
WHERE is_valid = TRUE 
  AND next_execution <= NOW() 
  AND (closed = FALSE OR closed IS NULL)
  AND (valid_from IS NULL OR valid_from <= NOW())
  AND (valid_to IS NULL OR valid_to >= NOW());

-- Trova query attive per utente
SELECT * FROM queries 
WHERE user_id = ? 
  AND is_valid = TRUE 
  AND (closed = FALSE OR closed IS NULL);

-- Statistiche per utente
SELECT 
    COUNT(*) as total_queries,
    SUM(CASE WHEN cron = TRUE THEN 1 ELSE 0 END) as cron_queries,
    SUM(CASE WHEN date_specific = TRUE THEN 1 ELSE 0 END) as specific_queries,
    SUM(CASE WHEN to_check = TRUE THEN 1 ELSE 0 END) as check_queries
FROM queries 
WHERE user_id = ? AND is_valid = TRUE;

-- Chiudi query scadute
UPDATE queries 
SET closed = TRUE, next_execution = NULL 
WHERE valid_to IS NOT NULL 
  AND valid_to < NOW() 
  AND (closed = FALSE OR closed IS NULL);