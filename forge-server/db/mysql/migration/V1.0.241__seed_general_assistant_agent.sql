-- =============================================================================
-- 社区版预置默认「通用助手」智能体与全局 AI 助手默认绑定：
-- 1) 在租户 1 补齐通用对话智能体（agent_code = 'general_assistant'），
--    不硬编码 provider_id / model_name，由运行时自动使用当前租户默认/首个启用供应商与模型。
-- 2) 将租户 1 全局 AI 助手默认路由绑定（ai_hub_binding / GENERAL_AGENT）指向 general_assistant，
--    避免日常问答误落到 CRUD配置生成器 / 大屏生成助手 等结构化 JSON 专用智能体。
-- =============================================================================

INSERT INTO ai_agent (
  id, tenant_id, agent_name, agent_code, description, system_prompt,
  provider_id, model_name, model_selection_mode, route_policy_id,
  temperature, max_tokens, extra_config, greeting, preset_questions,
  max_iters, knowledge_ids, rag_mode, tool_group_mode,
  status, create_by, create_time, update_by, update_time, del_flag, create_dept
)
SELECT
  2000000000000000101,
  1,
  '通用助手',
  'general_assistant',
  '回答日常办公、文档整理、问题排查与通用知识问答，作为全站 AI 助手的默认对话智能体。',
  '你是 Forge Admin 平台的通用 AI 助手。请用清晰、准确、简洁的中文回答用户的日常问答、文档撰写、需求分析与问题排查请求；如涉及具体业务数据但当前未绑定相关工具或知识库，请如实说明并给出可执行的下一步建议，不要编造不存在的系统数据。',
  NULL,
  NULL,
  'PINNED',
  NULL,
  0.70,
  4096,
  '{"openingStatement":"你好，我是平台通用 AI 助手，可以帮你解答日常问题、整理文档或梳理工作思路。","suggestedQuestions":["帮我梳理一份本周工作总结模板","把一段业务需求拆解为清晰的执行清单","帮我润色一封项目进度同步邮件"]}',
  '你好，我是平台通用 AI 助手，可以帮你解答日常问题、整理文档或梳理工作思路。',
  '["帮我梳理一份本周工作总结模板","把一段业务需求拆解为清晰的执行清单","帮我润色一封项目进度同步邮件"]',
  6,
  NULL,
  'none',
  'all',
  '0',
  1,
  NOW(),
  1,
  NOW(),
  0,
  '1'
WHERE NOT EXISTS (
  SELECT 1 FROM ai_agent
  WHERE agent_code = 'general_assistant' AND del_flag = 0
);

INSERT INTO ai_hub_binding (
  id, tenant_id, binding_kind, binding_value, remark,
  create_by, create_time, create_dept, update_by, update_time, del_flag
)
SELECT
  2000000000000000201,
  1,
  'GENERAL_AGENT',
  'general_assistant',
  '社区版默认全局通用助手绑定',
  1,
  NOW(),
  1,
  1,
  NOW(),
  0
WHERE NOT EXISTS (
  SELECT 1 FROM ai_hub_binding
  WHERE tenant_id = 1 AND binding_kind = 'GENERAL_AGENT' AND del_flag = 0
);
