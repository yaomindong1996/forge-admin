-- ============================================
-- Go-View 数据可视化大屏数据库初始化脚本
-- 数据库: forge_goview
-- ============================================


-- ============================================
-- 1. 项目管理表
-- ============================================
DROP TABLE IF EXISTS `ai_report_project`;
CREATE TABLE `ai_report_project` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `project_name` VARCHAR(100) NOT NULL COMMENT '项目名称',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `index_img` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',

  -- 画布配置
  `canvas_width` INT NOT NULL DEFAULT 1920 COMMENT '画布宽度',
  `canvas_height` INT NOT NULL DEFAULT 1080 COMMENT '画布高度',
  `background_color` VARCHAR(20) DEFAULT '#1e1e2e' COMMENT '背景颜色',

  -- 组件数据 JSON
  `component_data` LONGTEXT COMMENT '组件列表JSON',

  -- 发布相关
  `publish_status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '发布状态（0未发布 1已发布）',
  `publish_url` VARCHAR(500) DEFAULT NULL COMMENT '发布地址',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',

  -- 基础字段
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `create_dept` BIGINT DEFAULT NULL COMMENT '创建部门',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',

  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='go-view项目表';

-- ============================================
-- 2. AI 供应商配置表
-- ============================================
DROP TABLE IF EXISTS `ai_provider`;
CREATE TABLE `ai_provider` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `provider_name` VARCHAR(50) NOT NULL COMMENT '供应商名称（如 阿里百炼、OpenAI）',
  `provider_type` VARCHAR(30) NOT NULL COMMENT '类型（openai/azure/dashscope/ollama）',
  `api_key` VARCHAR(500) NOT NULL COMMENT 'API Key',
  `base_url` VARCHAR(500) DEFAULT NULL COMMENT 'API Base URL',
  `models` JSON DEFAULT NULL COMMENT '可用模型列表 [{"name":"qwen-plus","enabled":true}]',
  `is_default` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否默认供应商（0否 1是）',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',

  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI供应商配置表';

-- ============================================
-- 3. AI Agent 配置表
-- ============================================
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `agent_name` VARCHAR(100) NOT NULL COMMENT 'Agent名称',
  `agent_code` VARCHAR(50) NOT NULL COMMENT 'Agent编码（唯一）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `system_prompt` LONGTEXT NOT NULL COMMENT '系统提示词模板',
  `provider_id` BIGINT DEFAULT NULL COMMENT '关联供应商ID',
  `model_name` VARCHAR(100) DEFAULT NULL COMMENT '使用的模型',
  `temperature` DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度参数（0-1）',
  `max_tokens` INT DEFAULT 4000 COMMENT '最大Token数',
  `extra_config` JSON DEFAULT NULL COMMENT '扩展配置',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_code` (`agent_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI Agent配置表';

-- 预置数据：大屏生成 Agent
INSERT INTO `ai_agent` (
  `id`, `tenant_id`, `agent_name`, `agent_code`, `description`,
  `system_prompt`, `model_name`, `temperature`, `max_tokens`,
  `status`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`
) VALUES (
  1, 0, '大屏生成助手', 'dashboard_generator', '根据用户需求自动生成数据可视化大屏布局',
  '你是一个数据可视化大屏设计专家。用户会描述他们想要的数据大屏，你需要根据描述设计布局并输出JSON格式的组件配置。

## 输出 JSON 格式
你必须严格输出以下格式的 JSON（不要输出任何其他文字，不要用 markdown 代码块包裹）：

{
  "title": "大屏标题",
  "canvasConfig": {
    "width": 1920,
    "height": 1080,
    "background": "#1e1e2e"
  },
  "components": [
    {
      "key": "组件key",
      "x": 左上角x坐标,
      "y": 左上角y坐标,
      "w": 宽度,
      "h": 高度,
      "title": "组件显示标题",
      "option": { "dataset": { ... 根据组件类型填入数据 ... } }
    }
  ]
}

## 布局设计规则
1. 大屏顶部居中放置一个 TextCommon 作为标题（y=20, fontSize=32-40）
2. 标题区高度约 80px，剩余区域分为 2-3 列排列图表
3. 同行组件之间留 20-30px 间距
4. 数据要真实合理，符合大屏主题
5. 只输出纯 JSON，不要有任何额外文字或注释',
  'qwen-plus', 0.70, 4000,
  '0', 1, NOW(), 1, NOW(), '0'
);

-- ============================================
-- 4. AI 对话记录表
-- ============================================
DROP TABLE IF EXISTS `ai_chat_record`;
CREATE TABLE `ai_chat_record` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `agent_code` VARCHAR(50) DEFAULT NULL COMMENT 'Agent编码',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色（user/assistant/system）',
  `content` LONGTEXT NOT NULL COMMENT '消息内容',
  `token_usage` INT DEFAULT NULL COMMENT 'Token消耗',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',

  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI对话记录表';
