-- 补齐流程中心缺失菜单：流程模板、条件规则（与 MySQL V1.0.235 同语义）。
-- PG 禁用 MySQL 过程式变量；用子查询挂父菜单。

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '流程模板', menu.id, 2, 5,
       '/flow/template', 'flow/template', 0, 0, NULL,
       '_self', 0, 1, 1, 'flow:template:view', 'ionicons5:DocumentTextOutline',
       NULL, NULL, 0, 0, NULL, '维护独立流程模板；已绑定业务应用的流程请从应用中心配置',
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/flow' AND del_flag = 0
  ORDER BY id LIMIT 1
) menu
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.path = '/flow/template' AND r.del_flag = 0
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '条件规则', menu.id, 2, 6,
       '/flow/conditionRule', 'flow/conditionRule', 0, 0, NULL,
       '_self', 0, 1, 1, 'flow:condition-rule:view', 'ionicons5:GitBranchOutline',
       NULL, NULL, 0, 0, NULL, '维护流程网关/连线条件规则',
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/flow' AND del_flag = 0
  ORDER BY id LIMIT 1
) menu
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.path = '/flow/conditionRule' AND r.del_flag = 0
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, menu.id, 3, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1::bigint AS tenant_id, '维护流程模板'::varchar AS resource_name, 1 AS sort,
         'flow:template:manage'::varchar AS perms,
         '新增、编辑、启用/禁用、复制流程模板'::varchar AS remark
) seed
INNER JOIN (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND path = '/flow/template' AND del_flag = 0
  ORDER BY id LIMIT 1
) menu ON TRUE
WHERE NOT EXISTS (
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
SELECT seed.tenant_id, seed.resource_name, menu.id, 3, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1::bigint AS tenant_id, '维护条件规则'::varchar AS resource_name, 1 AS sort,
         'flow:condition-rule:manage'::varchar AS perms,
         '新增、编辑、删除条件规则'::varchar AS remark
) seed
INNER JOIN (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND path = '/flow/conditionRule' AND del_flag = 0
  ORDER BY id LIMIT 1
) menu ON TRUE
WHERE NOT EXISTS (
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
SELECT seed.tenant_id, seed.resource_name, menu.id, 4, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1::bigint AS tenant_id, '查询流程模板接口'::varchar AS resource_name, 10 AS sort,
         'flow:template:api:read'::varchar AS perms, 'GET'::varchar AS api_method,
         '/api/flow/template/**'::varchar AS api_url, '分页与详情查询流程模板'::varchar AS remark
  UNION ALL
  SELECT 1, '写入流程模板接口', 11,
         'flow:template:api:write', 'POST', '/api/flow/template/**', '创建、启用、禁用、复制模板及从模板建模型'
  UNION ALL
  SELECT 1, '修改流程模板接口', 12,
         'flow:template:api:update', 'PUT', '/api/flow/template', '更新流程模板'
  UNION ALL
  SELECT 1, '删除流程模板接口', 13,
         'flow:template:api:delete', 'DELETE', '/api/flow/template/**', '删除流程模板'
) seed
INNER JOIN (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND path = '/flow/template' AND del_flag = 0
  ORDER BY id LIMIT 1
) menu ON TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = seed.tenant_id AND r.resource_type = 4
    AND r.perms = seed.perms AND r.del_flag = 0
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, menu.id, 4, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1::bigint AS tenant_id, '查询条件规则接口'::varchar AS resource_name, 10 AS sort,
         'flow:condition-rule:api:read'::varchar AS perms, 'GET'::varchar AS api_method,
         '/api/flow/conditionRule/**'::varchar AS api_url, '分页与详情查询条件规则'::varchar AS remark
  UNION ALL
  SELECT 1, '写入条件规则接口', 11,
         'flow:condition-rule:api:write', 'POST', '/api/flow/conditionRule/**', '新增与测试条件规则'
  UNION ALL
  SELECT 1, '修改条件规则接口', 12,
         'flow:condition-rule:api:update', 'PUT', '/api/flow/conditionRule', '修改条件规则'
  UNION ALL
  SELECT 1, '删除条件规则接口', 13,
         'flow:condition-rule:api:delete', 'DELETE', '/api/flow/conditionRule/**', '删除条件规则'
) seed
INNER JOIN (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND path = '/flow/conditionRule' AND del_flag = 0
  ORDER BY id LIMIT 1
) menu ON TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = seed.tenant_id AND r.resource_type = 4
    AND r.perms = seed.perms AND r.del_flag = 0
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT seed.tenant_id, seed.role_id, target.id, NOW()
FROM (
  SELECT rr.tenant_id, rr.role_id
  FROM sys_role_resource rr
  INNER JOIN sys_resource r
    ON r.id = rr.resource_id
   AND r.tenant_id = rr.tenant_id
   AND r.del_flag = 0
   AND (
     r.path IN ('/flow', '/flow/model', '/flow/category', '/flow/monitor')
     OR r.perms IN ('flow:model:list', 'flow:model:view')
   )
) seed
INNER JOIN sys_resource target
  ON target.tenant_id = seed.tenant_id
 AND target.del_flag = 0
 AND (
   target.path IN ('/flow/template', '/flow/conditionRule')
   OR target.perms IN (
     'flow:template:view', 'flow:template:manage',
     'flow:template:api:read', 'flow:template:api:write',
     'flow:template:api:update', 'flow:template:api:delete',
     'flow:condition-rule:view', 'flow:condition-rule:manage',
     'flow:condition-rule:api:read', 'flow:condition-rule:api:write',
     'flow:condition-rule:api:update', 'flow:condition-rule:api:delete'
   )
 )
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_resource existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.role_id = seed.role_id
    AND existing.resource_id = target.id
);
