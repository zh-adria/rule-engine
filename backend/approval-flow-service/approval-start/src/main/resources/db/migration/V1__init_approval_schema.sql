-- 审批记录表
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
  current_level INT DEFAULT 1,
  max_level INT DEFAULT 1,
  level_approved_by VARCHAR(64),
  level_approved_at DATETIME,
  approval_chain LONGTEXT,
  KEY idx_approval_record_target (target_type, target_id),
  KEY idx_approval_record_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
