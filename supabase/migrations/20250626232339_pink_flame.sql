-- Query MySQL per aggiornare le tabelle esistenti con i nuovi campi
-- Eseguire queste TQuery in sequenza sul database MySQL esistente

-- =====================================================
-- AGGIORNAMENTO TABELLA QUERIES
-- =====================================================

-- 1. Aggiungere i campi di stato
ALTER TABLE queries 
ADD COLUMN closed BOOLEAN DEFAULT FALSE AFTER prompt;

-- 2. Aggiungere i flag di tipo dalla risposta ChatGPT
ALTER TABLE queries 
ADD COLUMN cron BOOLEAN DEFAULT FALSE AFTER is_valid,
ADD COLUMN date_specific BOOLEAN DEFAULT FALSE AFTER cron,
ADD COLUMN to_check BOOLEAN DEFAULT FALSE AFTER date_specific;

-- 3. Aggiungere i campi di scheduling aggiuntivi
ALTER TABLE queries
ADD COLUMN valid_to TIMESTAMP NULL AFTER valid_from;

-- 4. Aggiungere il campo per i canali di notifica abilitati
ALTER TABLE queries 
ADD COLUMN enabled_channels TEXT AFTER valid_to;

-- 5. Aggiungere i campi di validazione ChatGPT
ALTER TABLE queries 
ADD COLUMN out_of_bounds_prompt_length BOOLEAN AFTER enabled_channels,
ADD COLUMN offensive_language_detected BOOLEAN AFTER out_of_bounds_prompt_length,
ADD COLUMN nasty_instruction_detected BOOLEAN AFTER offensive_language_detected,
ADD COLUMN purpose_valid BOOLEAN AFTER nasty_instruction_detected,
ADD COLUMN reasonable_usage BOOLEAN AFTER purpose_valid,
ADD COLUMN self_enforcing BOOLEAN AFTER reasonable_usage,
ADD COLUMN invalid_reason TEXT AFTER self_enforcing;

-- 6. Aggiungere i campi summary
ALTER TABLE queries 
ADD COLUMN summary_text TEXT AFTER invalid_reason,
ADD COLUMN language VARCHAR(10) AFTER summary_text,
ADD COLUMN category VARCHAR(50) AFTER language;

-- 7. Aggiungere i campi metadata
ALTER TABLE queries 
ADD COLUMN model_version VARCHAR(50) AFTER category,
ADD COLUMN confidence_score DECIMAL(3,2) AFTER model_version,
ADD COLUMN policy_enforced BOOLEAN AFTER confidence_score,
ADD COLUMN tags TEXT AFTER policy_enforced;

-- 8. Modificare la lunghezza del campo cron_params se necessario
ALTER TABLE queries 
MODIFY COLUMN cron_params VARCHAR(100);

-- =====================================================
-- AGGIUNGERE INDICI PER PERFORMANCE
-- =====================================================

-- Indici per i nuovi campi di tipo
CREATE INDEX idx_queries_cron ON queries(cron);
CREATE INDEX idx_queries_date_specific ON queries(date_specific);
CREATE INDEX idx_queries_to_check ON queries(to_check);

-- Indici per i campi di validità temporale
CREATE INDEX idx_queries_valid_from ON queries(valid_from);
CREATE INDEX idx_queries_valid_to ON queries(valid_to);

-- Indice per il campo closed
CREATE INDEX idx_queries_closed ON queries(closed);

-- Indice composto per TQuery attive
CREATE INDEX idx_queries_active ON queries(is_valid, closed);

-- Indice composto per TQuery pronte all'esecuzione
CREATE INDEX idx_queries_execution_ready ON queries(is_valid, next_execution, closed);

-- =====================================================
-- QUERY DI VERIFICA
-- =====================================================

-- Verifica che tutti i campi siano stati aggiunti correttamente
DESCRIBE queries;

-- Verifica gli indici creati
SHOW INDEX FROM queries;

-- Conta le TQuery esistenti per verificare che i dati non siano stati persi
SELECT COUNT(*) as total_queries FROM queries;

-- Verifica che i nuovi campi siano NULL per le TQuery esistenti (comportamento atteso)
SELECT 
    COUNT(*) as total,
    COUNT(cron) as cron_not_null,
    COUNT(date_specific) as date_specific_not_null,
    COUNT(to_check) as to_check_not_null,
    COUNT(summary_text) as summary_not_null
FROM queries;

-- =====================================================
-- QUERY OPZIONALI DI PULIZIA/MIGRAZIONE DATI
-- =====================================================

-- Se vuoi impostare valori di default per le TQuery esistenti:

-- Imposta closed = FALSE per tutte le TQuery esistenti (se non già impostato)
UPDATE queries 
SET closed = FALSE 
WHERE closed IS NULL;

-- Imposta i flag di tipo a FALSE per le TQuery esistenti (se non già impostato)
UPDATE queries 
SET 
    cron = FALSE,
    date_specific = FALSE,
    to_check = FALSE
WHERE cron IS NULL OR date_specific IS NULL OR to_check IS NULL;

-- =====================================================
-- QUERY DI TEST PER VERIFICARE IL FUNZIONAMENTO
-- =====================================================

-- Test per trovare TQuery per tipo (dovrebbe funzionare dopo l'aggiornamento)
SELECT id, prompt, cron, date_specific, to_check 
FROM queries 
WHERE cron = TRUE 
LIMIT 5;

-- Test per trovare TQuery attive
SELECT id, prompt, is_valid, closed 
FROM queries 
WHERE is_valid = TRUE AND (closed = FALSE OR closed IS NULL)
LIMIT 5;

-- Test per trovare TQuery con periodo di validità
SELECT id, prompt, valid_from, valid_to, next_execution
FROM queries 
WHERE valid_from IS NOT NULL OR valid_to IS NOT NULL
LIMIT 5;

-- =====================================================
-- BACKUP RACCOMANDATO PRIMA DELL'ESECUZIONE
-- =====================================================

-- IMPORTANTE: Prima di eseguire queste TQuery, crea un backup del database:
-- mysqldump -u username -p database_name > backup_before_update.sql

-- =====================================================
-- ROLLBACK (se necessario)
-- =====================================================

-- Se qualcosa va storto, puoi rimuovere i campi aggiunti con:
/*
ALTER TABLE queries 
DROP COLUMN closed,
DROP COLUMN cron,
DROP COLUMN date_specific,
DROP COLUMN to_check,
DROP COLUMN valid_from,
DROP COLUMN valid_to,
DROP COLUMN enabled_channels,
DROP COLUMN out_of_bounds_prompt_length,
DROP COLUMN offensive_language_detected,
DROP COLUMN nasty_instruction_detected,
DROP COLUMN purpose_valid,
DROP COLUMN reasonable_usage,
DROP COLUMN self_enforcing,
DROP COLUMN invalid_reason,
DROP COLUMN summary_text,
DROP COLUMN language,
DROP COLUMN category,
DROP COLUMN model_version,
DROP COLUMN confidence_score,
DROP COLUMN policy_enforced,
DROP COLUMN tags;

-- E rimuovere gli indici:
DROP INDEX idx_queries_cron ON queries;
DROP INDEX idx_queries_date_specific ON queries;
DROP INDEX idx_queries_to_check ON queries;
DROP INDEX idx_queries_valid_from ON queries;
DROP INDEX idx_queries_valid_to ON queries;
DROP INDEX idx_queries_closed ON queries;
DROP INDEX idx_queries_active ON queries;
DROP INDEX idx_queries_execution_ready ON queries;
*/