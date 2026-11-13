-- 定时任务开放 API 服务账号、幂等记录、字典与管理权限。

CREATE TABLE IF NOT EXISTS `sys_job_api_token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `caller_name` varchar(100) NOT NULL COMMENT '调用方名称',
  `caller_description` varchar(500) DEFAULT NULL COMMENT '调用方说明',
  `token_key_id` varchar(22) NOT NULL COMMENT 'Token不可猜测Key ID',
  `token_prefix` varchar(32) NOT NULL COMMENT 'Token展示前缀',
  `token_hash` char(64) NOT NULL COMMENT 'Token HMAC-SHA256 Hash',
  `scopes` varchar(255) NOT NULL COMMENT '空格分隔Scope',
  `resource_job_ids` json NOT NULL COMMENT '允许访问的任务ID JSON数组',
  `resource_job_groups` json NOT NULL COMMENT '允许访问的任务组 JSON数组',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/REVOKED',
  `issued_at` datetime NOT NULL COMMENT '签发时间',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `last_used_at` datetime DEFAULT NULL COMMENT '最后使用时间',
  `revoked_at` datetime DEFAULT NULL COMMENT '吊销时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0正常 1删除',
  `logic_delete_active` tinyint GENERATED ALWAYS AS (IF(`del_flag` = 0, 1, NULL)) STORED COMMENT '未删除唯一键标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_api_token_key_active` (`token_key_id`, `logic_delete_active`),
  KEY `idx_job_api_token_tenant_status` (`tenant_id`, `status`, `del_flag`),
  KEY `idx_job_api_token_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务开放API服务账号Token';

CREATE TABLE IF NOT EXISTS `sys_job_api_idempotency` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `token_id` bigint NOT NULL COMMENT '服务账号Token ID',
  `job_config_id` bigint NOT NULL COMMENT '任务配置ID',
  `idempotency_key_hash` char(64) NOT NULL COMMENT '幂等键SHA-256 Hash',
  `execution_id` bigint NOT NULL COMMENT '预留执行记录ID',
  `expires_at` datetime NOT NULL COMMENT '幂等记录过期时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0正常 1删除',
  `logic_delete_active` tinyint GENERATED ALWAYS AS (IF(`del_flag` = 0, 1, NULL)) STORED COMMENT '未删除唯一键标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_api_idempotency_active` (`tenant_id`, `token_id`, `job_config_id`, `idempotency_key_hash`, `logic_delete_active`),
  KEY `idx_job_api_idempotency_execution` (`execution_id`),
  KEY `idx_job_api_idempotency_expires_at` (`expires_at`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务开放API幂等记录';

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '任务开放API Token状态' dict_name,
         'sys_job_api_token_status' dict_type, '服务账号Token状态' remark
  UNION ALL
  SELECT 1, '任务开放API Scope', 'sys_job_api_scope', '服务账号Token授权范围'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '生效中' dict_label, 'ACTIVE' dict_value,
         'sys_job_api_token_status' dict_type, 'success' list_class, 'Y' is_default, 'Token当前有效' remark
  UNION ALL SELECT 1, 2, '已吊销', 'REVOKED', 'sys_job_api_token_status', 'error', 'N', 'Token已被主动吊销'
  UNION ALL SELECT 1, 3, '已过期', 'EXPIRED', 'sys_job_api_token_status', 'warning', 'N', 'Token已超过有效期'
  UNION ALL SELECT 1, 1, '读取任务', 'jobs:read', 'sys_job_api_scope', 'info', 'Y', '查询授权任务摘要'
  UNION ALL SELECT 1, 2, '触发任务', 'jobs:trigger', 'sys_job_api_scope', 'warning', 'N', '幂等触发授权任务'
  UNION ALL SELECT 1, 3, '读取执行', 'executions:read', 'sys_job_api_scope', 'success', 'N', '查询授权任务执行状态'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 5, '已接受', '4', 'sys_job_log_status',
       NULL, 'info', 'N', 1, '开放API已预留执行记录，等待Quartz启动',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = 1
    AND data.dict_type = 'sys_job_log_status'
    AND data.dict_value = '4'
);

SET @job_config_menu_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/system/job-config'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '开放API服务账号', @job_config_menu_id, 2, 92,
       '/system/job-api-token', 'system/job-api-token',
       0, 0, NULL, '_self', 0, 1, 0, NULL,
       'ionicons5:KeyOutline', 0, 0, NULL,
       '定时任务开放API服务账号隐藏路由',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @job_config_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource
    WHERE resource.tenant_id = 1
      AND resource.resource_type = 2
      AND resource.path = '/system/job-api-token'
      AND resource.del_flag = 0
  );

SET @job_api_token_route_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/system/job-api-token'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, role_resource.role_id, @job_api_token_route_id, NOW()
FROM sys_role_resource role_resource
WHERE @job_api_token_route_id IS NOT NULL
  AND role_resource.tenant_id = 1
  AND role_resource.resource_id = @job_config_menu_id
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = role_resource.role_id
      AND existing.resource_id = @job_api_token_route_id
  );

-- 只创建可分配的管理权限，不自动绑定普通角色。
INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, @job_api_token_route_id, seed.resource_type, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '查看开放API服务账号' resource_name, 3 resource_type, 1 sort,
         'system:jobApiToken:list' perms, NULL api_method, NULL api_url, '查看服务账号Token列表和资源选项' remark
  UNION ALL SELECT 1, '创建开放API服务账号', 3, 2, 'system:jobApiToken:add', NULL, NULL, '创建服务账号Token'
  UNION ALL SELECT 1, '吊销开放API服务账号', 3, 3, 'system:jobApiToken:revoke', NULL, NULL, '吊销服务账号Token'
  UNION ALL SELECT 1, '轮换开放API服务账号', 3, 4, 'system:jobApiToken:rotate', NULL, NULL, '轮换服务账号Token'
  UNION ALL SELECT 1, '开放API服务账号查询接口', 4, 101, 'system:jobApiToken:api:list', 'GET', '/job/api-token/**', '查询服务账号Token接口'
  UNION ALL SELECT 1, '开放API服务账号创建接口', 4, 102, 'system:jobApiToken:api:add', 'POST', '/job/api-token', '创建服务账号Token接口'
  UNION ALL SELECT 1, '开放API服务账号吊销接口', 4, 103, 'system:jobApiToken:api:revoke', 'POST', '/job/api-token/*/revoke', '吊销服务账号Token接口'
  UNION ALL SELECT 1, '开放API服务账号轮换接口', 4, 104, 'system:jobApiToken:api:rotate', 'POST', '/job/api-token/*/rotate', '轮换服务账号Token接口'
) seed
WHERE @job_api_token_route_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource
    WHERE resource.tenant_id = seed.tenant_id
      AND resource.resource_type = seed.resource_type
      AND resource.perms = seed.perms
      AND resource.del_flag = 0
  );
