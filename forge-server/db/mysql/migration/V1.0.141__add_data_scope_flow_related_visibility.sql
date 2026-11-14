-- 业务 Mapper 数据权限可按流程经手附加只读可见。
-- 不改业务表：仍用发起流程时的 businessKey（业务类型:记录ID）对齐 sys_flow_record_participant。

SET @table_exists = (
  SELECT COUNT(*)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_data_scope_config'
);

SET @column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_data_scope_config'
    AND COLUMN_NAME = 'flow_related_visible'
);

SET @sql = IF(
  @table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE `sys_data_scope_config` ADD COLUMN `flow_related_visible` tinyint NOT NULL DEFAULT 0 COMMENT ''流程经手人可查看单据（0否 1是）'' AFTER `user_table_alias`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_data_scope_config'
    AND COLUMN_NAME = 'flow_business_type'
);

SET @sql = IF(
  @table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE `sys_data_scope_config` ADD COLUMN `flow_business_type` varchar(128) DEFAULT NULL COMMENT ''流程 businessKey 前缀，对应 sys_flow_record_participant.business_type'' AFTER `flow_related_visible`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_data_scope_config'
    AND COLUMN_NAME = 'record_id_column'
);

SET @sql = IF(
  @table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE `sys_data_scope_config` ADD COLUMN `record_id_column` varchar(64) DEFAULT NULL COMMENT ''业务表主键列，对应 businessKey 中的记录ID，默认 id'' AFTER `flow_business_type`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
