-- ----------------------------
-- API配置管理表
-- ----------------------------
CREATE TABLE `sys_api_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  -- 基础信息
  `api_name` varchar(100) NOT NULL COMMENT '接口名称 (如:查询用户信息)',
  `api_code` varchar(100) DEFAULT NULL COMMENT '接口编码 (用于程序逻辑引用)',
  `req_method` varchar(20) NOT NULL COMMENT '请求方式 (GET, POST, PUT, DELETE, ALL)',
  `url_path` varchar(255) NOT NULL COMMENT '接口请求路径 (支持Ant风格, 如 /api/user/**)',
  `api_version` varchar(20) DEFAULT 'v1.0.0' COMMENT '接口版本号',
  -- 业务归属
  `module_code` varchar(50) NOT NULL COMMENT '所属业务模块 (如: sys, order, pay)',
  `service_id` varchar(100) DEFAULT NULL COMMENT '所属微服务ID (若为微服务架构)',
  -- 核心控制开关 (1-开启, 0-关闭)
  `auth_flag` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否需认证/鉴权 (Token校验)',
  `encrypt_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否需报文加解密 (敏感数据保护)',
  `tenant_flag` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用租户隔离 (数据权限)',
  `limit_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否开启限流',
  -- 扩展控制
  `sensitive_fields` text DEFAULT NULL COMMENT '需脱敏字段 (JSON数组存储, 如 ["phone", "id_card"])',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 (1-正常, 0-停用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  -- 审计字段
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_method_url` (`url_path`, `req_method`) USING BTREE COMMENT '路径与方法组合唯一索引',
  KEY `idx_module` (`module_code`) USING BTREE COMMENT '模块查询索引',
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE COMMENT '租户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='REST接口配置管理表';

-- ----------------------------
-- 字典数据
-- ----------------------------
-- 请求方式字典
INSERT INTO `sys_dict_type` (`dict_type`, `dict_name`, `dict_sort`, `status`, `remark`)
VALUES ('req_method', '请求方式', 100, 1, 'API请求方式字典');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `remark`)
VALUES
('req_method', 'GET', 'GET', 1, 1, 'GET请求'),
('req_method', 'POST', 'POST', 2, 1, 'POST请求'),
('req_method', 'PUT', 'PUT', 3, 1, 'PUT请求'),
('req_method', 'DELETE', 'DELETE', 4, 1, 'DELETE请求'),
('req_method', 'PATCH', 'PATCH', 5, 1, 'PATCH请求'),
('req_method', 'ALL', 'ALL', 6, 1, '所有请求方式');

-- 模块编码字典
INSERT INTO `sys_dict_type` (`dict_type`, `dict_name`, `dict_sort`, `status`, `remark`)
VALUES ('module_code', '业务模块', 101, 1, '业务模块编码字典');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `remark`)
VALUES
('module_code', '系统管理', 'sys', 1, 1, '系统管理模块'),
('module_code', '用户管理', 'user', 2, 1, '用户管理模块'),
('module_code', '角色管理', 'role', 3, 1, '角色管理模块'),
('module_code', '菜单管理', 'menu', 4, 1, '菜单管理模块'),
('module_code', '部门管理', 'dept', 5, 1, '部门管理模块'),
('module_code', '岗位管理', 'post', 6, 1, '岗位管理模块'),
('module_code', '字典管理', 'dict', 7, 1, '字典管理模块'),
('module_code', '参数管理', 'config', 8, 1, '参数管理模块'),
('module_code', '通知管理', 'notice', 9, 1, '通知管理模块'),
('module_code', '日志管理', 'log', 10, 1, '日志管理模块'),
('module_code', '文件管理', 'file', 11, 1, '文件管理模块'),
('module_code', '任务管理', 'job', 12, 1, '任务管理模块'),
('module_code', '数据权限', 'datascope', 13, 1, '数据权限模块'),
('module_code', 'API配置', 'apiconfig', 14, 1, 'API配置管理模块');
