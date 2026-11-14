-- AI 模型能力、确定性路由、健康治理与安全调用审计。

SET @column_exists = (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ai_model' AND column_name = 'context_window');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE ai_model ADD COLUMN context_window int DEFAULT NULL COMMENT ''上下文窗口Token数'' AFTER max_tokens', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ai_model' AND column_name = 'input_price_per_million_cent');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE ai_model ADD COLUMN input_price_per_million_cent bigint DEFAULT NULL COMMENT ''输入价格（分/百万Token）'' AFTER context_window', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ai_model' AND column_name = 'output_price_per_million_cent');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE ai_model ADD COLUMN output_price_per_million_cent bigint DEFAULT NULL COMMENT ''输出价格（分/百万Token）'' AFTER input_price_per_million_cent', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ai_agent' AND column_name = 'model_selection_mode');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE ai_agent ADD COLUMN model_selection_mode varchar(16) NOT NULL DEFAULT ''PINNED'' COMMENT ''模型选择模式（PINNED/POLICY）'' AFTER model_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ai_agent' AND column_name = 'route_policy_id');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE ai_agent ADD COLUMN route_policy_id bigint DEFAULT NULL COMMENT ''模型路由策略ID'' AFTER model_selection_mode', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS ai_model_capability (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL DEFAULT 1,
  model_id bigint NOT NULL,
  capability_code varchar(64) NOT NULL,
  config_json json DEFAULT NULL,
  status char(1) NOT NULL DEFAULT '0',
  del_flag char(1) NOT NULL DEFAULT '0',
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = '0' THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept bigint DEFAULT NULL,
  update_by bigint DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_model_capability_active (tenant_id, model_id, capability_code, logic_delete_active),
  KEY idx_ai_model_capability_model (tenant_id, model_id, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型能力关系';

CREATE TABLE IF NOT EXISTS ai_model_route_policy (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL DEFAULT 1,
  policy_code varchar(64) NOT NULL,
  policy_name varchar(128) NOT NULL,
  required_capabilities json DEFAULT NULL,
  status char(1) NOT NULL DEFAULT '0',
  remark varchar(500) DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = '0' THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept bigint DEFAULT NULL,
  update_by bigint DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_route_policy_code_active (tenant_id, policy_code, logic_delete_active),
  KEY idx_ai_route_policy_status (tenant_id, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型路由策略';

CREATE TABLE IF NOT EXISTS ai_model_route_target (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL DEFAULT 1,
  policy_id bigint NOT NULL,
  model_id bigint NOT NULL,
  priority int NOT NULL DEFAULT 100,
  status char(1) NOT NULL DEFAULT '0',
  del_flag char(1) NOT NULL DEFAULT '0',
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = '0' THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept bigint DEFAULT NULL,
  update_by bigint DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_route_target_active (tenant_id, policy_id, model_id, logic_delete_active),
  KEY idx_ai_route_target_order (tenant_id, policy_id, status, del_flag, priority, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型路由显式候选';

CREATE TABLE IF NOT EXISTS ai_model_invocation_log (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL,
  request_id varchar(64) NOT NULL,
  user_id bigint DEFAULT NULL,
  agent_code varchar(64) NOT NULL,
  session_id varchar(80) DEFAULT NULL,
  phase varchar(24) NOT NULL,
  dispatched tinyint NOT NULL DEFAULT 0,
  route_source varchar(32) DEFAULT NULL,
  route_reason varchar(64) DEFAULT NULL,
  route_policy_id bigint DEFAULT NULL,
  provider_id bigint DEFAULT NULL,
  model_id bigint DEFAULT NULL,
  provider_model_id varchar(128) DEFAULT NULL,
  adapter_code varchar(32) DEFAULT NULL,
  outcome varchar(24) NOT NULL,
  error_category varchar(32) DEFAULT NULL,
  http_status int DEFAULT NULL,
  error_code varchar(64) DEFAULT NULL,
  latency_ms bigint NOT NULL DEFAULT 0,
  prompt_tokens bigint DEFAULT NULL,
  completion_tokens bigint DEFAULT NULL,
  total_tokens bigint DEFAULT NULL,
  usage_available tinyint NOT NULL DEFAULT 0,
  cost_available tinyint NOT NULL DEFAULT 0,
  input_price_per_million_cent bigint DEFAULT NULL,
  output_price_per_million_cent bigint DEFAULT NULL,
  create_by bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept bigint DEFAULT NULL,
  update_by bigint DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_invocation_request (tenant_id, request_id),
  KEY idx_ai_invocation_tenant_time (tenant_id, create_time),
  KEY idx_ai_invocation_model_time (tenant_id, model_id, create_time),
  KEY idx_ai_invocation_agent_time (tenant_id, agent_code, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型调用治理日志';

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 'AI模型能力' dict_name, 'ai_model_capability_type' dict_type, 'AI模型可路由能力' remark
  UNION ALL SELECT 1, 'Agent模型选择模式', 'ai_agent_model_selection_mode', '固定模型或路由策略'
  UNION ALL SELECT 1, 'AI模型健康状态', 'ai_model_health_status', '模型健康状态'
  UNION ALL SELECT 1, 'AI调用结果', 'ai_invocation_outcome', '模型调用结果'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.tenant_id = seed.tenant_id AND t.dict_type = seed.dict_type);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '流式输出' dict_label, 'streaming' dict_value, 'ai_model_capability_type' dict_type, 'info' list_class, 'N' is_default, '支持流式响应' remark
  UNION ALL SELECT 1, 2, '推理', 'reasoning', 'ai_model_capability_type', 'warning', 'N', '支持推理模型'
  UNION ALL SELECT 1, 3, '工具调用', 'tool_calling', 'ai_model_capability_type', 'success', 'N', '支持Tool Calling'
  UNION ALL SELECT 1, 4, '视觉', 'vision', 'ai_model_capability_type', 'error', 'N', '支持视觉输入'
  UNION ALL SELECT 1, 5, '结构化输出', 'structured_output', 'ai_model_capability_type', 'primary', 'N', '支持结构化输出'
  UNION ALL SELECT 1, 1, '固定模型', 'PINNED', 'ai_agent_model_selection_mode', 'info', 'Y', '固定供应商和模型'
  UNION ALL SELECT 1, 2, '路由策略', 'POLICY', 'ai_agent_model_selection_mode', 'success', 'N', '按显式候选策略路由'
  UNION ALL SELECT 1, 1, '未知', 'UNKNOWN', 'ai_model_health_status', 'default', 'N', '尚无健康样本'
  UNION ALL SELECT 1, 2, '健康', 'HEALTHY', 'ai_model_health_status', 'success', 'N', '调用健康'
  UNION ALL SELECT 1, 3, '降级', 'DEGRADED', 'ai_model_health_status', 'warning', 'N', '存在失败样本'
  UNION ALL SELECT 1, 4, '熔断', 'OPEN', 'ai_model_health_status', 'error', 'N', '暂停接收调用'
  UNION ALL SELECT 1, 5, '试探', 'HALF_OPEN', 'ai_model_health_status', 'warning', 'N', '等待单次试探'
  UNION ALL SELECT 1, 1, '成功', 'SUCCESS', 'ai_invocation_outcome', 'success', 'N', '调用成功'
  UNION ALL SELECT 1, 2, '失败', 'FAILED', 'ai_invocation_outcome', 'error', 'N', '调用失败'
  UNION ALL SELECT 1, 3, '取消', 'CANCELLED', 'ai_invocation_outcome', 'warning', 'N', '调用方取消'
  UNION ALL SELECT 1, 4, '路由失败', 'ROUTING_FAILED', 'ai_invocation_outcome', 'error', 'N', '未选择可用模型'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '模型治理', COALESCE((SELECT parent_id FROM (SELECT parent_id FROM sys_resource WHERE path = '/ai/provider-model' LIMIT 1) x), 0), 2, 3,
       '/ai/model-routing', 'ai/model-routing', 0, '_self', 0, 1, 1, 'ai:model-routing:list', 'ionicons5:GitNetworkOutline', 1, 0,
       'AI模型路由策略与调用记录', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.path = '/ai/model-routing');

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, menu.id, 3, seed.sort, 0, '_self', 0, 1, 1, seed.perms, 0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/model-routing' LIMIT 1) menu
JOIN (
  SELECT '维护路由策略' resource_name, 1 sort, 'ai:model-routing:edit' perms, '新增、编辑和删除路由策略' remark
  UNION ALL SELECT '预览模型路由', 2, 'ai:model-routing:preview', '只读预览路由决策'
  UNION ALL SELECT '查看调用记录', 3, 'ai:model-invocation:list', '查看安全调用治理记录'
  UNION ALL SELECT '测试模型连接', 4, 'ai:model:test', '手动测试单个模型连接'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.perms = seed.perms);

-- 新治理权限仅显式授予默认租户超级管理员，不向普通角色自动扩散。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1
WHERE resource.client_code = 'pc'
  AND (
    resource.path = '/ai/model-routing'
    OR resource.perms IN (
      'ai:model-routing:edit',
      'ai:model-routing:preview',
      'ai:model-invocation:list',
      'ai:model:test'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = admin_role.id
      AND existing.resource_id = resource.id
  );
