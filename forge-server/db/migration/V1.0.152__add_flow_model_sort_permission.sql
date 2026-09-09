-- 流程模型排序属于模型治理写操作，独立授权且不自动写入角色权限。
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
SELECT 1, '调整流程模型排序', @flow_model_menu_id, 3, 91,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1,
       'flow:model:sort', NULL, NULL, NULL, 0, 0, NULL,
       '调整当前租户流程模型目录顺序',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @flow_model_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.resource_type = 3
      AND r.perms = 'flow:model:sort'
      AND r.del_flag = 0
  );

SET @flow_model_sort_permission_id := (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND resource_type = 3
      AND perms = 'flow:model:sort' AND del_flag = 0
    ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort,
    path, component, is_external, sso_enabled, sso_target_client,
    open_target, is_public, menu_status, visible, perms, icon,
    api_method, api_url, keep_alive, always_show, redirect, remark,
    create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '流程模型排序接口', @flow_model_sort_permission_id, 4, 92,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1,
       'flow:model:sort:api', NULL, 'POST',
       '/api/flow/model/sort', 0, 0, NULL,
       '批量保存当前租户流程模型排序',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @flow_model_sort_permission_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.resource_type = 4
      AND r.perms = 'flow:model:sort:api'
      AND r.del_flag = 0
  );
