-- 定时任务可靠性加固：增加数据库与 Quartz 同步状态、乐观版本和日志关联字段。

SET @job_config_table_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
);

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'sync_status'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN sync_status varchar(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''Quartz同步状态''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'sync_error'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN sync_error varchar(1000) NULL COMMENT ''最近一次Quartz同步错误''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'sync_time'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN sync_time datetime NULL COMMENT ''最近一次Quartz同步时间''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'version'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN version int NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND INDEX_NAME = 'idx_job_sync_status_del'
);
SET @sql = IF(@job_config_table_exists = 1 AND @index_exists = 0,
  'CREATE INDEX idx_job_sync_status_del ON sys_job_config (sync_status, del_flag)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'sync_status'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 1,
  'UPDATE sys_job_config SET sync_status = ''PENDING'' WHERE sync_status IS NULL OR TRIM(sync_status) = ''''',
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
    AND COLUMN_NAME = 'job_config_id'
);
SET @sql = IF(@job_log_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_log ADD COLUMN job_config_id bigint NULL COMMENT ''任务配置ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
    AND COLUMN_NAME = 'trigger_type'
);
SET @sql = IF(@job_log_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_log ADD COLUMN trigger_type varchar(20) NOT NULL DEFAULT ''UNKNOWN'' COMMENT ''触发类型''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
    AND INDEX_NAME = 'idx_job_log_config_trigger'
);
SET @sql = IF(@job_log_table_exists = 1 AND @index_exists = 0,
  'CREATE INDEX idx_job_log_config_trigger ON sys_job_log (job_config_id, trigger_time)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
    AND COLUMN_NAME = 'trigger_type'
);
SET @sql = IF(@job_log_table_exists = 1 AND @column_exists = 1,
  'UPDATE sys_job_log SET trigger_type = ''UNKNOWN'' WHERE trigger_type IS NULL OR TRIM(trigger_type) = ''''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @job_config_id_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
    AND COLUMN_NAME = 'job_config_id'
);
SET @sql = IF(@job_config_table_exists = 1 AND @job_log_table_exists = 1 AND @job_config_id_exists = 1,
  'UPDATE sys_job_log job_log
   INNER JOIN sys_job_config job_config
     ON job_config.job_name = job_log.job_name
    AND job_config.job_group = job_log.job_group
    AND job_config.del_flag = 0
   SET job_log.job_config_id = job_config.id
   WHERE job_log.job_config_id IS NULL
     AND job_log.del_flag = 0',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '定时任务同步状态', 'sys_job_sync_status', 1, '任务配置与Quartz运行态同步状态',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type
  WHERE tenant_id = 1 AND dict_type = 'sys_job_sync_status'
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
  SELECT 1 tenant_id, 1 dict_sort, '待同步' dict_label, 'PENDING' dict_value,
         'sys_job_sync_status' dict_type, 'warning' list_class, 'Y' is_default, '等待同步到Quartz' remark
  UNION ALL SELECT 1, 2, '已同步', 'SYNCED', 'sys_job_sync_status', 'success', 'N', '数据库与Quartz状态一致'
  UNION ALL SELECT 1, 3, '同步失败', 'FAILED', 'sys_job_sync_status', 'error', 'N', '同步失败，可手动重试'
  UNION ALL SELECT 1, 4, '待删除', 'DELETE_PENDING', 'sys_job_sync_status', 'warning', 'N', '等待从Quartz移除'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);
