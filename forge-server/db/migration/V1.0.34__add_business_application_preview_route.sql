-- 应用草稿预览是手写动态路由；作为隐藏资源继承应用查看权限，避免权限守卫跳转 403。

SET @application_workspace_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/app-center/application/:applicationCode'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

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
SELECT 1, '应用草稿预览', COALESCE(@application_workspace_id, @application_menu_id), 2, 91,
       '/app-center/application/:applicationCode/preview',
       'app-center/application-preview.[applicationCode]',
       0, 0, NULL, '_self', 0, 1, 0, 'ai:businessApplication:preview',
       'ionicons5:EyeOutline', 0, 0, NULL,
       '不依赖页面入口的应用对象草稿预览隐藏路由',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE COALESCE(@application_workspace_id, @application_menu_id) IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = 1
      AND r.resource_type = 2
      AND r.path = '/app-center/application/:applicationCode/preview'
      AND r.del_flag = 0
  );

SET @application_preview_resource_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/app-center/application/:applicationCode/preview'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, role_resource.role_id, @application_preview_resource_id, NOW()
FROM sys_role_resource role_resource
INNER JOIN sys_resource list_permission
  ON list_permission.tenant_id = 1
 AND list_permission.id = role_resource.resource_id
 AND list_permission.perms = 'ai:businessApplication:list'
 AND list_permission.del_flag = 0
WHERE role_resource.tenant_id = 1
  AND @application_preview_resource_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = role_resource.role_id
      AND existing.resource_id = @application_preview_resource_id
  );
