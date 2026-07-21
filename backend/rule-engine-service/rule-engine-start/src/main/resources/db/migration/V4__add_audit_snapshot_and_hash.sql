-- P1-3: add before/after snapshot and audit-hash chain to re_rule_audit_log
ALTER TABLE re_rule_audit_log ADD COLUMN IF NOT EXISTS before_json CLOB;
ALTER TABLE re_rule_audit_log ADD COLUMN IF NOT EXISTS after_json CLOB;
ALTER TABLE re_rule_audit_log ADD COLUMN IF NOT EXISTS audit_hash VARCHAR(64);
ALTER TABLE re_rule_audit_log ADD COLUMN IF NOT EXISTS previous_hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_rule_audit_hash_chain ON re_rule_audit_log(rule_code, audit_hash);
