-- 为在线用户管理补充显式权限，不自动向普通角色授予高风险会话操作。

SET @online_user_menu_id = (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND path = '/system/online'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

UPDATE sys_resource
SET perms = 'system:online:query',
    update_time = NOW()
WHERE id = @online_user_menu_id
  AND (perms IS NULL OR perms = '' OR perms = 'system:online:list');

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort,
    path, component, is_external, sso_enabled, sso_target_client,
    open_target, is_public, menu_status, visible, perms, icon,
    api_method, api_url, keep_alive, always_show, redirect, remark,
    create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @online_user_menu_id, 3, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
    SELECT '强制会话下线' resource_name, 1 sort,
           'system:online:kickout' perms, '按受租户隔离的在线记录强制下线' remark
    UNION ALL SELECT '批量会话下线', 2, 'system:online:batchKickout', '批量强制在线会话下线'
    UNION ALL SELECT '封禁用户', 3, 'system:online:ban', '封禁当前租户用户并下线其会话'
    UNION ALL SELECT '解封用户', 4, 'system:online:unban', '解封当前租户用户'
) seed
WHERE @online_user_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_resource resource
      WHERE resource.tenant_id = 1
        AND resource.resource_type = 3
        AND resource.perms = seed.perms
        AND resource.del_flag = 0
  );
