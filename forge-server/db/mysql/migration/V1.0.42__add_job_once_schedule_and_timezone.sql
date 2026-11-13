-- 定时任务一次性调度与独立时区：存量任务按当前 Asia/Shanghai 部署基线保持原触发时刻。

SET @job_config_table_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
);

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'schedule_type'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN schedule_type varchar(20) NOT NULL DEFAULT ''CRON'' COMMENT ''调度类型：CRON/ONCE'' AFTER executor_service',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'fire_once_time'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN fire_once_time datetime NULL COMMENT ''一次性任务本地触发时间'' AFTER cron_expression',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'timezone'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN timezone varchar(64) NOT NULL DEFAULT ''Asia/Shanghai'' COMMENT ''IANA时区'' AFTER fire_once_time',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @cron_not_nullable = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'cron_expression'
    AND IS_NULLABLE = 'NO'
);
SET @sql = IF(@job_config_table_exists = 1 AND @cron_not_nullable = 1,
  'ALTER TABLE sys_job_config MODIFY COLUMN cron_expression varchar(100) NULL COMMENT ''Cron表达式''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schedule_type_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'schedule_type'
);
SET @sql = IF(@job_config_table_exists = 1 AND @schedule_type_exists = 1,
  'UPDATE sys_job_config SET schedule_type = ''CRON'' WHERE schedule_type IS NULL OR TRIM(schedule_type) = ''''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @timezone_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'timezone'
);
SET @sql = IF(@job_config_table_exists = 1 AND @timezone_exists = 1,
  'UPDATE sys_job_config SET timezone = ''Asia/Shanghai'' WHERE timezone IS NULL OR TRIM(timezone) = ''''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '定时任务调度类型', 'sys_job_schedule_type', 1, 'Cron周期调度或一次性调度',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type
  WHERE tenant_id = 1 AND dict_type = 'sys_job_schedule_type'
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
  SELECT 1 tenant_id, 1 dict_sort, '周期执行' dict_label, 'CRON' dict_value,
         'sys_job_schedule_type' dict_type, 'success' list_class, 'Y' is_default, '按固定周期重复执行' remark
  UNION ALL SELECT 1, 2, '仅执行一次', 'ONCE', 'sys_job_schedule_type', 'info', 'N', '在指定时间执行一次后结束'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 3, '已结束', '2', 'sys_job_status',
       NULL, 'info', 'N', 1, '一次性任务已完成计划触发',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data
  WHERE tenant_id = 1
    AND dict_type = 'sys_job_status'
    AND dict_value = '2'
);
