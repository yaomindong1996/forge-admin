-- V1.0.96: 修复 Agent对话 页面与智能体管理重叠问题
-- 原因：ai/agent/chat.vue 位于 ai/agent.vue 子目录，unplugin-vue-router 将其生成为
--       /ai/agent 的子路由。访问 /ai/agent/chat 时父组件 agent.vue 无 <RouterView> 插槽，
--       子组件 chat.vue 无法正常渲染，页面显示与智能体管理相同。
-- 修复：前端已将 chat.vue 移到 ai/agent-chat.vue（平级，生成独立路由 /ai/agent-chat），
--       此处同步更新菜单 path 与 component。

UPDATE sys_resource
SET path = '/ai/agent-chat',
    component = 'ai/agent-chat',
    update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/agent/chat' AND del_flag = 0;
