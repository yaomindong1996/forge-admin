-- 为 Batch 3.1 低代码/应用中心设计态主表补齐逻辑删除契约。
-- 说明：
-- 1. 本批只处理低代码和应用中心主设计态表，不处理关系表和触发日志表。
-- 2. 关系表如 ai_business_binding、ai_business_object_relation 仍保持物理删除，避免关系重建产生脏数据。
-- 3. 有业务唯一键的表使用 logic_delete_active 生成列，仅约束未删除记录。

-- ai_business_suite.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_suite');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_suite' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_business_suite ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_suite' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE ai_business_suite SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_suite' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_business_suite ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = ''0'' THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_suite' AND INDEX_NAME = 'uk_ai_business_suite_code');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE ai_business_suite DROP INDEX uk_ai_business_suite_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_suite' AND INDEX_NAME = 'uk_ai_business_suite_code_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_ai_business_suite_code_active ON ai_business_suite (tenant_id, suite_code, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_app.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_app');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_app' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_business_app ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_app' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE ai_business_app SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_app' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_business_app ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = ''0'' THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_app' AND INDEX_NAME = 'uk_ai_business_app_code');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE ai_business_app DROP INDEX uk_ai_business_app_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_app' AND INDEX_NAME = 'uk_ai_business_app_code_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_ai_business_app_code_active ON ai_business_app (tenant_id, app_code, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_object.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_object');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_object' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_business_object ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_object' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE ai_business_object SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_object' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_business_object ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = ''0'' THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_object' AND INDEX_NAME = 'uk_ai_business_object_code');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE ai_business_object DROP INDEX uk_ai_business_object_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_object' AND INDEX_NAME = 'uk_ai_business_object_code_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_ai_business_object_code_active ON ai_business_object (tenant_id, suite_code, object_code, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_trigger.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_business_trigger ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE ai_business_trigger SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_code_rule.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_code_rule ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE ai_code_rule SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_code_rule ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = ''0'' THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND INDEX_NAME = 'uk_ai_code_rule_code');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE ai_code_rule DROP INDEX uk_ai_code_rule_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND INDEX_NAME = 'uk_ai_code_rule_code_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_ai_code_rule_code_active ON ai_code_rule (tenant_id, rule_code, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_lowcode_domain.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_domain');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_domain' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_lowcode_domain ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_domain' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE ai_lowcode_domain SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_domain' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_lowcode_domain ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = ''0'' THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_domain' AND INDEX_NAME = 'uk_ai_lowcode_domain_code');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE ai_lowcode_domain DROP INDEX uk_ai_lowcode_domain_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_domain' AND INDEX_NAME = 'uk_ai_lowcode_domain_code_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_ai_lowcode_domain_code_active ON ai_lowcode_domain (tenant_id, domain_code, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_domain' AND INDEX_NAME = 'uk_ai_lowcode_domain_name');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE ai_lowcode_domain DROP INDEX uk_ai_lowcode_domain_name',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_domain' AND INDEX_NAME = 'uk_ai_lowcode_domain_name_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_ai_lowcode_domain_name_active ON ai_lowcode_domain (tenant_id, parent_id, domain_name, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_lowcode_model.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_model');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_model' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_lowcode_model ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_model' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE ai_lowcode_model SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_model' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE ai_lowcode_model ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = ''0'' THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_model' AND INDEX_NAME = 'uk_ai_lowcode_model_code');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE ai_lowcode_model DROP INDEX uk_ai_lowcode_model_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_lowcode_model' AND INDEX_NAME = 'uk_ai_lowcode_model_code_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_ai_lowcode_model_code_active ON ai_lowcode_model (tenant_id, domain_id, model_code, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
