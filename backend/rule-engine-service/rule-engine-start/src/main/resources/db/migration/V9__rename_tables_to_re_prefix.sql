-- V9: keep existing MySQL deployments compatible after adding the re_ table prefix.

SET @old_table = 'rule_definition';
SET @new_table = 're_rule_definition';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'rule_version';
SET @new_table = 're_rule_version';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'rule_execution_log';
SET @new_table = 're_rule_execution_log';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'rule_audit_log';
SET @new_table = 're_rule_audit_log';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'rule_set';
SET @new_table = 're_rule_set';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'rule_set_step';
SET @new_table = 're_rule_set_step';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'rule_product_binding';
SET @new_table = 're_rule_product_binding';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'webhook_config';
SET @new_table = 're_webhook_config';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'webhook_log';
SET @new_table = 're_webhook_log';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'rule_template';
SET @new_table = 're_rule_template';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'idempotency_key';
SET @new_table = 're_idempotency_key';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'custom_field';
SET @new_table = 're_custom_field';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'rule_test_case';
SET @new_table = 're_rule_test_case';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'rule_test_suite';
SET @new_table = 're_rule_test_suite';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'rule_test_suite_case';
SET @new_table = 're_rule_test_suite_case';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_table = 'rule_test_run';
SET @new_table = 're_rule_test_run';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
