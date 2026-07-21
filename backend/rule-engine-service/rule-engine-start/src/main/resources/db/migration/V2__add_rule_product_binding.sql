-- 规则-产品关联表（记录规则与外部系统产品编码的关联关系）
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
