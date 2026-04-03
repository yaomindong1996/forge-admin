-- 三方登录配置表
CREATE TABLE `sys_social_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform` varchar(50) NOT NULL COMMENT '平台类型（WECHAT、DINGTALK、GITHUB、GITEE等）',
  `platform_name` varchar(100) DEFAULT NULL COMMENT '平台名称',
  `platform_logo` varchar(500) DEFAULT NULL COMMENT '平台Logo',
  `client_id` varchar(255) NOT NULL COMMENT '应用ID/Key',
  `client_secret` varchar(255) NOT NULL COMMENT '应用Secret',
  `redirect_uri` varchar(500) DEFAULT NULL COMMENT '回调地址',
  `agent_id` varchar(100) DEFAULT NULL COMMENT '企业微信AgentId',
  `scope` varchar(500) DEFAULT NULL COMMENT '授权范围',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态（1-启用，0-停用）',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_tenant` (`platform`, `tenant_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三方登录配置表';

-- 用户三方账号绑定表
CREATE TABLE `sys_user_social` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `platform` varchar(50) NOT NULL COMMENT '平台类型',
  `uuid` varchar(255) NOT NULL COMMENT '第三方用户唯一标识',
  `username` varchar(100) DEFAULT NULL COMMENT '第三方用户名',
  `nickname` varchar(100) DEFAULT NULL COMMENT '第三方昵称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `access_token` varchar(500) DEFAULT NULL COMMENT '访问令牌',
  `refresh_token` varchar(500) DEFAULT NULL COMMENT '刷新令牌',
  `expire_time` datetime DEFAULT NULL COMMENT '令牌过期时间',
  `bind_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_uuid` (`platform`, `uuid`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户三方账号绑定表';
