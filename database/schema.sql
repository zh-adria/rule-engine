CREATE DATABASE IF NOT EXISTS rule_engine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rule_engine;

-- ==================== 规则核心表 ====================

CREATE TABLE IF NOT EXISTS re_rule_definition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  category VARCHAR(32) NOT NULL,
  business_line VARCHAR(64) NOT NULL,
  description VARCHAR(512),
  sensitive TINYINT(1) NOT NULL DEFAULT 0,
  owner VARCHAR(64) NOT NULL,
  current_version INT,
  gray_version INT,
  gray_percent INT DEFAULT 0,
  regulatory_ref VARCHAR(128),
  archived TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rule_definition_code (rule_code),
  KEY idx_rule_definition_code (rule_code),
  KEY idx_rule_definition_category (category),
  KEY idx_rule_definition_archived (archived)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS re_rule_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(64) NOT NULL,
  version INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  drl_content LONGTEXT NOT NULL,
  visual_model LONGTEXT,
  checksum VARCHAR(64) NOT NULL,
  effective_from DATETIME,
  effective_to DATETIME,
  created_by VARCHAR(64) NOT NULL,
  approved_by VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at DATETIME,
  UNIQUE KEY uk_rule_version (rule_code, version),
  KEY idx_rule_version_status (status),
  KEY idx_rule_version_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS re_rule_execution_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trace_id VARCHAR(64) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  version INT NOT NULL,
  scenario VARCHAR(64) NOT NULL,
  decision VARCHAR(64) NOT NULL,
  hit_rules VARCHAR(1024),
  elapsed_ms BIGINT NOT NULL,
  request_snapshot LONGTEXT,
  response_snapshot LONGTEXT,
  operator VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_rule_exec_trace (trace_id),
  KEY idx_rule_exec_rule_code (rule_code),
  KEY idx_rule_exec_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS re_rule_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(64) NOT NULL,
  version INT,
  action VARCHAR(64) NOT NULL,
  operator VARCHAR(64) NOT NULL,
  reason VARCHAR(512),
  ip_address VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_rule_audit_rule_code (rule_code),
  KEY idx_rule_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 规则编排表 ====================

CREATE TABLE IF NOT EXISTS re_rule_set (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  set_code VARCHAR(64) NOT NULL,
  set_name VARCHAR(128) NOT NULL,
  description VARCHAR(512),
  owner VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rule_set_code (set_code),
  KEY idx_rule_set_code (set_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS re_rule_set_step (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  set_id BIGINT NOT NULL,
  step_order INT NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_version INT,
  mode VARCHAR(16) NOT NULL DEFAULT 'SERIAL',
  stop_on_decline TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_rule_set_step_set_id (set_id),
  KEY idx_rule_set_step_rule_code (rule_code),
  CONSTRAINT fk_rule_set_step_set_id FOREIGN KEY (set_id) REFERENCES re_rule_set(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 规则关联表（仅记录规则与外部系统的关联关系） ====================

CREATE TABLE IF NOT EXISTS re_rule_product_binding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(64) NOT NULL,
  product_code VARCHAR(64) NOT NULL COMMENT '外部系统的产品编码',
  rule_type VARCHAR(32) NOT NULL COMMENT 'UNDERWRITING, PRICING, CLAIMS',
  priority INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rule_product (rule_code, product_code),
  KEY idx_rule_product_rule (rule_code),
  KEY idx_rule_product_product (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== Webhook 配置表 ====================

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
