-- 定时任务并发、重试与 Misfire 治理：增加策略字段和完整执行生命周期字段。

SET @job_config_table_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
);

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'concurrent_policy'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN concurrent_policy varchar(32) NOT NULL DEFAULT ''ALLOW'' COMMENT ''并发策略：ALLOW/SKIP_IF_RUNNING'' AFTER execute_mode',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'misfire_policy'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN misfire_policy varchar(32) NOT NULL DEFAULT ''DO_NOTHING'' COMMENT ''错过触发策略：FIRE_ONCE_NOW/DO_NOTHING'' AFTER concurrent_policy',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'idempotent_flag'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN idempotent_flag tinyint NOT NULL DEFAULT 0 COMMENT ''是否允许幂等重试：0否 1是'' AFTER misfire_policy',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @concurrent_policy_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'concurrent_policy'
);
SET @sql = IF(@job_config_table_exists = 1 AND @concurrent_policy_exists = 1,
  'UPDATE sys_job_config SET concurrent_policy = ''ALLOW'' WHERE concurrent_policy IS NULL OR TRIM(concurrent_policy) = ''''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @misfire_policy_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'misfire_policy'
);
SET @sql = IF(@job_config_table_exists = 1 AND @misfire_policy_exists = 1,
  'UPDATE sys_job_config SET misfire_policy = ''DO_NOTHING'' WHERE misfire_policy IS NULL OR TRIM(misfire_policy) = ''''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idempotent_flag_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'idempotent_flag'
);
SET @sql = IF(@job_config_table_exists = 1 AND @idempotent_flag_exists = 1,
  'UPDATE sys_job_config SET idempotent_flag = 0 WHERE idempotent_flag IS NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @job_log_table_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
);

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
    AND COLUMN_NAME = 'scheduled_fire_time'
);
SET @sql = IF(@job_log_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_log ADD COLUMN scheduled_fire_time datetime NULL COMMENT ''Quartz原计划触发时间'' AFTER trigger_type',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
    AND COLUMN_NAME = 'fire_instance_id'
);
SET @sql = IF(@job_log_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_log ADD COLUMN fire_instance_id varchar(200) NULL COMMENT ''Quartz执行实例ID'' AFTER scheduled_fire_time',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '定时任务并发策略' dict_name, 'sys_job_concurrent_policy' dict_type,
         '控制同一任务执行实例是否允许并发' remark
  UNION ALL SELECT 1, '定时任务Misfire策略', 'sys_job_misfire_policy', '控制错过计划触发时间后的行为'
  UNION ALL SELECT 1, '定时任务执行状态', 'sys_job_log_status', '定时任务单次顶层执行生命周期状态'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '允许并行' dict_label, 'ALLOW' dict_value,
         'sys_job_concurrent_policy' dict_type, 'success' list_class, 'Y' is_default, '允许同一任务同时执行' remark
  UNION ALL SELECT 1, 2, '运行中跳过', 'SKIP_IF_RUNNING', 'sys_job_concurrent_policy', 'warning', 'N', '上一轮未结束时立即跳过本轮'
  UNION ALL SELECT 1, 1, '立即补偿一次', 'FIRE_ONCE_NOW', 'sys_job_misfire_policy', 'warning', 'N', '错过计划时间后立即补偿执行一次'
  UNION ALL SELECT 1, 2, '不补偿', 'DO_NOTHING', 'sys_job_misfire_policy', 'info', 'Y', '错过计划时间后等待下次正常触发'
  UNION ALL SELECT 1, 1, '失败', '0', 'sys_job_log_status', 'error', 'N', '任务最终执行失败'
  UNION ALL SELECT 1, 2, '成功', '1', 'sys_job_log_status', 'success', 'N', '任务最终执行成功'
  UNION ALL SELECT 1, 3, '运行中', '2', 'sys_job_log_status', 'info', 'N', '任务已接受并正在执行'
  UNION ALL SELECT 1, 4, '已跳过', '3', 'sys_job_log_status', 'warning', 'N', '并发竞争或执行保护导致跳过'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);
