-- ============================================================
-- AI 页面模板表（组件市场化：每个模板定义 AI 提示词约束 + 默认配置）
-- ============================================================
CREATE TABLE `ai_page_template` (
  `id` bigint NOT NULL COMMENT '主键',
  `template_key` varchar(64) NOT NULL COMMENT '模板唯一标识（如 simple-crud / tree-crud）',
  `template_name` varchar(128) NOT NULL COMMENT '模板显示名称',
  `description` varchar(512) DEFAULT '' COMMENT '模板描述',
  `icon` varchar(64) DEFAULT '' COMMENT '图标（mdi: 前缀）',
  `system_prompt` text DEFAULT NULL COMMENT '该模板专属的 AI system prompt 补充',
  `schema_hint` json DEFAULT NULL COMMENT '告诉 AI 该模板可用的字段约束（JSON）',
  `default_config` json DEFAULT NULL COMMENT '默认配置值（JSON，如 modalType/searchGridCols 等）',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用（1启用 0停用）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `is_builtin` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否内置（1内置不可删除）',
  `codegen_type` varchar(16) NOT NULL DEFAULT 'TEMPLATE' COMMENT '代码生成策略：TEMPLATE-Velocity模板 / AI-大模型生成',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_key` (`template_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI页面模板表（组件市场）';

CREATE TABLE `ai_crud_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint DEFAULT 1 COMMENT '租户ID',
  `config_key` varchar(64) NOT NULL COMMENT '配置唯一标识（小写字母+数字+下划线）',
  `table_name` varchar(128) NOT NULL COMMENT '关联的数据库表名',
  `table_comment` varchar(255) DEFAULT '' COMMENT '表描述/功能名称',
  `search_schema` json DEFAULT NULL COMMENT '搜索表单Schema JSON',
  `columns_schema` json DEFAULT NULL COMMENT '表格列Schema JSON',
  `edit_schema` json DEFAULT NULL COMMENT '编辑表单Schema JSON',
  `api_config` json DEFAULT NULL COMMENT 'API配置JSON（method@url格式）',
  `options` json DEFAULT NULL COMMENT '其他AiCrudPage配置（modalType/modalWidth/gridCols等）',
  `mode` varchar(16) NOT NULL DEFAULT 'CONFIG' COMMENT '模式：CONFIG-配置驱动 / CODEGEN-代码生成',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `menu_name` varchar(128) DEFAULT '' COMMENT '菜单名称',
  `menu_parent_id` bigint DEFAULT NULL COMMENT '菜单父级ID',
  `menu_sort` int DEFAULT 0 COMMENT '菜单排序',
  `menu_resource_id` bigint DEFAULT NULL COMMENT '关联的sys_resource记录ID',
  `dict_config` json DEFAULT NULL COMMENT '字典数据配置',
  `desensitize_config` json DEFAULT NULL COMMENT '字段脱敏配置',
  `encrypt_config` json DEFAULT NULL COMMENT '接口加解密配置',
  `trans_config` json DEFAULT NULL COMMENT '字典翻译配置',
  `layout_type` varchar(64) DEFAULT 'simple-crud' COMMENT '页面模板类型（对应 ai_page_template.template_key）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_table_name` (`table_name`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRUD配置驱动表';

-- ============================================================
-- AI CRUD 配置管理菜单初始化数据
-- ============================================================

-- 确保 AI管理 目录菜单存在（如已存在则跳过）
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
SELECT 0, 'AI管理', 0, 1, 60, '/ai', NULL, 0, 0, 1, 1, 'mdi:robot-outline', 0, 1, 'AI管理目录', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 0 AND path = '/ai' AND resource_type = 1);

-- CRUD配置管理菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_time, update_time)
SELECT 0, 'CRUD配置', id, 2, 10, '/ai/crud-config', 'ai/crud-config', 0, 0, 1, 1, 'ai:crud-config:list', 'mdi:cog-outline', 0, 0, 'CRUD配置管理', NOW(), NOW()
FROM sys_resource WHERE tenant_id = 0 AND path = '/ai' AND resource_type = 1 LIMIT 1;

-- 通用CRUD渲染页（隐藏菜单，由配置自动创建的菜单指向此页面）
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
SELECT 0, 'CRUD渲染页', id, 2, 99, '/ai/crud-page', 'ai/crud-page', 0, 1, 0, 0, NULL, 0, 0, '通用CRUD渲染页（隐藏菜单）', NOW(), NOW()
FROM sys_resource WHERE tenant_id = 0 AND path = '/ai' AND resource_type = 1 LIMIT 1;

-- ============================================================
-- crud_config_builder Agent 初始数据
-- ============================================================
INSERT INTO ai_agent (tenant_id, agent_code, agent_name, description, system_prompt, model_id, temperature, max_tokens, enabled, create_time, update_time)
VALUES (1, 'crud_config_builder', 'CRUD配置生成器', '根据自然语言描述或数据库表结构生成CRUD配置JSON',
'你是一个CRUD配置生成专家。根据用户的描述或数据库表结构，生成符合AiCrudPage组件规范的CRUD配置JSON。

配置JSON必须包含以下字段：
- tableName: 表名
- tableComment: 表描述
- searchSchema: 搜索表单配置数组，每项包含field/label/type，可选dictType
- columnsSchema: 表格列配置数组，每项包含key/title/dataIndex，最后一列为操作列(key=actions)
- editSchema: 编辑表单配置数组，每项包含field/label/type/required，可选dictType/props
- apiConfig: API配置对象

搜索type可选：input/select/daterange/number
编辑type可选：input/textarea/select/radio/checkbox/switch/date/datetime/number/upload/imageUpload/fileUpload/cascader/treeSelect
基类字段(id/tenant_id/create_by/create_time/create_dept/update_by/update_time/del_flag)不进editSchema
有dictType的select字段必须加上dictType属性

请仅输出JSON，不要输出其他内容。', NULL, 0.3, 4096, 1, NOW(), NOW());

-- CRUD 生成器菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_time, update_time)
SELECT 0, 'CRUD生成器', id, 2, 11, '/ai/crud-generator', 'ai/crud-generator', 0, 0, 1, 1, 'ai:crud-generator:use', 'mdi:magic-staff', 0, 0, 'AI CRUD 配置生成器', NOW(), NOW()
FROM sys_resource WHERE tenant_id = 0 AND path = '/ai' AND resource_type = 1 LIMIT 1;

-- 原 CRUD 配置菜单排序调整为 12
UPDATE sys_resource SET sort = 12 WHERE tenant_id = 0 AND path = '/ai/crud-config' AND resource_type = 2;

-- ============================================================
-- crud_config_builder 上下文 SPEC 配置（提示词规范）
-- 说明：这里只初始化输出格式和字段规范，可在前端【AI管理 - 上下文配置】界面对内容进行自定义修改
-- ============================================================
INSERT INTO ai_context_config (tenant_id, agent_code, config_name, config_content, config_type, sort, status, create_time, update_time)
VALUES (1, 'crud_config_builder', 'CRUD配置输出格式规范',
'## 输出格式要求
请严格按以下格式分段输出，每个阶段前加标记：

[STAGE:meta]
{"configKey": "xxx", "tableName": "xxx", "tableComment": "xxx"}

[STAGE:searchSchema]
搜索表单配置 JSON 数组

[STAGE:columnsSchema]
表格列配置 JSON 数组

[STAGE:editSchema]
编辑表单配置 JSON 数组

[STAGE:apiConfig]
API配置 JSON 对象

[STAGE:dictConfig]
字典数据配置 JSON 数组，每项包含 dictType, dictName, isNew, items[]

[STAGE:desensitizeConfig]
字段脱敏配置 JSON 对象，key 为字段名，value 包含 type(PHONE/ID_CARD/EMAIL/BANK_CARD/NAME/ADDRESS/PASSWORD/CAR_LICENSE/CUSTOM)

[STAGE:encryptConfig]
接口加解密配置 JSON 对象，包含 enableEncrypt, enableDecrypt, operations[]

[STAGE:transConfig]
字典翻译配置 JSON 对象，key 为字段名，value 包含 dictType, targetField

[STAGE:createTableSql]
CREATE TABLE 语句（如没有关联表或表不存在，请生成建表SQL）

## 字段规范

### meta
- configKey: 小写字母开头，仅含小写字母+数字+下划线
- tableName: 数据库表名，小写，下划线分隔
- tableComment: 表的中文描述

### searchSchema
- 每项包含: field(字段名 camelCase), label(中文标签), type(input/select/daterange/number)
- 有字典的 select 字段必须加 dictType 属性
- 只选择最常用的 3-5 个搜索条件

### columnsSchema
- 每项包含: key(camelCase), title(中文标签), dataIndex(同 key)
- 有字典的列加 render: {type: "dictTag", dictType: "xxx"}
- 最后一列必须是操作列: {key: "actions", title: "操作", dataIndex: "actions", width: 180, fixed: "right"}

### editSchema
- 每项包含: field(camelCase), label(中文标签), type(input/textarea/select/radio/switch/date/datetime/number), required(boolean)
- 有字典的字段加 dictType 属性
- 基类字段不包含: id, tenantId, createBy, createTime, createDept, updateBy, updateTime, delFlag

### apiConfig
- 格式为 method@url
- 必须包含: list(分页)、detail(详情)、create(新廿)、update(编辑)、delete(删除)

### dictConfig
- 从 schema 中的 dictType 提取，生成字典类型和字典项
- isNew=true 表示需要新建到字典表；isNew=false 表示映射已有字典
- items 中每项包含 dictLabel, dictValue, dictSort

### desensitizeConfig
- 自动识别敏感字段并生成对应脱敏规则
- PHONE(手机号): 186****2213
- ID_CARD(身份证): 110101********0011
- EMAIL(邮箱): t***@example.com
- BANK_CARD(银行卡): 6222 **** **** 8888
- NAME(姓名): 张**
- ADDRESS(地址): 北京市海淀区******
- PASSWORD(密码): ********
- CUSTOM(自定义): 需指定 prefixKeep 和 suffixKeep

### encryptConfig
- enableEncrypt: 是否启用响应加密
- enableDecrypt: 是否启用请求解密
- operations: 需要加解密的操作列表，空数组表示全部 ["list","detail","create","update","delete"]

### transConfig
- key 为原始字段名（如 status）
- dictType: 对应字典类型
- targetField: 翻译后的字段名（如 statusName），默认 原字段名 + Name

### createTableSql
- 生成标准 MySQL CREATE TABLE 语句
- 必须包含基类字段: id(主键自增), tenant_id, create_by, create_time, create_dept, update_by, update_time, del_flag
- 使用 utf8mb4 字符集和 InnoDB 引擎

## 重要约束
- 直接输出纯 JSON/SQL，不要用 markdown 代码块包裹
- 不要输出任何解释性文字，只输出 [STAGE:xxx] 标记和内容
- 每个 STAGE 后面紧跟该段的内容，不要包在外层对象中
'field/key/dataIndex 使用 camelCase 命名',
'SPEC', 1, '0', NOW(), NOW());

-- ============================================================
-- 增量 DDL：已有环境添加 layout_type 字段（新建表可跳过）
-- ============================================================
-- ALTER TABLE ai_crud_config ADD COLUMN layout_type varchar(64) DEFAULT 'simple-crud' COMMENT '页面模板类型' AFTER trans_config;

-- ============================================================
-- ai_page_template 内置模板初始数据
-- ============================================================
INSERT INTO ai_page_template (id, template_key, template_name, description, icon, system_prompt, schema_hint, default_config, enabled, sort, is_builtin, codegen_type, create_time, update_time)
VALUES
(1, 'simple-crud', '标准 CRUD', '适合平坦型数据的属性增删改查，支持搜索、表格列表、弹窗/抒屉表单编辑',
 'mdi:table',
 '当前模板为「标准 CRUD」，请确保：
1. columnsSchema 包含内容列 + 操作列(actions)
2. editSchema 简洁清晰，不要嵌套
3. modalType 默认为 drawer
4. 尽量岑减搜索条件，保留最常用的3-5个',
 NULL,
 '{
  "modalType": "drawer",
  "modalWidth": "800px",
  "searchGridCols": 4,
  "editGridCols": 1
}',
 1, 1, 1, 'TEMPLATE', NOW(), NOW()),
(2, 'tree-crud', '树形 CRUD', '适合具有父子层级结构的数据，如部门结构、分类管理等，左树右表格布局',
 'mdi:file-tree',
 '当前模板为「树形 CRUD」，请确保：
1. editSchema 中必须包含 parentId 字段（type: treeSelect）
2. editSchema 中包含 sort 排序字段
3. columnsSchema 中包含 层级深度或父层名称列
4. apiConfig 中包含 tree 接口（查询树形结构）
5. treeConfig 配置：{"keyField": "id", "parentField": "parentId", "labelField": "name"}
6. 数据必须有 parent_id 字段，不要导入进 editSchema
7. 模式必须为 modal',
 '{
  "treeConfig": {
    "keyField": "id",
    "parentField": "parentId",
    "labelField": "name"
  }
}',
 '{
  "modalType": "modal",
  "modalWidth": "600px",
  "searchGridCols": 3,
  "editGridCols": 1
}',
 1, 2, 1, 'TEMPLATE', NOW(), NOW());

-- 模板管理菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_time, update_time)
SELECT 0, '页面模板', id, 2, 13, '/ai/page-template', 'ai/page-template', 0, 0, 1, 1, 'ai:page-template:list', 'mdi:puzzle-outline', 0, 0, 'AI页面模板管理', NOW(), NOW()
FROM sys_resource WHERE tenant_id = 0 AND path = '/ai' AND resource_type = 1 LIMIT 1;

-- ============================================================
-- 升级语句：如果不是新建库，执行以下 ALTER 语句
-- 只需执行一次！
-- ============================================================
-- ALTER TABLE ai_page_template ADD COLUMN codegen_type varchar(16) NOT NULL DEFAULT 'TEMPLATE'
--   COMMENT '代码生成策略：TEMPLATE-Velocity模板 / AI-大模型生成' AFTER is_builtin;
-- UPDATE ai_page_template SET codegen_type = 'TEMPLATE' WHERE codegen_type IS NULL OR codegen_type = '';
