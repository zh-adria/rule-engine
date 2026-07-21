-- V2: keep existing MySQL deployments compatible after adding the re_ table prefix.

SET @old_table = 'approval_record';
SET @new_table = 're_approval_record';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @old_table) = 1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = @new_table) = 0,
  CONCAT('RENAME TABLE `', @old_table, '` TO `', @new_table, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
