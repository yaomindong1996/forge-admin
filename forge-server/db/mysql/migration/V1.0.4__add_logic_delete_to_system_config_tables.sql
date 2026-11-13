-- 为 Batch 2.2 平台系统配置表补齐逻辑删除契约。
-- 说明：
-- 1. 本批只处理配置、字典、通知、API 配置、数据权限配置、文件存储配置。
-- 2. 用户、角色、组织、菜单资源和授权关系表继续拆到后续批次单独评审。
-- 3. 唯一索引使用 logic_delete_active 生成列，仅约束未删除记录。

-- sys_config.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_config ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）'' AFTER create_dept',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_config SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_config ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config' AND INDEX_NAME = 'uk_tenant_config_key');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sys_config DROP INDEX uk_tenant_config_key',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config' AND INDEX_NAME = 'uk_tenant_config_key_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_tenant_config_key_active ON sys_config (tenant_id, config_key, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_dict_type.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_type');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_type' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_dict_type ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）'' AFTER create_dept',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_type' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_dict_type SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_type' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_dict_type ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_type' AND INDEX_NAME = 'uk_tenant_dict_type');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sys_dict_type DROP INDEX uk_tenant_dict_type',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_type' AND INDEX_NAME = 'uk_tenant_dict_type_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_tenant_dict_type_active ON sys_dict_type (tenant_id, dict_type, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_dict_data.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_dict_data ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）'' AFTER create_dept',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_dict_data SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_dict_data ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND INDEX_NAME = 'uk_tenant_dict_data');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sys_dict_data DROP INDEX uk_tenant_dict_data',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND INDEX_NAME = 'uk_tenant_dict_data_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_tenant_dict_data_active ON sys_dict_data (tenant_id, dict_type, dict_value, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_notice.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notice');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notice' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_notice ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）'' AFTER create_dept',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notice' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_notice SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_api_config.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_api_config');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_api_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_api_config ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）'' AFTER tenant_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_api_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_api_config SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_api_config' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_api_config ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_api_config' AND INDEX_NAME = 'uk_method_url');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sys_api_config DROP INDEX uk_method_url',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_api_config' AND INDEX_NAME = 'uk_method_url_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_method_url_active ON sys_api_config (url_path, req_method, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_data_scope_config.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_data_scope_config');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_data_scope_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_data_scope_config ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）'' AFTER create_dept',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_data_scope_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_data_scope_config SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_data_scope_config' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_data_scope_config ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_data_scope_config' AND INDEX_NAME = 'uk_tenant_mapper');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sys_data_scope_config DROP INDEX uk_tenant_mapper',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_data_scope_config' AND INDEX_NAME = 'uk_tenant_mapper_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_tenant_mapper_active ON sys_data_scope_config (tenant_id, mapper_method, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_file_storage_config.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file_storage_config');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file_storage_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_file_storage_config ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）'' AFTER create_dept',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file_storage_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_file_storage_config SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
