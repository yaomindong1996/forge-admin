-- =============================================
-- AI 会话管理菜单及权限
-- =============================================

-- 会话管理菜单（挂在 AI管理 目录下）
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
SELECT 0, '会话管理', id, 2, 3, '/ai/session', '/ai/session', 0, 0, 1, 1, 'mdi:chat-processing', 0, 0, 'AI会话管理', NOW(), NOW()
FROM sys_resource WHERE tenant_id = 0 AND path = '/ai' AND resource_type = 1
LIMIT 1;

SET @ai_session_menu_id = LAST_INSERT_ID();

-- 会话管理按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, perms, is_external, is_public, menu_status, visible, remark, create_time, update_time)
VALUES
(0, '会话查看', @ai_session_menu_id, 3, 1, 'ai:session:query', 0, 0, 1, 1, '查看AI会话', NOW(), NOW()),
(0, '会话删除', @ai_session_menu_id, 3, 2, 'ai:session:delete', 0, 0, 1, 1, '删除AI会话', NOW(), NOW());

-- 给超级管理员角色分配权限
INSERT INTO sys_role_resource (role_id, resource_id)
SELECT 1, id FROM sys_resource WHERE tenant_id = 0 AND id >= @ai_session_menu_id;
