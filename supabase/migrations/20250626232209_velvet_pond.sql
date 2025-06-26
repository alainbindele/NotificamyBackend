-- V1__create_initial_schema.sql
-- Initial database schema for NotifyMe application

-- Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    discord_webhook TEXT,
    slack_webhook TEXT,
    whatsapp_phone VARCHAR(30),
    INDEX idx_users_email (email)
);

-- Queries table with all ChatGPT validation fields
CREATE TABLE queries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    prompt TEXT NOT NULL,
    
    -- Status fields
    closed BOOLEAN DEFAULT FALSE,
    is_valid BOOLEAN DEFAULT FALSE,
    
    -- Type flags from ChatGPT response
    cron BOOLEAN DEFAULT FALSE,
    date_specific BOOLEAN DEFAULT FALSE,
    to_check BOOLEAN DEFAULT FALSE,
    
    -- Scheduling fields
    cron_params VARCHAR(100),
    next_execution TIMESTAMP NULL,
    specific_datetime TIMESTAMP NULL,
    
    -- Validity period
    valid_from TIMESTAMP NULL,
    valid_to TIMESTAMP NULL,
    
    -- Notification channels
    enabled_channels TEXT,
    
    -- ChatGPT validation fields
    out_of_bounds_prompt_length BOOLEAN,
    offensive_language_detected BOOLEAN,
    nasty_instruction_detected BOOLEAN,
    purpose_valid BOOLEAN,
    reasonable_usage BOOLEAN,
    self_enforcing BOOLEAN,
    invalid_reason TEXT,
    
    -- Summary fields
    summary_text TEXT,
    language VARCHAR(10),
    category VARCHAR(50),
    
    -- Metadata fields
    model_version VARCHAR(50),
    confidence_score DECIMAL(3,2),
    policy_enforced BOOLEAN,
    tags TEXT, -- JSON array as string
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_queries_user_id (user_id),
    INDEX idx_queries_is_valid (is_valid),
    INDEX idx_queries_next_execution (next_execution),
    INDEX idx_queries_cron (cron),
    INDEX idx_queries_date_specific (date_specific),
    INDEX idx_queries_to_check (to_check),
    INDEX idx_queries_created_at (created_at)
);

-- Executions table
CREATE TABLE executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_id BIGINT NOT NULL,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('SUCCESS', 'FAILED') DEFAULT 'SUCCESS',
    response TEXT,
    
    FOREIGN KEY (query_id) REFERENCES queries(id) ON DELETE CASCADE,
    INDEX idx_executions_query_id (query_id),
    INDEX idx_executions_executed_at (executed_at)
);

-- Notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    query_id BIGINT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    subject VARCHAR(255),
    content TEXT,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (query_id) REFERENCES queries(id) ON DELETE SET NULL,
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_query_id (query_id),
    INDEX idx_notifications_sent_at (sent_at)
);