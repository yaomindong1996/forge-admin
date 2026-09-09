-- 为临时抄送增加可追踪、可撤回的关系状态。
SET @table_exists := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_cc');
SET @column_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_cc' AND COLUMN_NAME = 'status');
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_flow_cc ADD COLUMN status tinyint NOT NULL DEFAULT 0 COMMENT ''抄送关系状态：0有效/1已撤回'' AFTER is_read',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '流程抄送状态', 'flow_cc_status', 1, '流程抄送关系状态',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type WHERE tenant_id = 1 AND dict_type = 'flow_cc_status'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value,
       'flow_cc_status', NULL, seed.list_class, seed.is_default, 1,
       seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '有效' dict_label, '0' dict_value,
           'success' list_class, 'Y' is_default, '可正常查看的抄送关系' remark
    UNION ALL
    SELECT 1, 2, '已撤回', '1', 'warning', 'N', '发送人已撤回的抄送关系'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d
    WHERE d.tenant_id = seed.tenant_id
      AND d.dict_type = 'flow_cc_status'
      AND d.dict_value = seed.dict_value
);
SET @column_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_cc' AND COLUMN_NAME = 'revoke_by');
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_flow_cc ADD COLUMN revoke_by varchar(64) DEFAULT NULL AFTER status', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_cc' AND COLUMN_NAME = 'revoke_time');
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_flow_cc ADD COLUMN revoke_time datetime DEFAULT NULL AFTER revoke_by', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_cc' AND COLUMN_NAME = 'revoke_reason');
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_flow_cc ADD COLUMN revoke_reason varchar(500) DEFAULT NULL AFTER revoke_time', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_cc' AND INDEX_NAME = 'idx_flow_cc_status');
SET @ddl := IF(@table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE sys_flow_cc ADD KEY idx_flow_cc_status (tenant_id, cc_user_id, status, cc_time)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
