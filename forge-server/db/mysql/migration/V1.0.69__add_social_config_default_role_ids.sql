-- =====================================================================
-- V1.0.69 : sys_social_config 新增 default_role_ids 字段
-- 用途：连接级默认角色配置，OAuth 免登/目录同步自动建号时自动分配
-- =====================================================================

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'sys_social_config'
              AND COLUMN_NAME = 'default_role_ids');
SET @sql = IF(@col = 0,
    'ALTER TABLE sys_social_config ADD COLUMN default_role_ids varchar(512) DEFAULT NULL COMMENT ''自动建号默认角色ID列表（逗号分隔）'' AFTER identity_policy',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
