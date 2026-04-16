-- forge_admin_new.ai_agent definition

CREATE TABLE `ai_agent`
(
    `id`            bigint                                  NOT NULL COMMENT '主键ID',
    `tenant_id`     bigint                                  NOT NULL DEFAULT '0' COMMENT '租户ID',
    `agent_name`    varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Agent名称',
    `agent_code`    varchar(50) COLLATE utf8mb4_general_ci  NOT NULL COMMENT 'Agent编码（唯一）',
    `description`   varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '描述',
    `system_prompt` longtext COLLATE utf8mb4_general_ci     NOT NULL COMMENT '系统提示词模板',
    `provider_id`   bigint                                           DEFAULT NULL COMMENT '关联供应商ID',
    `model_name`    varchar(100) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '使用的模型',
    `temperature`   decimal(3, 2)                                    DEFAULT '0.70' COMMENT '温度参数（0-1）',
    `max_tokens`    int                                              DEFAULT '4000' COMMENT '最大Token数',
    `extra_config`  json                                             DEFAULT NULL COMMENT '扩展配置',
    `status`        char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_by`     bigint                                           DEFAULT NULL COMMENT '创建者',
    `create_time`   datetime                                         DEFAULT NULL COMMENT '创建时间',
    `update_by`     bigint                                           DEFAULT NULL COMMENT '更新者',
    `update_time`   datetime                                         DEFAULT NULL COMMENT '更新时间',
    `del_flag`      char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    `create_dept`   varchar(30) COLLATE utf8mb4_general_ci           DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_code` (`agent_code`),
    KEY             `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI Agent配置表';


-- forge_admin_new.ai_chat_record definition

CREATE TABLE `ai_chat_record`
(
    `id`          bigint                                 NOT NULL COMMENT '主键ID',
    `tenant_id`   bigint                                 NOT NULL DEFAULT '0' COMMENT '租户ID',
    `user_id`     bigint                                 NOT NULL COMMENT '用户ID',
    `agent_code`  varchar(50) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT 'Agent编码',
    `session_id`  varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID',
    `role`        varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色（user/assistant/system）',
    `content`     longtext COLLATE utf8mb4_general_ci    NOT NULL COMMENT '消息内容',
    `token_usage` int                                             DEFAULT NULL COMMENT 'Token消耗',
    `create_time` datetime                                        DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY           `idx_session_id` (`session_id`),
    KEY           `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI对话记录表';


-- forge_admin_new.ai_chat_session definition

CREATE TABLE `ai_chat_session`
(
    `id`           varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID（UUID，由前端或服务端生成）',
    `tenant_id`    bigint                                 NOT NULL DEFAULT '0' COMMENT '租户ID',
    `user_id`      bigint                                 NOT NULL COMMENT '用户ID',
    `agent_code`   varchar(50) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '关联的 Agent 编码',
    `session_name` varchar(200) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '会话标题（首条消息截取）',
    `status`       char(1) COLLATE utf8mb4_general_ci     NOT NULL DEFAULT '0' COMMENT '状态（0正常 1已删除）',
    `create_time`  datetime                                        DEFAULT NULL COMMENT '创建时间',
    `update_time`  datetime                                        DEFAULT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY            `idx_user_id` (`user_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI会话表';


-- forge_admin_new.ai_provider definition

CREATE TABLE `ai_provider`
(
    `id`            bigint                                  NOT NULL COMMENT '主键ID',
    `tenant_id`     bigint                                  NOT NULL DEFAULT '0' COMMENT '租户ID',
    `provider_name` varchar(50) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '供应商名称（如 阿里百炼、OpenAI）',
    `provider_type` varchar(30) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '类型（openai/azure/dashscope/ollama）',
    `api_key`       varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API Key',
    `base_url`      varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT 'API Base URL',
    `models`        json                                             DEFAULT NULL COMMENT '可用模型列表 [{"name":"qwen-plus","enabled":true}]',
    `is_default`    char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '是否默认供应商（0否 1是）',
    `status`        char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `remark`        varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '备注',
    `create_by`     bigint                                           DEFAULT NULL COMMENT '创建者',
    `create_time`   datetime                                         DEFAULT NULL COMMENT '创建时间',
    `update_by`     bigint                                           DEFAULT NULL COMMENT '更新者',
    `update_time`   datetime                                         DEFAULT NULL COMMENT '更新时间',
    `del_flag`      char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    `default_model` varchar(100) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '默认模型名称',
    `create_dept`   varchar(30) COLLATE utf8mb4_general_ci           DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY             `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI供应商配置表';


-- forge_admin_new.ai_report_project definition

CREATE TABLE `ai_report_project`
(
    `id`               bigint                                  NOT NULL COMMENT '主键ID',
    `tenant_id`        bigint                                  NOT NULL DEFAULT '0' COMMENT '租户ID',
    `project_name`     varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '项目名称',
    `remark`           varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '备注',
    `index_img`        varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '封面图URL',
    `status`           char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `canvas_width`     int                                     NOT NULL DEFAULT '1920' COMMENT '画布宽度',
    `canvas_height`    int                                     NOT NULL DEFAULT '1080' COMMENT '画布高度',
    `background_color` varchar(20) COLLATE utf8mb4_general_ci           DEFAULT '#1e1e2e' COMMENT '背景颜色',
    `component_data`   longtext COLLATE utf8mb4_general_ci COMMENT '组件列表JSON',
    `publish_status`   char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '发布状态（0未发布 1已发布）',
    `publish_url`      varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '发布地址',
    `publish_time`     datetime                                         DEFAULT NULL COMMENT '发布时间',
    `create_by`        bigint                                           DEFAULT NULL COMMENT '创建者',
    `create_time`      datetime                                         DEFAULT NULL COMMENT '创建时间',
    `update_by`        bigint                                           DEFAULT NULL COMMENT '更新者',
    `update_time`      datetime                                         DEFAULT NULL COMMENT '更新时间',
    `create_dept`      bigint                                           DEFAULT NULL COMMENT '创建部门',
    `del_flag`         char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`),
    KEY                `idx_tenant_id` (`tenant_id`),
    KEY                `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='go-view项目表';

INSERT INTO ai_agent (id, tenant_id, agent_name, agent_code, description, system_prompt, provider_id, model_name, temperature, max_tokens, extra_config, status, create_by, create_time, update_by, update_time, del_flag, create_dept) VALUES(1, 1, '大屏生成助手', 'dashboard_generator', '根据用户需求自动生成数据可视化大屏布局', '你是一个数据可视化大屏设计专家。用户会描述他们想要的数据大屏，你需要根据描述选择合适的组件并设计布局，输出一个 JSON 对象。

## 画布信息
- 画布尺寸: {{canvasWidth}}px × {{canvasHeight}}px
- 坐标原点在左上角，x 向右增大，y 向下增大
- 组件使用绝对定位，请合理安排布局，不要重叠

## 风格要求
- 当前主题风格: {{styleLabel}}
- 背景建议: {{backgroundSuggestion}}
- 文字建议: {{textColorSuggestion}}

## 用户需求
{{prompt}}

## 当前画布配置
{{canvasContext}}

## 可用组件列表（只能使用以下组件 key）
{{componentCatalog}}

## 输出 JSON 格式
你必须严格输出以下格式的 JSON（不要输出任何其他文字，不要用 markdown 代码块包裹）：

{
  "title": "大屏标题",
  "canvasConfig": {
    "width": {{canvasWidth}},
    "height": {{canvasHeight}},
    "background": "{{backgroundColor}}"
  },
  "components": [
    {
      "key": "组件key",
      "x": 左上角x坐标,
      "y": 左上角y坐标,
      "w": 宽度,
      "h": 高度,
      "title": "组件显示标题",
      "option": { ... 根据组件类型填入，见下方规则 ... }
    }
  ]
}

## 各组件 option 填写规则（非常重要！）

### 图表类组件（BarCommon, BarCrossrange, BarLine, CapsuleChart, LineCommon, LineGradientSingle, LineGradients, PieCommon, PieCircle, ScatterCommon, Heatmap, TreeMap, Funnel, MapBase 等）
这些是 ECharts 图表组件，option 中 **只需要填 dataset 字段**，不需要填 series/tooltip/xAxis/yAxis 等配置（系统会自动补充）。
- dataset 格式: { "dimensions": ["列名1", "列名2", ...], "source": [{"列名1": 值, "列名2": 值}, ...] }
- dimensions 的第一个元素是类目名（如"月份"、"地区"），其余是数据系列名
- 示例: { "option": { "dataset": { "dimensions": ["月份", "销售额", "利润"], "source": [{"月份": "1月", "销售额": 820, "利润": 320}] } } }

### 饼图 PieCommon / PieCircle
- dataset 只需要 2 个 dimensions: 类目名 + 数值
- 示例: { "option": { "dataset": { "dimensions": ["地区", "销售额"], "source": [{"地区": "华东", "销售额": 3350}] } } }

### 雷达图 Radar
- dataset 使用特殊格式: { "radarIndicator": [{ "name": "指标名", "max": 100 }], "seriesData": [{ "name": "系列名", "value": [80, 90] }] }

### 文本组件 TextCommon
- option.dataset 填入要显示的文字内容（字符串）
- 可选: option.fontSize, option.fontColor, option.fontWeight, option.textAlign, option.letterSpacing

### 数字翻牌器 FlipperNumber
- option.dataset 填入要显示的数字，可选 option.unit (单位文字)

### 时钟 Clock / 倒计时 CountDown
- 不需要 option

### 装饰边框 Border01-Border13
- 不需要 option，只需位置和尺寸，用于包裹图表

### 排行榜 TableList
- option.dataset 填入对象数组: [{ "name": "项目名", "value": 数值 }, ...]

### 滚动表格 TableScrollBoard
- option.header 填入表头数组: ["列1", "列2", "列3"]
- option.dataset 填入二维数组（每行是字符串数组）: [["行1列1", "行1列2", "行1列3"], ["行2列1", "行2列2", "行2列3"]]

## 布局设计规则
1. 大屏顶部居中放置一个 TextCommon 作为标题（y=20, fontSize=32-40, fontColor=#ffffff, fontWeight=bold）
2. 标题区高度约 80px，剩余区域分为 2-3 列排列图表
3. 同行组件之间留 20-30px 间距
4. 数据要真实合理，符合大屏主题
5. 只输出纯 JSON，不要有任何额外文字或注释', NULL, 'qwen-plus', 0.70, 4000, NULL, '0', 1, '2026-04-13 17:38:55', 1, '2026-04-13 17:38:55', '0', NULL);


