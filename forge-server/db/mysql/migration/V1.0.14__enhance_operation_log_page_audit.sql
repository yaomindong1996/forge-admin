-- 增强页面操作审计日志。
-- 说明：
-- 1. 复用 sys_operation_log，不新建并行审计表，避免日志查询和导出链路分叉。
-- 2. 新增页面、操作人、操作内容、操作前后快照和差异字段。
-- 3. 补齐操作日志导出配置和页面资源权限。

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log');

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'operator_name');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN operator_name varchar(100) DEFAULT NULL COMMENT ''操作人姓名'' AFTER username',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'operation_page');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN operation_page varchar(500) DEFAULT NULL COMMENT ''操作页面路径'' AFTER operation_desc',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'operation_page_title');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN operation_page_title varchar(200) DEFAULT NULL COMMENT ''操作页面标题'' AFTER operation_page',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'operation_content');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN operation_content varchar(1000) DEFAULT NULL COMMENT ''操作内容'' AFTER operation_page_title',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'before_data');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN before_data mediumtext COMMENT ''操作前数据'' AFTER response_result',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'after_data');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN after_data mediumtext COMMENT ''操作后数据'' AFTER before_data',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'diff_data');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN diff_data mediumtext COMMENT ''操作数据差异'' AFTER after_data',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND INDEX_NAME = 'idx_operation_page_time');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE INDEX idx_operation_page_time ON sys_operation_log (operation_page(191), operation_time)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND INDEX_NAME = 'idx_operator_name');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE INDEX idx_operator_name ON sys_operation_log (operator_name)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@table_exists > 0,
    'UPDATE sys_operation_log l
     LEFT JOIN sys_user u ON l.user_id = u.id
     SET l.operator_name = COALESCE(NULLIF(u.real_name, ''''), NULLIF(u.username, ''''), l.username),
         l.operation_content = COALESCE(NULLIF(l.operation_content, ''''), NULLIF(l.operation_desc, ''''))
     WHERE l.operator_name IS NULL OR l.operation_content IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 补齐 OperationType.ADD 对应字典，兼容历史 INSERT 字典值。
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
                           dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default,
       1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 2 dict_sort, '新增' dict_label, 'ADD' dict_value, 'sys_operation_type' dict_type,
         'success' list_class, 'N' is_default, '新增操作' remark
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

-- 操作日志导出配置。
INSERT INTO sys_excel_export_config (
  config_key, config_type, export_name, sheet_name, file_name_template, data_source_bean, query_method,
  auto_trans, pageable, max_rows, sort_field, sort_order, status, include_sample, allow_import,
  remark, create_time, update_time, create_by, update_by, create_dept
)
SELECT seed.config_key, seed.config_type, seed.export_name, seed.sheet_name, seed.file_name_template, seed.data_source_bean, seed.query_method,
       seed.auto_trans, seed.pageable, seed.max_rows, seed.sort_field, seed.sort_order, seed.status, seed.include_sample, seed.allow_import,
       seed.remark, NOW(), NOW(), 1, 1, 1
FROM (
  SELECT 'sys_operation_log_export' config_key,
         '页面操作审计日志导出' export_name,
         '操作审计日志' sheet_name,
         '页面操作审计日志_{date}.xlsx' file_name_template,
         'sysOperationLogService' data_source_bean,
         'selectExportList' query_method,
         'EXPORT' config_type,
         1 auto_trans,
         0 pageable,
         50000 max_rows,
         'operation_time' sort_field,
         'DESC' sort_order,
         1 status,
         0 include_sample,
         0 allow_import,
         '页面操作审计日志导出配置' remark
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_excel_export_config c
  WHERE c.config_key = seed.config_key
);

INSERT INTO sys_excel_column_config (
  config_key, field_name, column_name, width, order_num, export, date_format, number_format,
  dict_type, importable, required, example_value, validation_rule, validation_message,
  create_time, update_time, create_by, update_by, create_dept
)
SELECT seed.config_key, seed.field_name, seed.column_name, seed.width, seed.order_num, 1, seed.date_format, NULL,
       seed.dict_type, 0, 0, NULL, NULL, NULL, NOW(), NOW(), 1, 1, 1
FROM (
  SELECT 'sys_operation_log_export' config_key, 'username' field_name, '操作账号' column_name, 18 width, 1 order_num, NULL date_format, NULL dict_type
  UNION ALL SELECT 'sys_operation_log_export', 'operatorName', '操作人', 18, 2, NULL, NULL
  UNION ALL SELECT 'sys_operation_log_export', 'operationTime', '操作时间', 22, 3, 'yyyy-MM-dd HH:mm:ss', NULL
  UNION ALL SELECT 'sys_operation_log_export', 'operationPageTitle', '操作页面', 24, 4, NULL, NULL
  UNION ALL SELECT 'sys_operation_log_export', 'operationPage', '页面路径', 36, 5, NULL, NULL
  UNION ALL SELECT 'sys_operation_log_export', 'operationModule', '操作模块', 22, 6, NULL, NULL
  UNION ALL SELECT 'sys_operation_log_export', 'operationType', '操作类型', 14, 7, NULL, 'sys_operation_type'
  UNION ALL SELECT 'sys_operation_log_export', 'operationContent', '操作内容', 40, 8, NULL, NULL
  UNION ALL SELECT 'sys_operation_log_export', 'requestMethod', '请求方法', 12, 9, NULL, 'sys_req_method'
  UNION ALL SELECT 'sys_operation_log_export', 'requestUrl', '请求URL', 40, 10, NULL, NULL
  UNION ALL SELECT 'sys_operation_log_export', 'operationStatus', '操作状态', 12, 11, NULL, 'sys_common_status'
  UNION ALL SELECT 'sys_operation_log_export', 'operationIp', 'IP地址', 18, 12, NULL, NULL
  UNION ALL SELECT 'sys_operation_log_export', 'executeTime', '耗时ms', 12, 13, NULL, NULL
  UNION ALL SELECT 'sys_operation_log_export', 'diffData', '数据差异', 60, 14, NULL, NULL
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_excel_column_config c
  WHERE c.config_key = seed.config_key
    AND c.field_name = seed.field_name
);

-- 菜单和权限补丁。
UPDATE sys_resource
SET resource_name = '页面操作审计',
    perms = COALESCE(NULLIF(perms, ''), 'system:operationLog:list'),
    icon = COALESCE(NULLIF(icon, ''), 'ionicons5:ShieldCheckmarkOutline'),
    remark = COALESCE(NULLIF(remark, ''), '页面操作审计日志'),
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND path = '/system/operation-log';

SET @monitor_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type IN (1, 2)
    AND path = '/system/monitor'
    AND del_flag = 0
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '页面操作审计', @monitor_menu_id, 2, 2, '/system/operation-log', 'system/operation-log', 0,
       0, NULL, '_self', 0, 1, 1, 'system:operationLog:list', 'ionicons5:ShieldCheckmarkOutline',
       NULL, NULL, 1, 0, NULL, '页面操作审计日志', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @monitor_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND path = '/system/operation-log'
      AND del_flag = 0
  );

SET @operation_log_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/system/operation-log'
    AND del_flag = 0
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, @operation_log_menu_id, seed.resource_type, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '审计日志查询' resource_name, 3 resource_type, 1 sort, 'system:operationLog:query' perms,
         NULL api_method, NULL api_url, '查询页面操作审计日志' remark
  UNION ALL SELECT 1, '审计日志详情', 3, 2, 'system:operationLog:detail',
         NULL, NULL, '查看页面操作审计详情'
  UNION ALL SELECT 1, '审计日志导出', 3, 3, 'system:operationLog:export',
         NULL, NULL, '导出页面操作审计日志'
  UNION ALL SELECT 1, '审计日志分页接口', 4, 11, 'system:operationLog:api:page',
         'GET', '/system/operationLog/page', '页面操作审计日志分页接口'
  UNION ALL SELECT 1, '审计日志详情接口', 4, 12, 'system:operationLog:api:detail',
         'GET', '/system/operationLog/{id}', '页面操作审计日志详情接口'
  UNION ALL SELECT 1, '审计日志导出接口', 4, 13, 'system:operationLog:api:export',
         'POST', '/api/excel/export/sys_operation_log_export', '页面操作审计日志导出接口'
) seed
WHERE @operation_log_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id
      AND r.resource_type = seed.resource_type
      AND r.perms = seed.perms
      AND r.del_flag = 0
  );
