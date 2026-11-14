-- 流程监控读、管理和不可逆清理权限分离；管理与清理权限不自动向任何角色授权。

SET @flow_monitor_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/flow/monitor'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

-- 已有菜单角色继续拥有监控读权限，避免升级后页面不可访问。
UPDATE sys_resource
SET perms = 'flow:monitor:view',
    update_time = NOW()
WHERE id = @flow_monitor_menu_id
  AND (perms IS NULL OR perms = '' OR perms = 'flow:monitor:view');

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, @flow_monitor_menu_id, 3, seed.sort,
       NULL, NULL, 0, 0, NULL,
       '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '管理流程实例' resource_name, 80 sort,
         'flow:monitor:manage' perms, '终止、回退、转派、挂起和激活当前租户流程实例' remark
  UNION ALL
  SELECT 1, '清理流程数据', 90,
         'flow:monitor:cleanup', '不可逆删除当前租户流程引擎及Forge关联数据'
) seed
WHERE @flow_monitor_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource existing
    WHERE existing.tenant_id = seed.tenant_id
      AND existing.resource_type = 3
      AND existing.perms = seed.perms
      AND existing.del_flag = 0
  );

SET @flow_manage_permission_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 3
    AND perms = 'flow:monitor:manage'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

SET @flow_cleanup_permission_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 3
    AND perms = 'flow:monitor:cleanup'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '流程监控查询接口', @flow_monitor_menu_id, 4, 100,
       NULL, NULL, 0, 0, NULL,
       '_self', 0, 1, 1, 'flow:monitor:view:api:read', NULL,
       'GET', '/api/flow/monitor/**', 0, 0, NULL, '查询当前租户流程监控数据',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @flow_monitor_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource existing
    WHERE existing.tenant_id = 1
      AND existing.resource_type = 4
      AND existing.perms = 'flow:monitor:view:api:read'
      AND existing.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '流程实例管理接口', @flow_manage_permission_id, 4, 110,
       NULL, NULL, 0, 0, NULL,
       '_self', 0, 1, 1, 'flow:monitor:manage:api:write', NULL,
       'POST', '/api/flow/monitor/**', 0, 0, NULL, '管理当前租户流程实例状态',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @flow_manage_permission_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource existing
    WHERE existing.tenant_id = 1
      AND existing.resource_type = 4
      AND existing.perms = 'flow:monitor:manage:api:write'
      AND existing.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '流程错误日志解决接口', @flow_manage_permission_id, 4, 111,
       NULL, NULL, 0, 0, NULL,
       '_self', 0, 1, 1, 'flow:monitor:manage:api:update', NULL,
       'PUT', '/api/flow/monitor/error-logs/*/resolve', 0, 0, NULL, '解决当前租户流程错误日志',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @flow_manage_permission_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource existing
    WHERE existing.tenant_id = 1
      AND existing.resource_type = 4
      AND existing.perms = 'flow:monitor:manage:api:update'
      AND existing.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, @flow_cleanup_permission_id, 4, seed.sort,
       NULL, NULL, 0, 0, NULL,
       '_self', 0, 1, 1, seed.perms, NULL,
       'POST', seed.api_url, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '批量清理流程数据' resource_name, 121 sort,
         'flow:monitor:cleanup:api:batch' perms,
         '/api/flow/monitor/instances/cleanup' api_url,
         '批量清理当前租户筛选结果' remark
  UNION ALL
  SELECT 1, '清理单个流程数据', 122,
         'flow:monitor:cleanup:api:single',
         '/api/flow/monitor/instance/*/delete',
         '清理当前租户单个流程实例'
) seed
WHERE @flow_cleanup_permission_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource existing
    WHERE existing.tenant_id = seed.tenant_id
      AND existing.resource_type = 4
      AND existing.perms = seed.perms
      AND existing.del_flag = 0
  );

SET @flow_monitor_read_api_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 4
    AND perms = 'flow:monitor:view:api:read'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

-- 只继承既有监控菜单读权限；管理和清理 API 不做任何角色回填。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT role_menu.tenant_id, role_menu.role_id, @flow_monitor_read_api_id, NOW()
FROM sys_role_resource role_menu
WHERE role_menu.tenant_id = 1
  AND role_menu.resource_id = @flow_monitor_menu_id
  AND @flow_monitor_read_api_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = role_menu.tenant_id
      AND existing.role_id = role_menu.role_id
      AND existing.resource_id = @flow_monitor_read_api_id
  );
