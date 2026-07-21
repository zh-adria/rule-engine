CREATE DATABASE IF NOT EXISTS approval_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE approval_db;

CREATE TABLE IF NOT EXISTS re_approval_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  target_type VARCHAR(32) NOT NULL,
  target_id VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  submitted_by VARCHAR(64) NOT NULL,
  reviewed_by VARCHAR(64),
  reason VARCHAR(512),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_approval_record_target (target_type, target_id),
  KEY idx_approval_record_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
