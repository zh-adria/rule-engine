-- V8: rule test cases, suites, suite membership and run summaries
CREATE TABLE IF NOT EXISTS re_rule_test_case (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  case_code VARCHAR(100) NOT NULL,
  case_name VARCHAR(200) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  version INT,
  scenario VARCHAR(64),
  facts_json CLOB NOT NULL,
  expected_decision VARCHAR(32),
  expected_hit_rules_json CLOB,
  expected_outputs_json CLOB,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(100),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rule_test_case_code (case_code),
  KEY idx_rule_test_case_rule (rule_code),
  KEY idx_rule_test_case_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS re_rule_test_suite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  suite_code VARCHAR(100) NOT NULL,
  suite_name VARCHAR(200) NOT NULL,
  rule_code VARCHAR(64),
  business_line VARCHAR(64),
  description CLOB,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(100),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rule_test_suite_code (suite_code),
  KEY idx_rule_test_suite_rule (rule_code),
  KEY idx_rule_test_suite_biz (business_line)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS re_rule_test_suite_case (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  suite_code VARCHAR(100) NOT NULL,
  case_code VARCHAR(100) NOT NULL,
  case_order INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_rule_test_suite_case (suite_code, case_code),
  KEY idx_rule_test_suite_case_suite (suite_code),
  KEY idx_rule_test_suite_case_case (case_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS re_rule_test_run (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id VARCHAR(100) NOT NULL,
  suite_code VARCHAR(100),
  case_code VARCHAR(100),
  rule_code VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  total_cases INT NOT NULL DEFAULT 0,
  passed_cases INT NOT NULL DEFAULT 0,
  failed_cases INT NOT NULL DEFAULT 0,
  result_json CLOB,
  executed_by VARCHAR(100),
  started_at DATETIME,
  finished_at DATETIME,
  UNIQUE KEY uk_rule_test_run_id (run_id),
  KEY idx_rule_test_run_rule (rule_code),
  KEY idx_rule_test_run_suite (suite_code),
  KEY idx_rule_test_run_case (case_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
