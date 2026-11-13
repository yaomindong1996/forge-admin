-- 修复定时任务整体扫描发现的出站场景和运行状态一致性问题。

ALTER TABLE `sys_outbound_whitelist`
  MODIFY COLUMN `scene` varchar(32) NOT NULL COMMENT '出站场景：JOB_WEBHOOK/JOB_RPC/FLOW_API';

SET @has_outbound_private_constraint = (
  SELECT COUNT(*)
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_outbound_whitelist'
    AND CONSTRAINT_NAME = 'chk_outbound_private_scene'
);
SET @drop_outbound_private_constraint_sql = IF(
  @has_outbound_private_constraint > 0,
  'ALTER TABLE `sys_outbound_whitelist` DROP CHECK `chk_outbound_private_scene`',
  'SELECT 1'
);
PREPARE drop_outbound_private_constraint_stmt FROM @drop_outbound_private_constraint_sql;
EXECUTE drop_outbound_private_constraint_stmt;
DEALLOCATE PREPARE drop_outbound_private_constraint_stmt;

ALTER TABLE `sys_outbound_whitelist`
  ADD CONSTRAINT `chk_outbound_private_scene`
  CHECK (`allow_private` = 0 OR `scene` IN ('FLOW_API', 'JOB_RPC'));

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 3, '任务RPC', 'JOB_RPC', 'sys_outbound_scene',
       NULL, 'primary', 'N', 1, '任务执行器RPC场景，管理员可显式授权私网',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data data
  WHERE data.tenant_id = 1
    AND data.dict_type = 'sys_outbound_scene'
    AND data.dict_value = 'JOB_RPC'
);

SET @job_log_heartbeat_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
    AND COLUMN_NAME = 'heartbeat_time'
);
SET @add_job_log_heartbeat_column_sql = IF(
  @job_log_heartbeat_column_exists = 0,
  'ALTER TABLE `sys_job_log` ADD COLUMN `heartbeat_time` datetime NULL COMMENT ''运行中任务最近心跳时间'' AFTER `start_time`',
  'SELECT 1'
);
PREPARE add_job_log_heartbeat_column_stmt FROM @add_job_log_heartbeat_column_sql;
EXECUTE add_job_log_heartbeat_column_stmt;
DEALLOCATE PREPARE add_job_log_heartbeat_column_stmt;

SET @job_log_heartbeat_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
    AND INDEX_NAME = 'idx_job_log_recovery'
);
SET @add_job_log_heartbeat_index_sql = IF(
  @job_log_heartbeat_index_exists = 0,
  'CREATE INDEX `idx_job_log_recovery` ON `sys_job_log` (`status`, `heartbeat_time`, `trigger_time`, `del_flag`)',
  'SELECT 1'
);
PREPARE add_job_log_heartbeat_index_stmt FROM @add_job_log_heartbeat_index_sql;
EXECUTE add_job_log_heartbeat_index_stmt;
DEALLOCATE PREPARE add_job_log_heartbeat_index_stmt;

SET @job_last_completion_time_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'last_completion_time'
);
SET @add_job_last_completion_time_column_sql = IF(
  @job_last_completion_time_column_exists = 0,
  'ALTER TABLE `sys_job_config` ADD COLUMN `last_completion_time` datetime NULL COMMENT ''最近推进失败统计的执行完成时间'' AFTER `consecutive_failures`',
  'SELECT 1'
);
PREPARE add_job_last_completion_time_column_stmt FROM @add_job_last_completion_time_column_sql;
EXECUTE add_job_last_completion_time_column_stmt;
DEALLOCATE PREPARE add_job_last_completion_time_column_stmt;

SET @job_last_completion_execution_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'last_completion_execution_id'
);
SET @add_job_last_completion_execution_column_sql = IF(
  @job_last_completion_execution_column_exists = 0,
  'ALTER TABLE `sys_job_config` ADD COLUMN `last_completion_execution_id` bigint NULL COMMENT ''最近推进失败统计的执行ID'' AFTER `last_completion_time`',
  'SELECT 1'
);
PREPARE add_job_last_completion_execution_column_stmt FROM @add_job_last_completion_execution_column_sql;
EXECUTE add_job_last_completion_execution_column_stmt;
DEALLOCATE PREPARE add_job_last_completion_execution_column_stmt;
