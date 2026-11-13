-- V1.0.104: ai_crud_config 增加菜单挂载位置字段 mount_target
-- 支持低代码页面挂载到管理端（ADMIN）、移动端（MOBILE）或两端同时（BOTH）

-- 增加 mount_target 字段（如果不存在）
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_crud_config'
      AND COLUMN_NAME = 'mount_target'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE ai_crud_config ADD COLUMN mount_target VARCHAR(16) DEFAULT ''ADMIN'' COMMENT ''菜单挂载位置：ADMIN-管理端，MOBILE-移动端，BOTH-两端同时''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
