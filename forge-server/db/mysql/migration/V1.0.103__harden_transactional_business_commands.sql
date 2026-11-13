-- 事务型业务命令：记录不可变动作版本和执行模式，并按动作版本隔离幂等唯一域。

SET @table_exists = (
  SELECT COUNT(1) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
);

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'action_version'
);
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN action_version int NOT NULL DEFAULT 0 COMMENT ''不可变业务对象发布版本'' AFTER action_name',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'execution_mode'
);
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN execution_mode varchar(32) NOT NULL DEFAULT ''ORCHESTRATION'' COMMENT ''LOCAL_TRANSACTION/ORCHESTRATION'' AFTER action_version',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_index_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND index_name = 'uk_ai_business_action_idem'
);
SET @sql = IF(@table_exists > 0 AND @old_index_exists > 0,
  'ALTER TABLE ai_business_action_execution_log DROP INDEX uk_ai_business_action_idem',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @new_index_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND index_name = 'uk_ai_business_action_version_idem'
);
SET @sql = IF(@table_exists > 0 AND @new_index_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD UNIQUE INDEX uk_ai_business_action_version_idem (tenant_id, object_code, record_id, action_code, action_version, idempotency_key)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

