-- 流程模型历史版本清理是不可逆管理动作，单独授权且不自动回填角色。
SET @flow_model_menu_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 2
    AND path = '/flow/model' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '清理流程历史版本', @flow_model_menu_id, 3, 90,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1,
       'flow:model:version:cleanup', NULL, NULL, NULL, 0, 0, NULL,
       '清理当前租户不再引用的流程模型历史版本',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @flow_model_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.resource_type = 3
      AND r.perms = 'flow:model:version:cleanup'
      AND r.del_flag = 0
  );

SET @flow_model_cleanup_permission_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 3
    AND perms = 'flow:model:version:cleanup' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '流程历史版本清理接口', @flow_model_cleanup_permission_id, 4, 91,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1,
       'flow:model:version:cleanup:api', NULL, 'POST',
       '/api/flow/model/version/cleanup', 0, 0, NULL,
       '清理当前租户流程模型历史版本',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @flow_model_cleanup_permission_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.resource_type = 4
      AND r.perms = 'flow:model:version:cleanup:api'
      AND r.del_flag = 0
  );
