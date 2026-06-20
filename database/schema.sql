CREATE DATABASE IF NOT EXISTS rule_engine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rule_engine;

CREATE TABLE IF NOT EXISTS rule_definition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  category VARCHAR(32) NOT NULL,
  business_line VARCHAR(64) NOT NULL,
  description VARCHAR(512),
  sensitive TINYINT(1) NOT NULL DEFAULT 0,
  archived TINYINT(1) NOT NULL DEFAULT 0,
  owner VARCHAR(64) NOT NULL,
  current_version INT,
  gray_version INT,
  gray_percent INT DEFAULT 0,
  regulatory_ref VARCHAR(128),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rule_definition_code (rule_code),
  KEY idx_rule_definition_code (rule_code),
  KEY idx_rule_definition_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rule_version (
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

CREATE TABLE IF NOT EXISTS rule_execution_log (
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

CREATE TABLE IF NOT EXISTS rule_audit_log (
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

