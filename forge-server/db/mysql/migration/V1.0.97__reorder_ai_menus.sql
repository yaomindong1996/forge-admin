-- V1.0.97: 重排 AI能力 目录下菜单顺序（配置优先）
-- 智能体相关菜单因之前修复（V1.0.95/V1.0.96）挂到 AI能力 顶级目录，
-- 与原菜单 sort 值重复导致顺序混乱。统一重新赋 sort。

-- ============================================================
-- AI能力(9067) 下平级菜单排序：配置 → Agent生态 → 使用类
-- ============================================================

UPDATE sys_resource SET sort = 1,  update_time = NOW() WHERE tenant_id = 1 AND id = 9068;  -- 供应商管理
UPDATE sys_resource SET sort = 2,  update_time = NOW() WHERE tenant_id = 1 AND id = 9457;  -- 模型治理
UPDATE sys_resource SET sort = 3,  update_time = NOW() WHERE tenant_id = 1 AND id = 9160;  -- 智能体管理
UPDATE sys_resource SET sort = 4,  update_time = NOW() WHERE tenant_id = 1 AND id = 9741;  -- Agent对话
UPDATE sys_resource SET sort = 5,  update_time = NOW() WHERE tenant_id = 1 AND id = 9740;  -- 技能管理
UPDATE sys_resource SET sort = 6,  update_time = NOW() WHERE tenant_id = 1 AND id = 9742;  -- 工具管理
UPDATE sys_resource SET sort = 7,  update_time = NOW() WHERE tenant_id = 1 AND id = 9077;  -- 会话管理
UPDATE sys_resource SET sort = 8,  update_time = NOW() WHERE tenant_id = 1 AND id = 9721;  -- 知识库(目录)
UPDATE sys_resource SET sort = 9,  update_time = NOW() WHERE tenant_id = 1 AND id = 9167;  -- 提示词模板库
UPDATE sys_resource SET sort = 10, update_time = NOW() WHERE tenant_id = 1 AND id = 9751;  -- AI工具(目录)
UPDATE sys_resource SET sort = 11, update_time = NOW() WHERE tenant_id = 1 AND id = 9163;  -- 报表生成记录
