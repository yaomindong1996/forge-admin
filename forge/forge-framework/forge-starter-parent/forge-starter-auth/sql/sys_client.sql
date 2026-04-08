CREATE TABLE `sys_client` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_code` varchar(50) NOT NULL COMMENT '客户端编码',
  `client_name` varchar(100) NOT NULL COMMENT '客户端名称',
  `app_id` varchar(64) NOT NULL COMMENT '应用ID',
  `app_secret` varchar(128) NOT NULL COMMENT '应用密钥（加密存储）',
  
  `token_timeout` bigint DEFAULT 7200 COMMENT 'Token有效期（秒）',
  `token_activity_timeout` bigint DEFAULT -1 COMMENT 'Token活跃超时（秒）',
  `token_prefix` varchar(20) DEFAULT 'Bearer' COMMENT 'Token前缀',
  `token_name` varchar(50) DEFAULT 'Authorization' COMMENT 'Token名称',
  `concurrent_login` tinyint(1) DEFAULT 0 COMMENT '是否允许并发登录（0-否 1-是）',
  `share_token` tinyint(1) DEFAULT 0 COMMENT '是否共享Token（0-否 1-是）',
  
  `enable_ip_limit` tinyint(1) DEFAULT 0 COMMENT '是否启用IP限制',
  `ip_whitelist` text COMMENT 'IP白名单（JSON数组）',
  `enable_encrypt` tinyint(1) DEFAULT 0 COMMENT '是否启用加密传输',
  `encrypt_algorithm` varchar(50) DEFAULT 'AES' COMMENT '加密算法',
  
  `max_user_count` bigint DEFAULT -1 COMMENT '最大用户数（-1不限）',
  `max_online_count` bigint DEFAULT -1 COMMENT '最大在线数（-1不限）',
  `auth_types` varchar(255) DEFAULT 'password' COMMENT '支持的认证方式（逗号分隔）',
  
  `status` tinyint DEFAULT 1 COMMENT '状态（0-禁用 1-启用）',
  `description` varchar(500) COMMENT '客户端描述',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_by` bigint,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_by` bigint,
  `create_dept` bigint,
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_code` (`client_code`),
  UNIQUE KEY `uk_app_id` (`app_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端管理表';