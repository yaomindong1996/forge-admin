-- 企业协同消息测试发送按钮与 API 资源（Task 18 补充）。
-- 内置数据 tenant_id=1，具备 NOT EXISTS 防重复保护；不写入任何 Secret。

-- ============================================================
-- 1. 按钮资源：消息测试发送（挂在连接管理菜单下）
-- ============================================================

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible,
  perms, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '消息测试发送', menu.id, 3, 6, 0, '_self', 0, 1, 1,
       'system:collaboration:message:test', 0, 0, '向指定测试用户发送协同消息验证通道',
       1, NOW(), 1, NOW(), 1, 'pc'
FROM sys_resource menu
WHERE menu.tenant_id = 1
  AND menu.resource_type = 2
  AND menu.perms = 'system:collaboration:connection:list'
  AND menu.del_flag = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.resource_type = 3
      AND r.perms = 'system:collaboration:message:test' AND r.del_flag = 0
  );

-- ============================================================
-- 2. API 资源：消息测试发送接口（挂在企业协同目录下）
-- ============================================================

SET @collab_dir_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/system/collaboration' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible,
  perms, api_method, api_url, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '消息测试发送接口', COALESCE(@collab_dir_id, 0), 4, 18, 0, '_self', 0, 1, 1,
       'system:collaboration:api:message:test', 'POST', '/system/collaboration/message/test-send', 0, 0,
       '向指定测试用户发送协同消息验证通道',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 4
    AND r.perms = 'system:collaboration:api:message:test' AND r.del_flag = 0
);

-- ============================================================
-- 3. 角色授权：仅显式授予默认租户超级管理员
-- ============================================================

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1 AND resource.del_flag = 0
WHERE resource.client_code = 'pc'
  AND resource.perms IN ('system:collaboration:message:test', 'system:collaboration:api:message:test')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = admin_role.id
      AND existing.resource_id = resource.id
  );
