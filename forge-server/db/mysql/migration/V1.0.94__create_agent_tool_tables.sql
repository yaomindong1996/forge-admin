-- V1.0.94: 工具管理菜单路径修正 + 按钮权限补齐 + 清理空父菜单
-- 注：ai_agent_tool_config / ai_agent_tool_permission 表已在 V1.0.88 创建

-- ============================================================
-- 1. 工具管理菜单路径修正：/ai/agent-workspace/tool → /ai/agent-tool
-- ============================================================
UPDATE sys_resource
SET path = '/ai/agent-tool', update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/agent-workspace/tool' AND del_flag = 0;

-- ============================================================
-- 2. 补齐工具管理增删改按钮权限（原只有 list）
-- ============================================================

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '新增工具绑定', r.id, 3, 1, 0, '_self', 0, 1, 1, 'ai:agent:tool:add', 0, 0, '新增工具绑定', 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-tool' AND del_flag = 0) r
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'ai:agent:tool:add' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '修改工具绑定', r.id, 3, 2, 0, '_self', 0, 1, 1, 'ai:agent:tool:edit', 0, 0, '修改工具绑定', 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-tool' AND del_flag = 0) r
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'ai:agent:tool:edit' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '删除工具绑定', r.id, 3, 3, 0, '_self', 0, 1, 1, 'ai:agent:tool:delete', 0, 0, '删除工具绑定', 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-tool' AND del_flag = 0) r
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'ai:agent:tool:delete' AND del_flag = 0);

-- 将新增按钮权限授予超级管理员角色
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, s.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' ORDER BY id LIMIT 1) admin_role
JOIN sys_resource s ON s.tenant_id = 1
WHERE s.perms IN ('ai:agent:tool:add', 'ai:agent:tool:edit', 'ai:agent:tool:delete')
  AND s.del_flag = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource rr
    WHERE rr.tenant_id = 1 AND rr.role_id = admin_role.id AND rr.resource_id = s.id
  );

-- ============================================================
-- 3. 删除空父菜单 /ai/agent-workspace（所有子菜单已移出）
-- ============================================================

-- 先删除角色-菜单关联
DELETE rr FROM sys_role_resource rr
INNER JOIN sys_resource r ON r.id = rr.resource_id AND r.del_flag = 0
WHERE r.tenant_id = 1 AND r.path = '/ai/agent-workspace';

-- 再逻辑删除菜单本身
UPDATE sys_resource SET del_flag = id, update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/agent-workspace' AND del_flag = 0;
