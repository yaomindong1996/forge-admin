-- Add runtime callback and legacy approval compatibility permissions for business flow closure.

SET @app_center_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:businessApp:list'
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, COALESCE(@app_center_menu_id, 0), 3, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '流程回调' resource_name, 29 sort, 'ai:businessFlow:callback' perms, '处理业务流程回调权限' remark
  UNION ALL SELECT '审批兼容发起', 30, 'ai:businessApproval:start', '旧业务审批入口兼容转发权限'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource r
  WHERE r.tenant_id = 1
    AND r.resource_type = 3
    AND r.perms = seed.perms
);
