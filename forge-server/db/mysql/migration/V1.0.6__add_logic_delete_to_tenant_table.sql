-- 为 Batch 2.3b-1 租户表补齐逻辑删除契约。
-- 说明：
-- 1. 本批只处理 sys_tenant，用户、角色、菜单资源继续拆到后续小批次。
-- 2. 关系表仍保持物理删除；租户删除入口会先校验用户和用户租户绑定。
-- 3. 租户名称唯一索引使用 logic_delete_active 生成列，仅约束未删除记录。

-- sys_tenant.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_tenant ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_tenant SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_tenant ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND INDEX_NAME = 'uk_tenant_name');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sys_tenant DROP INDEX uk_tenant_name',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND INDEX_NAME = 'uk_tenant_name_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_tenant_name_active ON sys_tenant (tenant_name, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
