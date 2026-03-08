-- 配置属性表
CREATE TABLE IF NOT EXISTS `config_properties` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `key` VARCHAR(200) NOT NULL COMMENT '配置键',
  `value` TEXT COMMENT '配置值',
  `description` VARCHAR(500) COMMENT '描述',
  `group` VARCHAR(100) DEFAULT 'DEFAULT_GROUP' COMMENT '配置分组',
  `type` VARCHAR(50) DEFAULT 'STRING' COMMENT '值类型(STRING/NUMBER/BOOLEAN/JSON)',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) COMMENT '创建人',
  `updater` VARCHAR(64) COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`key`),
  KEY `idx_group` (`group`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置属性表';

-- 插入示例配置
INSERT INTO config_properties (`key`, `value`, `description`, `group`, `type`) VALUES
('app.name', 'Forge Framework', '应用名称', 'APPLICATION', 'STRING'),
('app.version', '1.0.0', '应用版本', 'APPLICATION', 'STRING'),
('feature.user.registration', 'true', '用户注册功能开关', 'FEATURE', 'BOOLEAN'),
('feature.email.notification', 'false', '邮件通知功能开关', 'FEATURE', 'BOOLEAN'),
('system.max.upload.size', '10485760', '最大上传文件大小(字节)', 'SYSTEM', 'NUMBER'),
('system.session.timeout', '1800', '会话超时时间(秒)', 'SYSTEM', 'NUMBER'),
('api.rate.limit', '100', 'API请求速率限制(次/分钟)', 'API', 'NUMBER'),
('cache.redis.enabled', 'true', 'Redis缓存是否启用', 'CACHE', 'BOOLEAN'),
('cache.expire.time', '3600', '缓存过期时间(秒)', 'CACHE', 'NUMBER');
