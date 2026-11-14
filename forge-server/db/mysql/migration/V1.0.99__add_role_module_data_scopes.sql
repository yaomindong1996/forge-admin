-- 角色授权支持按业务模块覆盖数据范围。

CREATE TABLE IF NOT EXISTS `sys_role_module_data_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `module_code` varchar(100) NOT NULL COMMENT '业务模块编码，对应 sys_data_scope_config.resource_code',
  `data_scope` tinyint NOT NULL COMMENT '数据范围（1全部，2本租户，3本组织，4本组织及下级，5本人，7行政区划）',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_role_module` (`tenant_id`, `role_id`, `module_code`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_tenant_module_scope` (`tenant_id`, `module_code`, `data_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色业务模块数据范围覆盖表';

SET @role_menu_id := COALESCE(
  (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND perms = 'system:role:list'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
  ),
  0
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  api_method, api_url, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @role_menu_id, 4, seed.sort,
       0, '_self', 0, 1, 1, seed.perms,
       seed.api_method, seed.api_url, 0, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 13 sort, '角色数据权限查询API' resource_name, 'system:role:api:data-scopes-detail' perms,
         'GET' api_method, '/system/role/*/dataScopes' api_url, '查询角色默认及模块数据范围' remark
  UNION ALL
  SELECT 14, '角色数据权限保存API', 'system:role:api:data-scopes-save',
         'POST', '/system/role/*/dataScopes', '保存角色默认及模块数据范围'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource existing_resource
  WHERE existing_resource.tenant_id = 1
    AND existing_resource.resource_type = 4
    AND existing_resource.perms = seed.perms
    AND existing_resource.del_flag = 0
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT source_grant.tenant_id, source_grant.role_id, target_resource.id, NOW()
FROM sys_role_resource source_grant
INNER JOIN sys_resource source_resource
        ON source_resource.tenant_id = source_grant.tenant_id
       AND source_resource.id = source_grant.resource_id
       AND source_resource.del_flag = 0
INNER JOIN sys_resource target_resource
        ON target_resource.tenant_id = source_grant.tenant_id
       AND target_resource.resource_type = 4
       AND target_resource.del_flag = 0
       AND source_resource.perms = CASE
         WHEN target_resource.perms = 'system:role:api:data-scopes-detail' THEN 'system:role:query'
         WHEN target_resource.perms = 'system:role:api:data-scopes-save' THEN 'system:role:edit'
         ELSE NULL
       END
WHERE source_grant.tenant_id = 1
  AND source_resource.perms IN ('system:role:query', 'system:role:edit')
  AND target_resource.perms IN (
    'system:role:api:data-scopes-detail',
    'system:role:api:data-scopes-save'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing_grant
    WHERE existing_grant.tenant_id = source_grant.tenant_id
      AND existing_grant.role_id = source_grant.role_id
      AND existing_grant.resource_id = target_resource.id
  );
