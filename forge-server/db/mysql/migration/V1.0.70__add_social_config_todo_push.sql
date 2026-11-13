-- 企业协同连接支持待办卡片推送配置（企微 textcard，点击跳转 H5 待办详情）
-- 配置收敛到连接管理，替代原 forge.flow.todo-push 配置文件方式

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'todo_push_enabled');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN todo_push_enabled tinyint NOT NULL DEFAULT 0 COMMENT ''待办卡片推送开关：1开启 0关闭'' AFTER api_base_url', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'todo_push_h5_url');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN todo_push_h5_url varchar(512) DEFAULT NULL COMMENT ''待办H5访问地址：须在平台可信域名内，用于拼接待办详情深链'' AFTER todo_push_enabled', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
