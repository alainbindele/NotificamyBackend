-- =====================================================
-- AGGIUNTA SUPPORTO TIMEZONE
-- =====================================================

-- Aggiunge il campo timezone alla tabella queries
ALTER TABLE queries 
ADD COLUMN timezone VARCHAR(50) AFTER enabled_channels;

-- Aggiunge un indice per il campo timezone per query di performance
CREATE INDEX idx_queries_timezone ON queries(timezone);

-- =====================================================
-- QUERY DI VERIFICA
-- =====================================================

-- Verifica che il campo sia stato aggiunto correttamente
DESCRIBE queries;

-- Verifica che il campo timezone sia presente
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'queries' AND COLUMN_NAME = 'timezone';

-- =====================================================
-- ESEMPI DI UTILIZZO
-- =====================================================

-- Query di esempio per trovare notifiche per timezone
-- SELECT * FROM queries WHERE timezone = 'Asia/Tokyo' AND is_valid = TRUE;

-- Query per statistiche per timezone
-- SELECT timezone, COUNT(*) as query_count 
-- FROM queries 
-- WHERE is_valid = TRUE 
-- GROUP BY timezone 
-- ORDER BY query_count DESC;

-- =====================================================
-- NOTE IMPORTANTI
-- =====================================================

-- Il campo timezone conterrà valori come:
-- - 'Asia/Tokyo' per il Giappone
-- - 'Europe/Rome' per l'Italia  
-- - 'America/New_York' per New York
-- - 'UTC' per UTC
-- - NULL per query create prima di questo aggiornamento

-- Il sistema convertirà automaticamente tutti gli orari da timezone utente a UTC
-- per la memorizzazione nel database, garantendo consistenza temporale.