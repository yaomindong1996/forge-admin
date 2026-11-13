-- 为 Batch 2.1 平台内部配置表补齐逻辑删除契约。
-- 说明：
-- 1. 只处理 Forge 自有内部表，不处理 qrtz_*、act_*、flw_* 等框架表。
-- 2. sys_job_log 普通删除接入逻辑删除；cleanLog 留存清理通过专用 Mapper 执行物理删除。
-- 3. 唯一索引使用 logic_delete_active 生成列，仅约束未删除记录，避免多次删除同一业务键后冲突。

-- ai_context_config.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_context_config');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_context_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_context_config ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）'' AFTER update_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_context_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE ai_context_config SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_model.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_model');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_model' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_model ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）'' AFTER remark',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_model' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE ai_model SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_model' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_model ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = ''0'' THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_model' AND INDEX_NAME = 'uk_ai_model_provider_model');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE ai_model DROP INDEX uk_ai_model_provider_model',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_model' AND INDEX_NAME = 'uk_ai_model_provider_model_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_ai_model_provider_model_active ON ai_model (provider_id, model_id, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_page_template.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_page_template');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_page_template' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_page_template ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）'' AFTER update_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_page_template' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE ai_page_template SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_page_template' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_page_template ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = ''0'' THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_page_template' AND INDEX_NAME = 'uk_template_key');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE ai_page_template DROP INDEX uk_template_key',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_page_template' AND INDEX_NAME = 'uk_template_key_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_template_key_active ON ai_page_template (template_key, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_crud_config.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_crud_config');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_crud_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_crud_config ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）'' AFTER logic_delete_strategy',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_crud_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE ai_crud_config SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_crud_config' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_crud_config ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = ''0'' THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_crud_config' AND INDEX_NAME = 'uk_config_key');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE ai_crud_config DROP INDEX uk_config_key',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_crud_config' AND INDEX_NAME = 'uk_config_key_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_config_key_active ON ai_crud_config (config_key, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_message_template.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_template');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_template' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_message_template ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）'' AFTER create_dept',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_template' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_message_template SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_template' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_message_template ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_template' AND INDEX_NAME = 'uk_tenant_code');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sys_message_template DROP INDEX uk_tenant_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_template' AND INDEX_NAME = 'uk_tenant_code_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_tenant_code_active ON sys_message_template (tenant_id, template_code, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_message_biz_type.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_biz_type');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_biz_type' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_message_biz_type ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）'' AFTER create_dept',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_biz_type' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_message_biz_type SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_biz_type' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_message_biz_type ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_biz_type' AND INDEX_NAME = 'uk_tenant_type');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sys_message_biz_type DROP INDEX uk_tenant_type',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_biz_type' AND INDEX_NAME = 'uk_tenant_type_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_tenant_type_active ON sys_message_biz_type (tenant_id, biz_type, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_job_config.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_job_config');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_job_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_job_config ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）'' AFTER update_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_job_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_job_config SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_job_config' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_job_config ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_job_config' AND INDEX_NAME = 'uk_job_name_group');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sys_job_config DROP INDEX uk_job_name_group',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_job_config' AND INDEX_NAME = 'uk_job_name_group_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_job_name_group_active ON sys_job_config (job_name, job_group, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_job_log.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_job_log');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_job_log' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_job_log ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）'' AFTER retry_count',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_job_log' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_job_log SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
