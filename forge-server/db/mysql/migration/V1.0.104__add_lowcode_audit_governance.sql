-- 低代码状态迁移、金额治理和审计归档元数据。
-- 审计日志属于运行留存表，普通行级删除不开放；专用归档任务按 retention_until 处理。

SET @table_exists = (
  SELECT COUNT(1) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
);

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'audit_event_type'
);
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN audit_event_type varchar(32) DEFAULT NULL COMMENT ''结构化审计事件类型'' AFTER actor_type',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'status_field'
);
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN status_field varchar(64) DEFAULT NULL COMMENT ''状态字段编码'' AFTER audit_event_type',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'status_from'
);
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN status_from varchar(64) DEFAULT NULL COMMENT ''状态迁移前值'' AFTER status_field',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'status_to'
);
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN status_to varchar(64) DEFAULT NULL COMMENT ''状态迁移后值'' AFTER status_from',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'change_summary'
);
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN change_summary varchar(4000) DEFAULT NULL COMMENT ''脱敏结构化变更摘要'' AFTER status_to',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'retention_until'
);
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN retention_until datetime DEFAULT NULL COMMENT ''审计留存截止时间'' AFTER change_summary',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND index_name = 'idx_ai_business_action_audit'
);
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
  'CREATE INDEX idx_ai_business_action_audit ON ai_business_action_execution_log (tenant_id, object_code, record_id, audit_event_type, create_time)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
