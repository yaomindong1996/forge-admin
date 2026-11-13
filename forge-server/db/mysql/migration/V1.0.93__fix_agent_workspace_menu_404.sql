-- 修复 Agent工作台 子菜单 404——对齐 unplugin-vue-router 自动路由路径
-- V1.0.91: 菜单 path 从 /ai/agent-workspace/* 改为实际文件路径对应的路由
-- 同时移除 "AI创建Agent" 菜单（属于功能入口，不作为独立菜单）

-- ============================================================
-- 1. 技能管理：/ai/agent-workspace/skill → /ai/skill
-- ============================================================
UPDATE sys_resource
SET path = '/ai/skill', update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/agent-workspace/skill' AND del_flag = 0;

-- ============================================================
-- 2. Agent对话：/ai/agent-workspace/chat → /ai/agent/chat
-- ============================================================
UPDATE sys_resource
SET path = '/ai/agent/chat', update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/agent-workspace/chat' AND del_flag = 0;

-- ============================================================
-- 3. 删除 "AI创建Agent" 菜单及其按钮权限（功能入口，不作为独立菜单）
-- ============================================================
DELETE FROM sys_role_resource
WHERE resource_id IN (
  SELECT id FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND (path = '/ai/agent-workspace/ai-create' OR perms IN ('ai:agent:ai-create', 'ai:agent:ai-create:confirm'))) r
);

DELETE FROM sys_resource
WHERE tenant_id = 1 AND (path = '/ai/agent-workspace/ai-create' OR perms IN ('ai:agent:ai-create', 'ai:agent:ai-create:confirm'));
