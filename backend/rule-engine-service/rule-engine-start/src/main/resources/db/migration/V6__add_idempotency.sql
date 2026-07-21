-- V6: Idempotency key persistence table
CREATE TABLE IF NOT EXISTS re_idempotency_key (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    resource_type   VARCHAR(64) NOT NULL,
    resource_id     VARCHAR(128) NOT NULL,
    response_body   TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP NOT NULL,
    INDEX idx_key (idempotency_key),
    INDEX idx_expires (expires_at)
);
