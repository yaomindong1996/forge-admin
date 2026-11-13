-- 流程用户组管理：租户内维护稳定 group_code、成员关系和运行时解析边界。
-- 业务数据使用 tenant_id=1 作为种子约定；运行时按当前租户过滤。

CREATE TABLE IF NOT EXISTS `sys_flow_user_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `group_code` varchar(100) NOT NULL COMMENT '用户组编码，作为 candidateGroups 稳定引用',
  `group_name` varchar(100) NOT NULL COMMENT '用户组名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0禁用，1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建组织',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记，删除后写当前行主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_user_group_code` (`tenant_id`, `group_code`, `del_flag`),
  KEY `idx_flow_user_group_status` (`tenant_id`, `status`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程用户组';

CREATE TABLE IF NOT EXISTS `sys_flow_user_group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `group_id` bigint NOT NULL COMMENT '用户组ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0失效，1有效',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建组织',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记，删除后写当前行主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_user_group_member` (`tenant_id`, `group_id`, `user_id`, `del_flag`),
  KEY `idx_flow_user_group_member_user` (`tenant_id`, `user_id`, `status`, `del_flag`),
  KEY `idx_flow_user_group_member_group` (`tenant_id`, `group_id`, `status`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程用户组成员';

SET @flow_menu_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/flow' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '流程用户组', @flow_menu_id, 2, 40,
       '/flow/userGroup', 'flow/userGroup', 0, 0, NULL,
       '_self', 0, 1, 1, 'flow:org:group:view', 'ionicons5:People',
       NULL, NULL, 0, 0, NULL, '维护流程 candidateGroups 用户组及成员',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @flow_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.path = '/flow/userGroup' AND r.del_flag = 0
  );

SET @flow_group_menu_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND path = '/flow/userGroup' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, @flow_group_menu_id, 3, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '维护流程用户组' resource_name, 1 sort,
         'flow:org:group:manage' perms, '新增、编辑、删除用户组并维护成员' remark
) seed
WHERE @flow_group_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.resource_type = 3
      AND r.perms = seed.perms AND r.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, @flow_group_menu_id, 4, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '查询流程用户组接口' resource_name, 10 sort,
         'flow:org:group:api:read' perms, 'GET' api_method,
         '/api/flow/org/groups/**' api_url, '查询当前租户用户组和成员' remark
  UNION ALL
  SELECT 1, '维护流程用户组接口', 11,
         'flow:org:group:api:write', 'POST', '/api/flow/org/groups/**', '写入当前租户用户组和成员'
  UNION ALL
  SELECT 1, '修改流程用户组接口', 12,
         'flow:org:group:api:update', 'PUT', '/api/flow/org/groups/**', '修改当前租户用户组'
  UNION ALL
  SELECT 1, '删除流程用户组接口', 13,
         'flow:org:group:api:delete', 'DELETE', '/api/flow/org/groups/**', '逻辑删除当前租户用户组或成员'
) seed
WHERE @flow_group_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id AND r.resource_type = 4
      AND r.perms = seed.perms AND r.del_flag = 0
  );
