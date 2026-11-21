-- 补齐流程中心缺失菜单：流程模板、条件规则（SQL Server，与 MySQL/PG V1.0.239 同语义）
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, N'流程模板', menu.id, 2, 5,
       N'/flow/template', N'flow/template', 0, 0, NULL,
       N'_self', 0, 1, 1, N'flow:template:view', N'ionicons5:DocumentTextOutline',
       NULL, NULL, 0, 0, NULL, N'维护独立流程模板；已绑定业务应用的流程请从应用中心配置',
       1, SYSUTCDATETIME(), 1, SYSUTCDATETIME(), 1, N'pc'
FROM (
  SELECT TOP 1 id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = N'/flow' AND del_flag = 0
  ORDER BY id
) menu
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.path = N'/flow/template' AND r.del_flag = 0
);
GO

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, N'条件规则', menu.id, 2, 6,
       N'/flow/conditionRule', N'flow/conditionRule', 0, 0, NULL,
       N'_self', 0, 1, 1, N'flow:condition-rule:view', N'ionicons5:GitBranchOutline',
       NULL, NULL, 0, 0, NULL, N'维护流程网关/连线条件规则',
       1, SYSUTCDATETIME(), 1, SYSUTCDATETIME(), 1, N'pc'
FROM (
  SELECT TOP 1 id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = N'/flow' AND del_flag = 0
  ORDER BY id
) menu
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.path = N'/flow/conditionRule' AND r.del_flag = 0
);
GO

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, menu.id, 3, seed.sort,
       NULL, NULL, 0, 0, NULL, N'_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark,
       1, SYSUTCDATETIME(), 1, SYSUTCDATETIME(), 1, N'pc'
FROM (
  SELECT CAST(1 AS BIGINT) AS tenant_id, N'维护流程模板' AS resource_name, 1 AS sort,
         N'flow:template:manage' AS perms,
         N'新增、编辑、启用/禁用、复制流程模板' AS remark
) seed
INNER JOIN (
  SELECT TOP 1 id FROM sys_resource
  WHERE tenant_id = 1 AND path = N'/flow/template' AND del_flag = 0
  ORDER BY id
) menu ON 1 = 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 3
    AND r.perms = seed.perms AND r.del_flag = 0
);
GO

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, menu.id, 3, seed.sort,
       NULL, NULL, 0, 0, NULL, N'_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark,
       1, SYSUTCDATETIME(), 1, SYSUTCDATETIME(), 1, N'pc'
FROM (
  SELECT CAST(1 AS BIGINT) AS tenant_id, N'维护条件规则' AS resource_name, 1 AS sort,
         N'flow:condition-rule:manage' AS perms,
         N'新增、编辑、删除条件规则' AS remark
) seed
INNER JOIN (
  SELECT TOP 1 id FROM sys_resource
  WHERE tenant_id = 1 AND path = N'/flow/conditionRule' AND del_flag = 0
  ORDER BY id
) menu ON 1 = 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 3
    AND r.perms = seed.perms AND r.del_flag = 0
);
GO

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, menu.id, 4, seed.sort,
       NULL, NULL, 0, 0, NULL, N'_self', 0, 1, 1, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark,
       1, SYSUTCDATETIME(), 1, SYSUTCDATETIME(), 1, N'pc'
FROM (
  SELECT CAST(1 AS BIGINT) AS tenant_id, N'查询流程模板接口' AS resource_name, 10 AS sort,
         N'flow:template:api:read' AS perms, N'GET' AS api_method,
         N'/api/flow/template/**' AS api_url, N'分页与详情查询流程模板' AS remark
  UNION ALL
  SELECT 1, N'写入流程模板接口', 11,
         N'flow:template:api:write', N'POST', N'/api/flow/template/**', N'创建、启用、禁用、复制模板及从模板建模型'
  UNION ALL
  SELECT 1, N'修改流程模板接口', 12,
         N'flow:template:api:update', N'PUT', N'/api/flow/template', N'更新流程模板'
  UNION ALL
  SELECT 1, N'删除流程模板接口', 13,
         N'flow:template:api:delete', N'DELETE', N'/api/flow/template/**', N'删除流程模板'
) seed
INNER JOIN (
  SELECT TOP 1 id FROM sys_resource
  WHERE tenant_id = 1 AND path = N'/flow/template' AND del_flag = 0
  ORDER BY id
) menu ON 1 = 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = seed.tenant_id AND r.resource_type = 4
    AND r.perms = seed.perms AND r.del_flag = 0
);
GO

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, menu.id, 4, seed.sort,
       NULL, NULL, 0, 0, NULL, N'_self', 0, 1, 1, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark,
       1, SYSUTCDATETIME(), 1, SYSUTCDATETIME(), 1, N'pc'
FROM (
  SELECT CAST(1 AS BIGINT) AS tenant_id, N'查询条件规则接口' AS resource_name, 10 AS sort,
         N'flow:condition-rule:api:read' AS perms, N'GET' AS api_method,
         N'/api/flow/conditionRule/**' AS api_url, N'分页与详情查询条件规则' AS remark
  UNION ALL
  SELECT 1, N'写入条件规则接口', 11,
         N'flow:condition-rule:api:write', N'POST', N'/api/flow/conditionRule/**', N'新增与测试条件规则'
  UNION ALL
  SELECT 1, N'修改条件规则接口', 12,
         N'flow:condition-rule:api:update', N'PUT', N'/api/flow/conditionRule', N'修改条件规则'
  UNION ALL
  SELECT 1, N'删除条件规则接口', 13,
         N'flow:condition-rule:api:delete', N'DELETE', N'/api/flow/conditionRule/**', N'删除条件规则'
) seed
INNER JOIN (
  SELECT TOP 1 id FROM sys_resource
  WHERE tenant_id = 1 AND path = N'/flow/conditionRule' AND del_flag = 0
  ORDER BY id
) menu ON 1 = 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = seed.tenant_id AND r.resource_type = 4
    AND r.perms = seed.perms AND r.del_flag = 0
);
GO

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT seed.tenant_id, seed.role_id, target.id, SYSUTCDATETIME()
FROM (
  SELECT rr.tenant_id, rr.role_id
  FROM sys_role_resource rr
  INNER JOIN sys_resource r
    ON r.id = rr.resource_id
   AND r.tenant_id = rr.tenant_id
   AND r.del_flag = 0
   AND (
     r.path IN (N'/flow', N'/flow/model', N'/flow/category', N'/flow/monitor')
     OR r.perms IN (N'flow:model:list', N'flow:model:view')
   )
) seed
INNER JOIN sys_resource target
  ON target.tenant_id = seed.tenant_id
 AND target.del_flag = 0
 AND (
   target.path IN (N'/flow/template', N'/flow/conditionRule')
   OR target.perms IN (
     N'flow:template:view', N'flow:template:manage',
     N'flow:template:api:read', N'flow:template:api:write',
     N'flow:template:api:update', N'flow:template:api:delete',
     N'flow:condition-rule:view', N'flow:condition-rule:manage',
     N'flow:condition-rule:api:read', N'flow:condition-rule:api:write',
     N'flow:condition-rule:api:update', N'flow:condition-rule:api:delete'
   )
 )
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_resource existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.role_id = seed.role_id
    AND existing.resource_id = target.id
);
GO
