-- 外部系统配置表
CREATE TABLE `sys_external_system` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  
  `system_code` varchar(50) NOT NULL COMMENT '系统编码（租户内唯一）',
  `system_name` varchar(100) NOT NULL COMMENT '系统名称',
  `system_desc` varchar(500) DEFAULT NULL COMMENT '系统描述',
  
  `base_url` varchar(255) NOT NULL COMMENT '基础URL',
  `auth_type` varchar(20) NOT NULL COMMENT '认证类型（none/basic/token/current_token/oauth2/api_key/custom）',
  
  -- Basic认证配置
  `basic_username` varchar(100) DEFAULT NULL COMMENT 'Basic认证用户名',
  `basic_password` varchar(200) DEFAULT NULL COMMENT 'Basic认证密码（加密存储）',
  
  -- Token认证配置
  `token_value` varchar(500) DEFAULT NULL COMMENT 'Token值（加密存储）',
  `token_header_name` varchar(50) DEFAULT 'Authorization' COMMENT 'Token请求头名称',
  `token_prefix` varchar(20) DEFAULT 'Bearer' COMMENT 'Token前缀',
  
  -- OAuth2认证配置
  `oauth2_token_url` varchar(255) DEFAULT NULL COMMENT 'OAuth2获取Token的URL',
  `oauth2_client_id` varchar(100) DEFAULT NULL COMMENT 'OAuth2客户端ID',
  `oauth2_client_secret` varchar(200) DEFAULT NULL COMMENT 'OAuth2客户端密钥（加密存储）',
  `oauth2_grant_type` varchar(20) DEFAULT 'client_credentials' COMMENT 'OAuth2授权类型',
  `oauth2_scope` varchar(100) DEFAULT NULL COMMENT 'OAuth2授权范围',
  
  -- API Key认证配置
  `api_key_name` varchar(50) DEFAULT NULL COMMENT 'API Key参数名',
  `api_key_value` varchar(200) DEFAULT NULL COMMENT 'API Key值（加密存储）',
  `api_key_position` varchar(20) DEFAULT 'header' COMMENT 'API Key位置（header/query/body）',
  
  -- 自定义认证配置
  `custom_auth_adapter` varchar(100) DEFAULT NULL COMMENT '自定义认证适配器字典值（external_auth_adapter）',
  `custom_auth_config` text DEFAULT NULL COMMENT '自定义认证配置（JSON）',
  `trusted_internal` tinyint(1) DEFAULT 0 COMMENT '是否可信内部Forge系统',
  
  -- 代理配置
  `proxy_enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用代理',
  `proxy_host` varchar(100) DEFAULT NULL COMMENT '代理主机',
  `proxy_port` int DEFAULT NULL COMMENT '代理端口',
  `proxy_username` varchar(100) DEFAULT NULL COMMENT '代理用户名',
  `proxy_password` varchar(200) DEFAULT NULL COMMENT '代理密码（加密存储）',
  
  -- 重试配置
  `retry_enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用重试',
  `retry_max_attempts` int DEFAULT 3 COMMENT '最大重试次数',
  `retry_backoff_interval` int DEFAULT 1000 COMMENT '重试间隔（毫秒）',
  
  -- 超时配置
  `connect_timeout` int DEFAULT 5000 COMMENT '连接超时（毫秒）',
  `read_timeout` int DEFAULT 10000 COMMENT '读取超时（毫秒）',
  `write_timeout` int DEFAULT 10000 COMMENT '写入超时（毫秒）',
  
  -- 其他配置
  `ssl_verify_enabled` tinyint(1) DEFAULT 1 COMMENT '是否验证SSL证书',
  `request_logging_enabled` tinyint(1) DEFAULT 1 COMMENT '是否记录请求日志',
  
  `system_status` tinyint DEFAULT 1 COMMENT '系统状态（0-禁用 1-启用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_code_tenant` (`system_code`, `tenant_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_system_status` (`system_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部系统配置表';

-- 外部接口配置表
CREATE TABLE `sys_external_api` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  
  `system_id` bigint NOT NULL COMMENT '所属外部系统ID',
  `api_code` varchar(50) NOT NULL COMMENT '接口编码（系统内唯一）',
  `api_name` varchar(100) NOT NULL COMMENT '接口名称',
  `api_desc` varchar(500) DEFAULT NULL COMMENT '接口描述',
  
  `api_path` varchar(255) NOT NULL COMMENT '接口路径（相对路径）',
  `api_method` varchar(10) NOT NULL COMMENT '请求方法（GET/POST/PUT/DELETE）',
  
  -- 请求配置
  `request_content_type` varchar(50) DEFAULT 'application/json' COMMENT '请求内容类型',
  `request_headers` text DEFAULT NULL COMMENT '额外请求头（JSON）',
  `request_params` text DEFAULT NULL COMMENT '固定请求参数（JSON）',
  `request_body_template` text DEFAULT NULL COMMENT '请求体模板（JSON）',
  
  -- 响应配置
  `response_content_type` varchar(50) DEFAULT 'application/json' COMMENT '响应内容类型',
  `response_data_path` varchar(100) DEFAULT NULL COMMENT '响应数据提取路径（如 data.items）',
  `response_total_path` varchar(100) DEFAULT NULL COMMENT '响应总数提取路径（用于分页）',
  
  -- 参数映射配置
  `param_mapping_enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用参数映射',
  `param_mappings` text DEFAULT NULL COMMENT '参数映射规则（JSON）',
  
  -- 响应转换配置
  `response_transform_enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用响应转换',
  `response_transform_script` text DEFAULT NULL COMMENT '响应转换脚本（JavaScript）',
  
  -- 错误处理配置
  `error_code_path` varchar(100) DEFAULT NULL COMMENT '错误码提取路径',
  `error_msg_path` varchar(100) DEFAULT NULL COMMENT '错误消息提取路径',
  `success_codes` varchar(100) DEFAULT '0,200' COMMENT '成功状态码列表',

  -- 接口文档
  `doc_file_id` varchar(100) DEFAULT NULL COMMENT '接口文档文件ID',
  `doc_file_name` varchar(255) DEFAULT NULL COMMENT '接口文档文件名',
  
  -- 限流配置
  `rate_limit_enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用限流',
  `rate_limit_qps` int DEFAULT 10 COMMENT '限流QPS',
  
  -- 缓存配置
  `cache_enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用缓存',
  `cache_ttl` int DEFAULT 300 COMMENT '缓存时长（秒）',
  `cache_key_template` varchar(100) DEFAULT NULL COMMENT '缓存键模板',
  
  -- 权限配置
  `permission_check_enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用权限检查',
  `required_permission` varchar(100) DEFAULT NULL COMMENT '所需权限标识',
  
  `api_status` tinyint DEFAULT 1 COMMENT '接口状态（0-禁用 1-启用）',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_code_system` (`api_code`, `system_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_system_id` (`system_id`),
  KEY `idx_api_status` (`api_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部接口配置表';

-- 外部接口调用日志表
CREATE TABLE `sys_external_api_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `system_id` bigint DEFAULT NULL COMMENT '外部系统ID',
  `api_id` bigint NOT NULL COMMENT '外部接口ID',
  `system_name` varchar(100) DEFAULT NULL COMMENT '系统名称快照',
  `api_name` varchar(100) DEFAULT NULL COMMENT '接口名称快照',
  `api_code` varchar(50) DEFAULT NULL COMMENT '接口编码快照',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(1000) DEFAULT NULL COMMENT '请求URL',
  `request_params` text DEFAULT NULL COMMENT '请求参数',
  `request_body` text DEFAULT NULL COMMENT '请求体',
  `response_body` text DEFAULT NULL COMMENT '响应体',
  `http_status_code` int DEFAULT NULL COMMENT 'HTTP状态码',
  `call_status` tinyint DEFAULT 1 COMMENT '调用状态（0失败 1成功）',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `debug_flag` tinyint(1) DEFAULT 0 COMMENT '是否调试调用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_api` (`tenant_id`, `api_id`),
  KEY `idx_system_id` (`system_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部接口调用日志表';

-- 外部系统自定义认证适配器字典类型
-- 后端每实现一个自定义认证逻辑，在 sys_dict_data 中维护对应的 dict_value。
INSERT IGNORE INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
VALUES (1, '外部认证适配器', 'external_auth_adapter', 1, '外部系统自定义认证适配器字典', NOW(), NOW());

INSERT IGNORE INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, dict_status, remark, create_time, update_time)
VALUES (1, 1, '请求头映射', 'header_map', 'external_auth_adapter', 1, '将自定义认证配置中的 KEY:VALUE 写入请求头', NOW(), NOW());
