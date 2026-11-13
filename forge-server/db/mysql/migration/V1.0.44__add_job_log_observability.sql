-- 定时任务运维可观测性：连续失败计数、日志查询索引和安全导出配置。

SET @job_config_table_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
);

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'consecutive_failures'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN consecutive_failures int NOT NULL DEFAULT 0 COMMENT ''连续失败次数'' AFTER retry_count',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @job_log_table_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
);

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_log'
    AND INDEX_NAME = 'idx_job_log_observability'
);
SET @sql = IF(@job_log_table_exists = 1 AND @index_exists = 0,
  'CREATE INDEX idx_job_log_observability ON sys_job_log (job_config_id, status, trigger_type, trigger_time)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 存量数据按最近一次成功之后的失败日志回填；跳过日志不清零也不递增。
SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'consecutive_failures'
);
SET @sql = IF(@job_config_table_exists = 1 AND @job_log_table_exists = 1 AND @column_exists = 1,
  'UPDATE sys_job_config job
   LEFT JOIN (
     SELECT failed_log.job_config_id, COUNT(*) AS failure_count
     FROM sys_job_log failed_log
     LEFT JOIN (
       SELECT success_log.job_config_id,
              MAX(COALESCE(success_log.end_time, success_log.start_time, success_log.trigger_time)) AS last_success_time
       FROM sys_job_log success_log
       WHERE success_log.status = 1
         AND success_log.del_flag = 0
       GROUP BY success_log.job_config_id
     ) success_state ON success_state.job_config_id = failed_log.job_config_id
     WHERE failed_log.status = 0
       AND failed_log.del_flag = 0
       AND (success_state.last_success_time IS NULL
         OR COALESCE(failed_log.end_time, failed_log.start_time, failed_log.trigger_time) > success_state.last_success_time)
     GROUP BY failed_log.job_config_id
   ) failure_state ON failure_state.job_config_id = job.id
   SET job.consecutive_failures = COALESCE(failure_state.failure_count, 0)
   WHERE job.del_flag = 0',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '定时任务触发来源', 'sys_job_trigger_type', 1, '定时任务执行日志触发来源',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type data
  WHERE data.tenant_id = 1
    AND data.dict_type = 'sys_job_trigger_type'
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
  SELECT 1 tenant_id, 1 dict_sort, '计划触发' dict_label, 'SCHEDULED' dict_value,
         'sys_job_trigger_type' dict_type, 'info' list_class, 'Y' is_default, 'Quartz计划触发' remark
  UNION ALL SELECT 1, 2, '手动触发', 'MANUAL', 'sys_job_trigger_type', 'warning', 'N', '管理员立即运行触发'
  UNION ALL SELECT 1, 3, '未知来源', 'UNKNOWN', 'sys_job_trigger_type', 'default', 'N', '历史日志或未标记来源'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

INSERT INTO sys_excel_export_config (
  config_key, config_type, export_name, sheet_name, file_name_template, data_source_bean, query_method,
  auto_trans, pageable, max_rows, sort_field, sort_order, status, include_sample, allow_import,
  remark, create_time, update_time, create_by, update_by, create_dept
)
SELECT 'sys_job_log_export', 'EXPORT', '定时任务执行日志导出', '执行日志',
       '定时任务执行日志_{date}_{time}.xlsx', 'sysJobLogService', 'selectExportList',
       0, 0, 50000, 'trigger_time', 'DESC', 1, 0, 0,
       '仅导出运维白名单字段，不包含任务参数、完整结果和完整异常', NOW(), NOW(), 1, 1, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_excel_export_config config
  WHERE config.config_key = 'sys_job_log_export'
);

INSERT INTO sys_excel_column_config (
  config_key, field_name, column_name, width, order_num, export, date_format, number_format,
  dict_type, importable, required, example_value, validation_rule, validation_message,
  create_time, update_time, create_by, update_by, create_dept
)
SELECT seed.config_key, seed.field_name, seed.column_name, seed.width, seed.order_num, 1,
       seed.date_format, NULL, seed.dict_type, 0, 0, NULL, NULL, NULL,
       NOW(), NOW(), 1, 1, 1
FROM (
  SELECT 'sys_job_log_export' config_key, 'jobName' field_name, '任务名称' column_name, 24 width, 1 order_num, NULL date_format, NULL dict_type
  UNION ALL SELECT 'sys_job_log_export', 'jobGroup', '任务分组', 18, 2, NULL, NULL
  UNION ALL SELECT 'sys_job_log_export', 'executorHandler', '执行器', 28, 3, NULL, NULL
  UNION ALL SELECT 'sys_job_log_export', 'triggerType', '触发来源', 14, 4, NULL, 'sys_job_trigger_type'
  UNION ALL SELECT 'sys_job_log_export', 'status', '执行状态', 12, 5, NULL, 'sys_job_log_status'
  UNION ALL SELECT 'sys_job_log_export', 'scheduledFireTime', '计划触发时间', 22, 6, 'yyyy-MM-dd HH:mm:ss', NULL
  UNION ALL SELECT 'sys_job_log_export', 'triggerTime', '实际触发时间', 22, 7, 'yyyy-MM-dd HH:mm:ss', NULL
  UNION ALL SELECT 'sys_job_log_export', 'startTime', '开始时间', 22, 8, 'yyyy-MM-dd HH:mm:ss', NULL
  UNION ALL SELECT 'sys_job_log_export', 'endTime', '结束时间', 22, 9, 'yyyy-MM-dd HH:mm:ss', NULL
  UNION ALL SELECT 'sys_job_log_export', 'duration', '执行时长(ms)', 16, 10, NULL, NULL
  UNION ALL SELECT 'sys_job_log_export', 'retryCount', '重试次数', 12, 11, NULL, NULL
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_excel_column_config column_config
  WHERE column_config.config_key = seed.config_key
    AND column_config.field_name = seed.field_name
);
