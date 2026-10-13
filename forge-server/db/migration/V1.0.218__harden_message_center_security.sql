-- 消息中心加固：发送状态字典对齐枚举；补齐发送/监控 API 权限资源并授予已有菜单角色。

-- 1) sys_message_send_status: 0=发送中（与 MessageSendStatus.SENDING 对齐，原误标「草稿」）
UPDATE sys_dict_data
SET dict_label = '发送中',
    list_class = 'warning',
    remark = '消息发送中',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_type = 'sys_message_send_status'
  AND dict_value = '0'
  AND del_flag = 0
  AND dict_label <> '发送中';

-- 确保「部分成功」存在（兼容未跑过 V1.0.80 的环境）
INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, 4, '部分成功', '3', 'sys_message_send_status',
       NULL, 'warning', 'N', 1, '部分接收人投递失败',
       1, NOW(), 1, NOW(), 1, 0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_data
    WHERE tenant_id = 1
      AND dict_type = 'sys_message_send_status'
      AND dict_value = '3'
      AND del_flag = 0
);

-- 2) API 权限：发送消息 / 消息监控
SET @message_manage_menu_id = (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND client_code = 'pc'
      AND path = '/message/manage'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

SET @message_root_id = (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND client_code = 'pc'
      AND path = '/message'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

SET @message_perm_parent_id = COALESCE(@message_manage_menu_id, @message_root_id);

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort,
    is_external, open_target, is_public, menu_status, visible,
    perms, api_method, api_url, keep_alive, always_show,
    remark, create_by, create_time, update_by, update_time, create_dept,
    client_code, min_user_type, del_flag
)
SELECT 1, '发送消息', @message_perm_parent_id, 4, 1,
       0, '_self', 0, 1, 1,
       'message:send', 'POST', '/api/message/send', 0, 0,
       '消息中心手动/测试发送', 1, NOW(), 1, NOW(), 1,
       'pc', 2, 0
WHERE @message_perm_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE tenant_id = 1 AND perms = 'message:send' AND del_flag = 0
  );

-- 同一 perms 在 uk_tenant_resource_active 下只能一条；用通配覆盖分页与详情
INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort,
    is_external, open_target, is_public, menu_status, visible,
    perms, api_method, api_url, keep_alive, always_show,
    remark, create_by, create_time, update_by, update_time, create_dept,
    client_code, min_user_type, del_flag
)
SELECT 1, '消息监控查询', @message_perm_parent_id, 4, 2,
       0, '_self', 0, 1, 1,
       'message:manage:query', NULL, '/api/message/manage/**', 0, 0,
       '消息监控分页与详情', 1, NOW(), 1, NOW(), 1,
       'pc', 2, 0
WHERE @message_perm_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE tenant_id = 1 AND perms = 'message:manage:query' AND del_flag = 0
  );

-- 把权限授予已拥有「消息监控」菜单的角色；若无菜单则授予超级管理员角色（role_key=admin）
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT menu_grant.tenant_id, menu_grant.role_id, permission_resource.id, NOW()
FROM sys_role_resource menu_grant
INNER JOIN sys_resource menu_resource
        ON menu_resource.id = menu_grant.resource_id
       AND menu_resource.tenant_id = menu_grant.tenant_id
       AND menu_resource.path = '/message/manage'
       AND menu_resource.del_flag = 0
INNER JOIN sys_resource permission_resource
        ON permission_resource.tenant_id = menu_grant.tenant_id
       AND permission_resource.del_flag = 0
       AND permission_resource.perms IN ('message:send', 'message:manage:query')
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = menu_grant.tenant_id
      AND existing.role_id = menu_grant.role_id
      AND existing.resource_id = permission_resource.id
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT r.tenant_id, r.id, permission_resource.id, NOW()
FROM sys_role r
INNER JOIN sys_resource permission_resource
        ON permission_resource.tenant_id = r.tenant_id
       AND permission_resource.del_flag = 0
       AND permission_resource.perms IN ('message:send', 'message:manage:query')
WHERE r.del_flag = 0
  AND r.role_key = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_resource existing
      WHERE existing.tenant_id = r.tenant_id
        AND existing.role_id = r.id
        AND existing.resource_id = permission_resource.id
  );
