-- 需求2：企业协同连接新增“工作台免登”开关，替代前端写死的 VITE_WECOM_CONNECTION_CODE
-- 需求3：消息落库企业协同平台编码，便于在消息/投递记录中区分钉钉/飞书/企业微信

-- 1. sys_social_config 新增工作台免登开关（1开启 0关闭）
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'sso_workbench_enabled');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN sso_workbench_enabled tinyint NOT NULL DEFAULT 0 COMMENT ''工作台免登开关：1开启 0关闭；开启后客户端工作台可用该连接的 connection_code 免登'' AFTER todo_push_h5_url', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. sys_message 新增企业协同平台编码（与 sys_collab_platform 字典一致，非协同渠道为空）
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message' AND COLUMN_NAME = 'platform');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message ADD COLUMN platform varchar(50) DEFAULT NULL COMMENT ''企业协同平台编码：WECHAT_ENTERPRISE/DINGTALK/FEISHU 等，非协同渠道为空'' AFTER connection_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. sys_message_send_record 新增企业协同平台编码
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_send_record' AND COLUMN_NAME = 'platform');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message_send_record ADD COLUMN platform varchar(50) DEFAULT NULL COMMENT ''企业协同平台编码：WECHAT_ENTERPRISE/DINGTALK/FEISHU 等，非协同渠道为空'' AFTER connection_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
