-- Webhook 配置表
CREATE TABLE IF NOT EXISTS re_webhook_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  webhook_url VARCHAR(512) NOT NULL,
  event_types VARCHAR(512) NOT NULL COMMENT 'JSON数组，订阅的事件类型',
  secret VARCHAR(128) COMMENT '签名密钥，用于验证回调来源',
  description VARCHAR(256),
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_webhook_url (webhook_url),
  KEY idx_webhook_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Webhook 调用日志表
CREATE TABLE IF NOT EXISTS re_webhook_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  webhook_id BIGINT NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  rule_code VARCHAR(64),
  request_url VARCHAR(512) NOT NULL,
  request_body TEXT,
  response_status INT,
  response_body TEXT,
  success TINYINT(1) NOT NULL DEFAULT 0,
  error_message VARCHAR(512),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_webhook_log_webhook (webhook_id),
  KEY idx_webhook_log_created (created_at),
  CONSTRAINT fk_webhook_log_webhook FOREIGN KEY (webhook_id) REFERENCES re_webhook_config(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
