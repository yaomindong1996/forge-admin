-- V1.0.98: 恢复 AI创建Agent 菜单入口
-- V1.0.93 删除了 /ai/agent-workspace/ai-create 菜单，但 agent-create 页面（AI 流式创建向导）
-- 仍在开发中且已接入智能体管理页的"创建智能体"按钮（跳转 /ai/agent-create）。
-- 为恢复访问入口，新增独立菜单挂到 AI能力(9067) 目录下。

-- ============================================================
-- 1. 新增 AI创建Agent 菜单（挂到 /ai 目录，sort=12）
-- ============================================================
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, 'AI创建Agent', menu.id, 2, 12, '/ai/agent-create', 'ai/agent-create/index', 0, '_self', 0, 1, 1, 'ai:agent:ai-create', 'ionicons5:SparklesOutline', 1, 0, '描述需求，AI 自动生成 Agent', 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai' AND del_flag = 0 LIMIT 1) menu
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-create' AND del_flag = 0);

-- ============================================================
-- 2. 授予 admin 角色
-- ============================================================
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, s.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' ORDER BY id LIMIT 1) admin_role
JOIN sys_resource s ON s.tenant_id = 1
WHERE s.perms = 'ai:agent:ai-create'
  AND s.del_flag = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource rr
    WHERE rr.tenant_id = 1 AND rr.role_id = admin_role.id AND rr.resource_id = s.id
  );
