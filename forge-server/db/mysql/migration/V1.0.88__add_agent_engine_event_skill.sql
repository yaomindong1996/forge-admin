-- Agent引擎事件/工具/技能表与菜单
-- V1.0.88: ai_agent_event, ai_agent_tool_config, ai_agent_tool_permission, ai_skill, ai_skill_file, ai_agent_skill + ai_agent扩展列 + 字典 + 菜单

-- ============================================================
-- 1. Agent 事件流表（审计流水，不做逻辑删除）
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_agent_event` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `turn_index` int NOT NULL COMMENT 'ReAct轮次',
  `event_type` varchar(50) NOT NULL COMMENT '事件类型(28种)',
  `event_data` longtext DEFAULT NULL COMMENT '事件数据JSON',
  `parent_id` bigint DEFAULT NULL COMMENT '父事件ID(工具结果关联工具调用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`, `turn_index`),
  KEY `idx_session_type` (`session_id`, `event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent事件流(全量持久化)';

-- ============================================================
-- 2. Agent 工具绑定表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_agent_tool_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `tool_source` varchar(32) NOT NULL COMMENT '工具来源(mcp/builtin/capability)',
  `tool_key` varchar(200) NOT NULL COMMENT '工具标识',
  `tool_group` varchar(64) DEFAULT 'default' COMMENT '工具组(技能激活)',
  `enabled` char(1) DEFAULT '0' COMMENT '是否启用(0否 1是)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_agent_tool_active` (`agent_id`, `tool_source`, `tool_key`, `del_flag`),
  KEY `idx_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent工具绑定';

-- ============================================================
-- 3. Agent 工具权限表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_agent_tool_permission` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NOT NULL,
  `tool_key` varchar(200) NOT NULL COMMENT '工具标识',
  `decision` varchar(16) NOT NULL COMMENT '权限(allowed/ask/denied)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_agent_perm_active` (`agent_id`, `tool_key`, `del_flag`),
  KEY `idx_agent_tool` (`agent_id`, `tool_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent工具权限(ALLOW/ASK/DENY)';

-- ============================================================
-- 4. AI 技能包
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_skill` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `skill_name` varchar(100) NOT NULL COMMENT '技能名称',
  `skill_code` varchar(100) NOT NULL COMMENT '技能编码',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `version` varchar(32) DEFAULT '1.0.0' COMMENT '版本',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_skill_code_active` (`tenant_id`, `skill_code`, `del_flag`),
  KEY `idx_code` (`skill_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI技能包';

-- ============================================================
-- 5. 技能文件
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_skill_file` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL COMMENT '技能ID',
  `file_path` varchar(500) NOT NULL COMMENT '技能内文件路径(SKILL.md/scripts/x.py)',
  `file_content` longtext NOT NULL COMMENT '文件内容',
  `encoding` varchar(16) DEFAULT 'utf-8' COMMENT '编码',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  KEY `idx_skill` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能文件';

-- ============================================================
-- 6. Agent 技能绑定
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_agent_skill` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_agent_skill_active` (`agent_id`, `skill_id`, `del_flag`),
  KEY `idx_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent技能绑定';

-- ============================================================
-- 7. ai_agent 扩展列（幂等：用 information_schema 判断列存在）
-- ============================================================
SET @db = DATABASE();

-- greeting
SET @col = 'greeting';
SET @sql = (SELECT IF(COUNT(*)=0,
  CONCAT('ALTER TABLE ai_agent ADD COLUMN `', @col, '` varchar(500) DEFAULT NULL COMMENT ''问候语'' AFTER `extra_config`'),
  'SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='ai_agent' AND column_name=@col);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- preset_questions
SET @col = 'preset_questions';
SET @sql = (SELECT IF(COUNT(*)=0,
  CONCAT('ALTER TABLE ai_agent ADD COLUMN `', @col, '` text DEFAULT NULL COMMENT ''预设问题JSON数组'' AFTER `greeting`'),
  'SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='ai_agent' AND column_name=@col);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- max_iters
SET @col = 'max_iters';
SET @sql = (SELECT IF(COUNT(*)=0,
  CONCAT('ALTER TABLE ai_agent ADD COLUMN `', @col, '` int DEFAULT 10 COMMENT ''ReAct最大轮次'' AFTER `preset_questions`'),
  'SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='ai_agent' AND column_name=@col);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- knowledge_ids
SET @col = 'knowledge_ids';
SET @sql = (SELECT IF(COUNT(*)=0,
  CONCAT('ALTER TABLE ai_agent ADD COLUMN `', @col, '` text DEFAULT NULL COMMENT ''关联知识库ID列表JSON'' AFTER `max_iters`'),
  'SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='ai_agent' AND column_name=@col);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rag_mode
SET @col = 'rag_mode';
SET @sql = (SELECT IF(COUNT(*)=0,
  CONCAT('ALTER TABLE ai_agent ADD COLUMN `', @col, '` varchar(32) DEFAULT ''none'' COMMENT ''RAG模式(none/forced/smart)'' AFTER `knowledge_ids`'),
  'SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='ai_agent' AND column_name=@col);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tool_group_mode
SET @col = 'tool_group_mode';
SET @sql = (SELECT IF(COUNT(*)=0,
  CONCAT('ALTER TABLE ai_agent ADD COLUMN `', @col, '` varchar(32) DEFAULT ''all'' COMMENT ''工具组模式(all/skill)'' AFTER `rag_mode`'),
  'SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='ai_agent' AND column_name=@col);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 8. 字典
-- ============================================================
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 'AI Agent事件类型' dict_name, 'ai_agent_event_type' dict_type, 'Agent引擎28种事件' remark
  UNION ALL SELECT 1, 'AI工具来源', 'ai_tool_source', 'mcp/builtin/capability'
  UNION ALL SELECT 1, 'AI工具权限', 'ai_tool_permission', 'allowed/ask/denied'
  UNION ALL SELECT 1, 'AI Agent RAG模式', 'ai_agent_rag_mode', 'none/forced/smart'
  UNION ALL SELECT 1, 'AI工具组模式', 'ai_agent_tool_group_mode', 'all/skill'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.tenant_id = seed.tenant_id AND t.dict_type = seed.dict_type);

-- 事件类型字典数据（28种）
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, 'Agent开始' dict_label, 'AGENT_START' dict_value, 'ai_agent_event_type' dict_type, 'primary' list_class, 'N' is_default, '' remark
  UNION ALL SELECT 1, 2, 'Agent结束', 'AGENT_END', 'ai_agent_event_type', 'primary', 'N', ''
  UNION ALL SELECT 1, 3, 'Agent结果', 'AGENT_RESULT', 'ai_agent_event_type', 'success', 'N', ''
  UNION ALL SELECT 1, 4, '模型调用开始', 'MODEL_CALL_START', 'ai_agent_event_type', 'info', 'N', ''
  UNION ALL SELECT 1, 5, '模型调用结束', 'MODEL_CALL_END', 'ai_agent_event_type', 'info', 'N', ''
  UNION ALL SELECT 1, 6, '文本块开始', 'TEXT_BLOCK_START', 'ai_agent_event_type', 'default', 'N', ''
  UNION ALL SELECT 1, 7, '文本块增量', 'TEXT_BLOCK_DELTA', 'ai_agent_event_type', 'default', 'N', ''
  UNION ALL SELECT 1, 8, '文本块结束', 'TEXT_BLOCK_END', 'ai_agent_event_type', 'default', 'N', ''
  UNION ALL SELECT 1, 9, '思考块开始', 'THINKING_BLOCK_START', 'ai_agent_event_type', 'warning', 'N', ''
  UNION ALL SELECT 1, 10, '思考块增量', 'THINKING_BLOCK_DELTA', 'ai_agent_event_type', 'warning', 'N', ''
  UNION ALL SELECT 1, 11, '思考块结束', 'THINKING_BLOCK_END', 'ai_agent_event_type', 'warning', 'N', ''
  UNION ALL SELECT 1, 12, '数据块开始', 'DATA_BLOCK_START', 'ai_agent_event_type', 'info', 'N', ''
  UNION ALL SELECT 1, 13, '数据块增量', 'DATA_BLOCK_DELTA', 'ai_agent_event_type', 'info', 'N', ''
  UNION ALL SELECT 1, 14, '数据块结束', 'DATA_BLOCK_END', 'ai_agent_event_type', 'info', 'N', ''
  UNION ALL SELECT 1, 15, '工具调用开始', 'TOOL_CALL_START', 'ai_agent_event_type', 'success', 'N', ''
  UNION ALL SELECT 1, 16, '工具调用增量', 'TOOL_CALL_DELTA', 'ai_agent_event_type', 'success', 'N', ''
  UNION ALL SELECT 1, 17, '工具调用结束', 'TOOL_CALL_END', 'ai_agent_event_type', 'success', 'N', ''
  UNION ALL SELECT 1, 18, '工具结果开始', 'TOOL_RESULT_START', 'ai_agent_event_type', 'info', 'N', ''
  UNION ALL SELECT 1, 19, '工具结果文本增量', 'TOOL_RESULT_TEXT_DELTA', 'ai_agent_event_type', 'info', 'N', ''
  UNION ALL SELECT 1, 20, '工具结果数据增量', 'TOOL_RESULT_DATA_DELTA', 'ai_agent_event_type', 'info', 'N', ''
  UNION ALL SELECT 1, 21, '工具结果结束', 'TOOL_RESULT_END', 'ai_agent_event_type', 'info', 'N', ''
  UNION ALL SELECT 1, 22, '超过最大轮次', 'EXCEED_MAX_ITERS', 'ai_agent_event_type', 'error', 'N', ''
  UNION ALL SELECT 1, 23, '请求停止', 'REQUEST_STOP', 'ai_agent_event_type', 'error', 'N', ''
  UNION ALL SELECT 1, 24, '需要用户确认', 'REQUIRE_USER_CONFIRM', 'ai_agent_event_type', 'warning', 'Y', ''
  UNION ALL SELECT 1, 25, '用户确认结果', 'USER_CONFIRM_RESULT', 'ai_agent_event_type', 'success', 'N', ''
  UNION ALL SELECT 1, 26, '子Agent暴露', 'SUBAGENT_EXPOSED', 'ai_agent_event_type', 'primary', 'N', ''
  UNION ALL SELECT 1, 27, '提示块', 'HINT_BLOCK', 'ai_agent_event_type', 'warning', 'N', ''
  UNION ALL SELECT 1, 28, '所有工具被拒绝', 'ALL_TOOLS_DENIED', 'ai_agent_event_type', 'error', 'N', ''
  UNION ALL SELECT 1, 29, '自定义', 'CUSTOM', 'ai_agent_event_type', 'default', 'N', ''
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);

-- 工具来源
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '内置' dict_label, 'builtin' dict_value, 'ai_tool_source' dict_type, 'success' list_class, 'Y' is_default, '内置工具' remark
  UNION ALL SELECT 1, 2, 'MCP', 'mcp', 'ai_tool_source', 'primary', 'N', 'MCP工具'
  UNION ALL SELECT 1, 3, '能力平台', 'capability', 'ai_tool_source', 'info', 'N', '统一能力平台'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);

-- 工具权限
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '允许' dict_label, 'allowed' dict_value, 'ai_tool_permission' dict_type, 'success' list_class, 'Y' is_default, '自动执行' remark
  UNION ALL SELECT 1, 2, '需确认', 'ask', 'ai_tool_permission', 'warning', 'N', '需人工确认'
  UNION ALL SELECT 1, 3, '拒绝', 'denied', 'ai_tool_permission', 'error', 'N', '禁止执行'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);

-- RAG模式
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '关闭' dict_label, 'none' dict_value, 'ai_agent_rag_mode' dict_type, 'default' list_class, 'Y' is_default, '不使用RAG' remark
  UNION ALL SELECT 1, 2, '强制', 'forced', 'ai_agent_rag_mode', 'warning', 'N', '每次查询强制检索'
  UNION ALL SELECT 1, 3, '智能', 'smart', 'ai_agent_rag_mode', 'success', 'N', '由LLM判断是否检索'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);

-- 工具组模式
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '全部工具' dict_label, 'all' dict_value, 'ai_agent_tool_group_mode' dict_type, 'success' list_class, 'Y' is_default, '加载全部启用工具' remark
  UNION ALL SELECT 1, 2, '技能激活', 'skill', 'ai_agent_tool_group_mode', 'primary', 'N', '按技能组激活工具'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);

-- ============================================================
-- 9. 菜单
-- ============================================================

-- Agent工作台菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, 'Agent工作台', COALESCE((SELECT parent_id FROM (SELECT parent_id FROM sys_resource WHERE path = '/ai/provider-model' LIMIT 1) x), 0), 1, 5,
       '/ai/agent-workspace', NULL, 0, '_self', 0, 1, 1, NULL, 'ionicons5:RocketOutline', 1, 0,
       'Agent执行工作台', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.path = '/ai/agent-workspace');

-- Agent工作台子菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, menu.id, seed.resource_type, seed.sort, seed.path, seed.component, 0, '_self', 0, 1, 1, seed.perms, seed.icon, 1, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-workspace' LIMIT 1) menu
JOIN (
  SELECT '技能管理' resource_name, 2 resource_type, 1 sort, '/ai/agent-workspace/skill' path, 'ai/skill/index' component, 'ai:skill:list' perms, 'ionicons5:SparklesOutline' icon, '技能包管理' remark
  UNION ALL SELECT 'Agent对话', 2, 2, '/ai/agent-workspace/chat' path, 'ai/agent/chat' component, 'ai:engine:stream' perms, 'ionicons5:ChatbubblesOutline' icon, 'Agent执行对话'
  UNION ALL SELECT '工具管理', 2, 3, '/ai/agent-workspace/tool' path, 'ai/agent-tool/index' component, 'ai:agent:tool:list' perms, 'ionicons5:ConstructOutline' icon, 'Agent工具与权限管理'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.path = seed.path);

-- 技能管理按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, menu.id, 3, seed.sort, 0, '_self', 0, 1, 1, seed.perms, 0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-workspace/skill' LIMIT 1) menu
JOIN (
  SELECT '新增技能' resource_name, 1 sort, 'ai:skill:add' perms, '新增技能' remark
  UNION ALL SELECT '编辑技能', 2, 'ai:skill:edit', '编辑技能'
  UNION ALL SELECT '删除技能', 3, 'ai:skill:delete', '删除技能'
  UNION ALL SELECT 'AI生成', 4, 'ai:skill:ai-generate', 'AI生成技能'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.perms = seed.perms);

-- Agent对话按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, menu.id, 3, seed.sort, 0, '_self', 0, 1, 1, seed.perms, 0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-workspace/chat' LIMIT 1) menu
JOIN (
  SELECT 'Agent对话' resource_name, 1 sort, 'ai:engine:stream' perms, 'Agent引擎对话' remark
  UNION ALL SELECT '恢复中断', 2, 'ai:engine:resume', 'HITL恢复'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.perms = seed.perms);

-- 授予管理员权限
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1
WHERE resource.client_code = 'pc'
  AND (
    resource.path IN ('/ai/agent-workspace', '/ai/agent-workspace/skill', '/ai/agent-workspace/chat', '/ai/agent-workspace/tool')
    OR resource.perms IN (
      'ai:skill:list', 'ai:skill:add', 'ai:skill:edit', 'ai:skill:delete', 'ai:skill:ai-generate',
      'ai:engine:stream', 'ai:engine:resume',
      'ai:agent:tool:list'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = admin_role.id
      AND existing.resource_id = resource.id
  );
