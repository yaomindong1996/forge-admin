-- V1.0.90: 对齐知识库/存储实例菜单路径与前端路由
-- 知识库页面文件改为 ai/knowledge/list.vue、ai/store-instance/store.vue，
-- unplugin-vue-router 生成 /ai/knowledge/list、/ai/store-instance/store；
-- 原菜单 component/path 与实际路由不一致导致点击 404，此处对齐。
-- 同时将知识库相关表 tenant_id 改为 DEFAULT 0，超级管理员创建时落 0 不报错。

-- 0. 知识库相关表 tenant_id 允许落 0（超级管理员创建时租户拦截器不填充）
ALTER TABLE ai_knowledge
    ALTER COLUMN tenant_id SET DEFAULT 0;
ALTER TABLE ai_store_instance
    ALTER COLUMN tenant_id SET DEFAULT 0;
ALTER TABLE ai_knowledge_document
    ALTER COLUMN tenant_id SET DEFAULT 0;
ALTER TABLE ai_knowledge_chunk
    ALTER COLUMN tenant_id SET DEFAULT 0;

-- 1. 知识库管理菜单：component 从 ai/knowledge/index 改为 ai/knowledge/list
UPDATE sys_resource
SET component = 'ai/knowledge/list',
    update_time = NOW()
WHERE tenant_id = 1
  AND path = '/ai/knowledge/list'
  AND del_flag = 0;

-- 2. 存储实例菜单：path 从 /ai/knowledge/store 改为 /ai/store-instance/store，
--    component 从 ai/store-instance/index 改为 ai/store-instance/store
UPDATE sys_resource
SET path = '/ai/store-instance/store',
    component = 'ai/store-instance/store',
    update_time = NOW()
WHERE tenant_id = 1
  AND path = '/ai/knowledge/store'
  AND del_flag = 0;
