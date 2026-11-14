-- V1.0.95: 修复 Agent工作台 子菜单孤儿问题
-- V1.0.94 逻辑删除了 /ai/agent-workspace 父菜单（id=9739），
-- 但其子菜单（技能管理/Agent对话/工具管理）的 parent_id 仍指向 9739，
-- 导致后端菜单树从根递归构建时无法到达这些子菜单 → 前端 403/不可见。
-- 修复：将这三个子菜单的父节点改为顶级目录 /ai。
-- 以 path 定位（不硬编码 id），幂等可重复执行。

-- ============================================================
-- 1. 技能管理：/ai/skill → 挂到 /ai
-- ============================================================
UPDATE sys_resource
SET parent_id = COALESCE(
        (SELECT id FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai' AND del_flag = 0 LIMIT 1) t),
        parent_id),
    update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/skill' AND del_flag = 0
  AND parent_id = (SELECT id FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-workspace' AND del_flag <> 0 LIMIT 1) t);

-- ============================================================
-- 2. Agent对话：/ai/agent/chat → 挂到 /ai
-- ============================================================
UPDATE sys_resource
SET parent_id = COALESCE(
        (SELECT id FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai' AND del_flag = 0 LIMIT 1) t),
        parent_id),
    update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/agent/chat' AND del_flag = 0
  AND parent_id = (SELECT id FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-workspace' AND del_flag <> 0 LIMIT 1) t);

-- ============================================================
-- 3. 工具管理：/ai/agent-tool → 挂到 /ai
-- ============================================================
UPDATE sys_resource
SET parent_id = COALESCE(
        (SELECT id FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai' AND del_flag = 0 LIMIT 1) t),
        parent_id),
    update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/agent-tool' AND del_flag = 0
  AND parent_id = (SELECT id FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-workspace' AND del_flag <> 0 LIMIT 1) t);
