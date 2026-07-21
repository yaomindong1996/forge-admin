-- 定时任务 Flowable 编排：固定已发布流程定义，并关联流程实例。

SET @job_config_table_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
);

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'invoke_mode'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN invoke_mode varchar(16) NOT NULL DEFAULT ''SINGLE'' COMMENT ''调用方式：SINGLE/FLOW'' AFTER execute_mode',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'flow_model_key'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN flow_model_key varchar(100) NULL COMMENT ''已发布流程模型Key'' AFTER invoke_mode',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'flow_model_version'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN flow_model_version int NULL COMMENT ''保存时固定的流程模型版本'' AFTER flow_model_key',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'flow_deployment_id'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN flow_deployment_id varchar(64) NULL COMMENT ''保存时固定的Flowable部署ID'' AFTER flow_model_version',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'flow_process_definition_id'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN flow_process_definition_id varchar(64) NULL COMMENT ''保存时固定的Flowable流程定义ID'' AFTER flow_deployment_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @invoke_mode_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'invoke_mode'
);
SET @sql = IF(@job_config_table_exists = 1 AND @invoke_mode_exists = 1,
  'UPDATE sys_job_config SET invoke_mode = ''SINGLE'' WHERE invoke_mode IS NULL OR TRIM(invoke_mode) = ''''',
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
    AND COLUMN_NAME = 'process_instance_id'
);
SET @sql = IF(@job_log_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_log ADD COLUMN process_instance_id varchar(64) NULL COMMENT ''Flowable流程实例ID'' AFTER fire_instance_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
    AND INDEX_NAME = 'idx_job_log_process_instance'
);
SET @process_instance_column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
    AND COLUMN_NAME = 'process_instance_id'
);
SET @sql = IF(@job_log_table_exists = 1 AND @process_instance_column_exists = 1 AND @index_exists = 0,
  'ALTER TABLE sys_job_log ADD INDEX idx_job_log_process_instance (process_instance_id, del_flag)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '定时任务调用方式' dict_name, 'sys_job_invoke_mode' dict_type,
         '控制任务执行单一执行器或启动固定Flowable流程' remark
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
  SELECT 1 tenant_id, 1 dict_sort, '单一执行器' dict_label, 'SINGLE' dict_value,
         'sys_job_invoke_mode' dict_type, 'info' list_class, 'Y' is_default,
         '按BEAN/HANDLER/RPC执行一个任务目标' remark
  UNION ALL SELECT 1, 2, '流程编排', 'FLOW', 'sys_job_invoke_mode', 'success', 'N',
         '启动保存时固定的已发布Flowable流程定义' remark
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);
