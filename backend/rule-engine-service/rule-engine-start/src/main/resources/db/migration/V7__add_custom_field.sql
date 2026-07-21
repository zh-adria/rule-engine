-- V7: re_custom_field table (P4)
CREATE TABLE IF NOT EXISTS re_custom_field (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  field_code VARCHAR(64) NOT NULL,
  field_label VARCHAR(128) NOT NULL,
  field_type VARCHAR(16) NOT NULL,
  business_line VARCHAR(64),
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_custom_field_code (field_code),
  KEY idx_custom_field_biz (business_line)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
