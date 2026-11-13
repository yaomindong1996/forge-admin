-- 应用工作台是手写动态路由，必须作为隐藏菜单进入角色路由授权树，否则权限守卫会跳转 403。

SET @application_menu_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type IN (1, 2)
    AND path = '/app-center'
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
SELECT 1, '应用工作台', @application_menu_id, 2, 90,
       '/app-center/application/:applicationCode', 'app-center/application.[applicationCode]',
       0, 0, NULL, '_self', 0, 1, 0, 'ai:businessApplication:list',
       'ionicons5:AppsOutline', 0, 0, NULL,
       '业务应用聚合工作台隐藏路由，仅进入授权树，不显示在侧边菜单',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @application_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = 1
      AND r.resource_type = 2
      AND r.path = '/app-center/application/:applicationCode'
      AND r.del_flag = 0
  );

SET @application_workspace_resource_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/app-center/application/:applicationCode'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

-- 只继承已经拥有业务应用查看权限的角色，不向其它角色扩大应用访问范围。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, role_resource.role_id, @application_workspace_resource_id, NOW()
FROM sys_role_resource role_resource
INNER JOIN sys_resource list_permission
  ON list_permission.tenant_id = 1
 AND list_permission.id = role_resource.resource_id
 AND list_permission.perms = 'ai:businessApplication:list'
 AND list_permission.del_flag = 0
WHERE role_resource.tenant_id = 1
  AND @application_workspace_resource_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = role_resource.role_id
      AND existing.resource_id = @application_workspace_resource_id
  );
