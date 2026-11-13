-- 企业协同连接支持自定义 API 基础地址（私有化部署企微/API 网关场景）
-- 为空时使用平台官方默认地址（企业微信为 https://qyapi.weixin.qq.com）

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'api_base_url');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN api_base_url varchar(255) DEFAULT NULL COMMENT ''API基础地址：为空使用平台官方地址，私有化部署可自定义'' AFTER default_org_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
