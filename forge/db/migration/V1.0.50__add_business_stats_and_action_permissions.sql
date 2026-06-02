-- 低代码应用全链路闭环：Phase 5 报表、触发器执行和单据动作权限

SET @app_center_menu_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 2 AND perms = 'ai:businessApp:list'
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
  SELECT '保存单据' resource_name, 31 sort, 'ai:businessDocument:save' perms, '保存业务单据权限' remark
  UNION ALL SELECT '提交单据', 32, 'ai:businessDocument:submit', '提交业务单据权限'
  UNION ALL SELECT '撤回单据', 33, 'ai:businessDocument:withdraw', '撤回业务单据权限'
  UNION ALL SELECT '执行触发器', 34, 'ai:businessTrigger:execute', '执行业务触发器权限'
  UNION ALL SELECT '查看业务报表', 35, 'ai:businessStats:view', '查看业务对象报表权限'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource r
  WHERE r.tenant_id = 1
    AND r.resource_type = 3
    AND r.perms = seed.perms
);
