-- 定时任务配置工作台隐藏路由。只继承已有任务管理菜单的角色范围，不扩大访问权限。

SET @job_config_resource_id = (
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
SELECT 1, '新建定时任务', @job_config_resource_id, 2, 90,
       '/system/job-config/editor', 'system/job-config.editor',
       0, 0, NULL, '_self', 0, 1, 0, NULL,
       'ionicons5:AddCircleOutline', 0, 0, NULL,
       '定时任务全屏新建工作台隐藏路由',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @job_config_resource_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource resource
    WHERE resource.tenant_id = 1
      AND resource.resource_type = 2
      AND resource.path = '/system/job-config/editor'
      AND resource.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '编辑定时任务', @job_config_resource_id, 2, 91,
       '/system/job-config/editor/:id', 'system/job-config.editor.[id]',
       0, 0, NULL, '_self', 0, 1, 0, NULL,
       'ionicons5:CreateOutline', 0, 0, NULL,
       '定时任务全屏编辑工作台隐藏路由',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @job_config_resource_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource resource
    WHERE resource.tenant_id = 1
      AND resource.resource_type = 2
      AND resource.path = '/system/job-config/editor/:id'
      AND resource.del_flag = 0
  );

SET @job_create_resource_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/system/job-config/editor'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

SET @job_edit_resource_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/system/job-config/editor/:id'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, role_resource.role_id, target.resource_id, NOW()
FROM sys_role_resource role_resource
INNER JOIN (
  SELECT @job_create_resource_id AS resource_id
  UNION ALL
  SELECT @job_edit_resource_id AS resource_id
) target ON target.resource_id IS NOT NULL
WHERE role_resource.tenant_id = 1
  AND role_resource.resource_id = @job_config_resource_id
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = role_resource.role_id
      AND existing.resource_id = target.resource_id
  );
