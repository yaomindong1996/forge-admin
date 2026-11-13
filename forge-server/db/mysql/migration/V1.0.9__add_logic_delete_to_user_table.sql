-- 为 Batch 2.3b-4 用户表补齐逻辑删除契约。
-- 说明：
-- 1. 本批只处理 sys_user 主表，用户租户/组织/岗位/角色关系表仍保持物理删除。
-- 2. 用户删除入口会先清理关系表，再逻辑删除用户主记录。
-- 3. 用户名唯一索引使用 logic_delete_active 生成列，仅约束未删除记录。

-- sys_user.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志（0正常 1删除）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sys_user SET del_flag = 0 WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'sys_user_unique');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sys_user DROP INDEX sys_user_unique',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'sys_user_unique_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX sys_user_unique_active ON sys_user (tenant_id, username, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
