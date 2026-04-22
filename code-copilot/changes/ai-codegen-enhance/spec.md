# AI 代码生成器基础增强
> status: apply
> created: 2026-04-20
> complexity: 🔴复杂

## 1. 背景与目标

### 为什么做
当前代码生成器（`forge-plugin-generator`）采用纯 Velocity 模板填充模式，字段类型推断、表单组件推荐、字典识别等全靠硬编码规则（`GenUtils`），智能化程度低：
- DB 类型到 Java 类型的映射过于简单（如所有数值类型都映射为 INPUT）
- HTML 组件推荐几乎全部返回 `INPUT`，无法根据语义推荐 SELECT/DATETIME/IMAGE 等
- 字典类型仅从注释括号中解析，识别率低
- 无自然语言输入能力，用户必须先有数据库表才能生成代码

同时，现有 AI 能力（`forge-plugin-ai`）与"大屏生成"场景硬耦合，没有通用 AiClient 抽象，其他模块（如代码生成器）无法复用 AI 能力。

### 做完后的效果（可验证）
1. **通用 AiClient**：从 `AiChatService` 重构出统一的 `AiClient` 接口，支持流式/非流式调用，所有场景通过 Agent 配置驱动
2. **AI 辅助字段推荐**：导入表后，AI 自动推荐 JavaType/HtmlType/DictType/验证规则等，用户可审核修改后再生成
3. **自然语言到 Schema**：用户输入自然语言描述，AI 单轮推断数据模型 Schema，不满意可追问优化
4. **上下文感知生成**：AI 生成时可引用项目 code-style（文件）和 SPEC 上下文（DB），生成代码符合项目规范
5. **显式降级**：AI 不可用时提示用户并回退到现有 `GenUtils` 规则推断，不阻塞生成流程

## 2. 代码现状（Research Findings）

### 2.1 AI 模块（forge-plugin-ai）

#### 2.1.1 供应商与模型管理
- **AiProvider**：供应商实体，含 `baseUrl`/`apiKey`/`defaultModel`/`isDefault`/`status` 等字段
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/domain/AiProvider.java`
  - 继承 `TenantEntity`，支持多租户
- **AiProviderService**：供应商 CRUD 服务，含 `getDefaultProvider()` 方法
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/service/AiProviderService.java:26`
- **AiModel**：模型实体，含 `providerId`/`modelId`/`modelName`/`modelType`/`maxTokens` 等
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/domain/AiModel.java`

#### 2.1.2 Agent 管理
- **AiAgent**：Agent 实体，含 `agentCode`/`agentName`/`systemPrompt`/`providerId`/`modelName`/`temperature`/`maxTokens`/`extraConfig`
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/domain/AiAgent.java`
  - `extraConfig` 为 String 类型，可存 JSON 扩展配置
- **AiAgentService**：Agent 服务，仅提供 `getByCode()` 方法
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/service/AiAgentService.java:15`

#### 2.1.3 对话服务（核心问题所在）
- **AiChatService**：AI 对话服务，**与"大屏生成"场景强耦合**
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/service/AiChatService.java`
  - `buildChatClient()` 方法（第 129-162 行）：每次调用都新建 `OpenAiApi` + `OpenAiChatModel` + `ChatClient`，无缓存/池化
  - `generateDashboard()` 方法（第 208-224 行）：硬编码使用 `dashboard_generator` Agent
  - `generateDashboardStream()` 方法（第 229-261 行）：硬编码使用 `dashboard_generator` Agent
  - `chatStream()` 方法（第 266-299 行）：通用对话方法，但 systemPrompt 的默认值硬编码为大屏助手
  - `resolveProvider()`/`resolveModel()`/`resolveTemperature()`/`resolveMaxTokens()`：解析逻辑可复用，但嵌套在 Service 中
  - `executeStreamChat()` 方法（第 304-346 行）：公共流式处理逻辑，可提取
  - `persistChatConversation()` 方法（第 348-382 行）：对话记录持久化，可复用
- **AiPromptTemplateRenderer**：提示词模板渲染，仅支持大屏场景的变量替换
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/service/AiPromptTemplateRenderer.java`
- **DbChatMemory**：基于 DB 的对话记忆实现
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/memory/DbChatMemory.java`
- **AIGenerateRequest**：生成请求 DTO，字段全是大屏专用（canvasWidth/canvasHeight/style/componentCatalog）
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/dto/AIGenerateRequest.java`

#### 2.1.4 对话控制器
- **AiChatController**：AI 对话入口，`/ai/generate` 和 `/ai/generate/stream` 仅服务大屏场景
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/controller/AiChatController.java`
  - `/ai/chat/stream` 是通用对话接口，但参数设计偏大屏

### 2.2 代码生成器模块（forge-plugin-generator）

#### 2.2.1 核心服务
- **GenTableServiceImpl**：代码生成核心服务
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/impl/GenTableServiceImpl.java`
  - `importGenTable()` 方法（第 48-111 行）：导入表结构，调用 `GenUtils.initTable()` 和 `GenUtils.initColumnField()` 初始化
  - `generatorCode()` 方法（第 114-155 行）：Velocity 模板填充，无 AI 介入
  - `previewCode()` 方法（第 194-216 行）：预览生成代码
  - 依赖：`GenTableMapper`/`GenTableColumnMapper`/`GeneratorConfig`/`IGenDatasourceService`

#### 2.2.2 工具类（规则推断的关键问题）
- **GenUtils**：代码生成工具类，**硬编码规则，推断能力弱**
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/util/GenUtils.java`
  - `convertDbTypeToJavaType()` 方法（第 20-48 行）：简单的 DB→Java 类型映射，如 int→Integer, bigint→Long
  - `getHtmlType()` 方法（第 169-180 行）：**几乎所有类型都返回 INPUT**，仅 LocalDateTime 返回 DATETIME
  - `parseDictTypeFromComment()` 方法（第 137-154 行）：仅从注释括号 `(1:正常,0:停用)` 格式解析字典
  - `initColumnField()` 方法（第 101-131 行）：字段初始化，调用上述三个方法
  - `isSuperColumn()` 方法（第 159-164 行）：判断基类字段
  - `initTable()` 方法（第 86-96 行）：表信息初始化

#### 2.2.3 模板引擎
- **VelocityUtils**：Velocity 模板引擎工具
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/util/VelocityUtils.java`
  - `prepareContext()` 方法（第 32-69 行）：构建 Velocity 上下文，注入 tableName/className/columns 等
  - `getTemplateList()` 方法（第 93-107 行）：硬编码 11 个模板文件路径
  - `renderTemplate()` 方法（第 149-153 行）：渲染单个模板

#### 2.2.4 配置
- **GeneratorConfig**：代码生成器配置，仅含基础配置项
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/config/GeneratorConfig.java`
  - 配置项：`author`/`packageName`/`moduleName`/`templateEngine`/`autoRemovePrefix`/`tablePrefix`/`basePath`
  - **缺失**：无 code-style / SPEC 上下文 / AI 相关配置

#### 2.2.5 数据模型
- **GenTable**：表配置实体，含 `options`（Map 类型，可存扩展配置）
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/GenTable.java`
- **GenTableColumn**：字段配置实体，含 `javaType`/`htmlType`/`dictType`/`queryType`/`isRequired` 等 AI 可增强的字段
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/GenTableColumn.java`
- **GenTemplate**：模板配置实体，支持 DB 管理模板
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/GenTemplate.java`

### 2.3 前端代码生成页面

- **table.vue**：代码生成主页面，使用 `AiCrudPage` 组件
  - 文件：`forge-admin-ui/src/views/generator/table.vue`
  - 操作：配置/字段/预览/生成/删除，无 AI 入口
- **ColumnConfigModal.vue**：字段配置弹窗，手动选择 JavaType/HtmlType/DictType 等
  - 文件：`forge-admin-ui/src/views/generator/components/ColumnConfigModal.vue`
  - 当前所有配置项均为手动操作，无 AI 推荐按钮
- **ImportTableModal.vue**：导入表弹窗，选择数据源后勾选表导入
  - 文件：`forge-admin-ui/src/views/generator/components/ImportTableModal.vue`
- **CodePreviewModal.vue**：代码预览弹窗，使用 CodeMirror 展示
  - 文件：`forge-admin-ui/src/views/generator/components/CodePreviewModal.vue`

### 2.4 前端 AI API

- **ai.js**：前端 AI 接口定义，含供应商管理、模型管理、会话管理的 API
  - 文件：`forge-admin-ui/src/api/ai.js`
  - **缺失**：无通用的 AI 调用接口（如按 Agent 编码调用）

### 2.5 发现与风险

1. **AiChatService 每次新建 ChatClient**：`buildChatClient()` 每次调用都新建 `OpenAiApi` + `ChatModel` + `ChatClient`，无缓存机制，高并发下有性能问题
   - 出处：`AiChatService.java:129-162`
2. **AI 能力与场景耦合**：`AiChatService` 的 `generateDashboard*()` 方法硬编码 `dashboard_generator` Agent，其他场景无法复用
   - 出处：`AiChatService.java:209,229`
3. **AIGenerateRequest 字段大屏专用**：canvasWidth/canvasHeight/style/componentCatalog 等字段不通用
   - 出处：`AIGenerateRequest.java:9-22`
4. **GenUtils.getHtmlType() 推断能力极弱**：仅 LocalDateTime 返回 DATETIME，其余全返回 INPUT
   - 出处：`GenUtils.java:169-180`
5. **GenTableColumn 无验证规则字段**：当前实体没有 `validateRule` 字段，AI 推荐验证规则需要扩展数据模型
   - 出处：`GenTableColumn.java` 全文
6. **GenTemplate 已支持 DB 管理**：模板内容存在数据库中，可通过管理界面编辑，为 AI 生成模板提供了基础
   - 出处：`GenTemplate.java:47` `templateContent` 字段
7. **GenTable.options 已预留扩展**：`options` 字段为 `Map<String, Object>` 类型，可存 AI 推荐的额外配置
   - 出处：`GenTable.java:89-90`

## 3. 功能点

### 3.1 通用 AiClient 重构
- [ ] **F3.1.1 AiClient 接口定义**：抽象统一 AI 调用接口，支持非流式 `call()` 和流式 `stream()` 两种调用方式，输入为 agentCode + 用户消息 + 可选参数，输出为 AI 响应文本
- [ ] **F3.1.2 AiClientImpl 实现**：从 `AiChatService` 中提取 ChatClient 构建、Provider 解析、参数解析等通用逻辑，实现 `AiClient` 接口，通过 Agent 配置驱动（agentCode→AiAgent→Provider/Model/SystemPrompt）
- [ ] **F3.1.3 ChatClient 缓存池化**：基于 providerId+modelName 构建 ChatClient 缓存 key，避免每次请求新建 OpenAiApi/ChatModel/ChatClient，使用 Caffeine 本地缓存，TTL 30 分钟
- [ ] **F3.1.4 AiChatService 重构**：`generateDashboard*()`/`chatStream()` 等方法改为委托 `AiClient`，自身仅保留大屏场景的 prompt 构建逻辑，确保现有大屏生成功能不受影响
- [ ] **F3.1.5 通用 AI 调用接口**：新增 `POST /ai/client/call`（非流式）和 `POST /ai/client/stream`（SSE 流式）接口，参数为 `AiClientRequest(agentCode, message, providerId, modelName, temperature, maxTokens, contextVars)`
- [ ] **F3.1.6 AiPromptTemplateRenderer 泛化**：从仅支持大屏变量扩展为通用变量替换，支持任意 Agent 的 systemPrompt 中的 `{{变量名}}` 占位符

### 3.2 AI 辅助字段推荐（代码生成器集成）
- [ ] **F3.2.1 AI 字段推荐接口**：新增 `POST /generator/ai/recommendColumns` 接口，入参为 `GenTableColumn` 列表 + `GenTable` 表信息，返回 AI 推荐的列配置（javaType/htmlType/dictType/queryType/isRequired/validateRule）
- [ ] **F3.2.2 字段推荐 Agent**：新增 `codegen_column_advisor` Agent，systemPrompt 指导 AI 根据 DB 字段名/类型/注释/表语义推荐配置，输出 JSON 格式
- [ ] **F3.2.3 前端 AI 推荐按钮**：在 `ColumnConfigModal.vue` 中新增"AI 推荐"按钮，点击后调用推荐接口，将 AI 推荐结果回填到字段配置表格中（用户可逐字段修改）
- [ ] **F3.2.4 推荐结果 Diff 标记**：AI 推荐字段与原始配置不同的单元格，用浅色背景标记，方便用户识别 AI 修改了哪些配置

### 3.3 自然语言到 Schema 推断
- [ ] **F3.3.1 NL→Schema 接口**：新增 `POST /generator/ai/nlToSchema` 接口，入参为自然语言描述字符串，返回推断的表结构（tableName/tableComment + columns 列表）
- [ ] **F3.3.2 Schema 推断 Agent**：新增 `codegen_schema_builder` Agent，systemPrompt 指导 AI 从自然语言中推断数据模型，输出标准 JSON Schema
- [ ] **F3.3.3 前端自然语言输入**：在代码生成页面新增"AI 建表"入口，弹窗中输入自然语言描述，AI 返回 Schema 后展示预览，用户确认后导入为 GenTable + GenTableColumn
- [ ] **F3.3.4 追问优化**：Schema 推断结果下方提供"追问优化"按钮，点击后进入多轮对话模式（复用 `/ai/client/stream` 接口），用户可追加描述，AI 迭代优化 Schema

### 3.4 上下文感知配置
- [ ] **F3.4.1 code-style 文件配置**：在 `resources/ai-codegen/` 下放置 `code-style.md` 文件，包含项目编码规范（命名规则、组件映射偏好、验证规则偏好等），AI 运行时读取注入 systemPrompt
- [ ] **F3.4.2 SPEC 上下文 DB 管理**：新增 `ai_context_config` 表，存储 SPEC 上下文片段（如业务字段命名偏好、组件映射规则等），按 agentCode 关联，后台可增删改
- [ ] **F3.4.3 上下文注入机制**：AiClient 在构建 prompt 时，自动从文件读取 code-style + 从 DB 读取 SPEC 上下文，拼接注入 systemPrompt 末尾
- [ ] **F3.4.4 前端上下文管理页面**：在 AI 管理菜单下新增"AI 上下文配置"页面，管理 SPEC 上下文片段

### 3.5 显式降级
- [ ] **F3.5.1 AI 调用异常捕获**：AiClient 调用 AI 失败时（网络异常/API Key 失效/供应商未配置），不抛异常中断业务，而是返回降级标识 + 原始输入
- [ ] **F3.5.2 降级回退逻辑**：字段推荐失败时回退到 `GenUtils.initColumnField()` 规则推断；Schema 推断失败时提示用户"AI 不可用，请手动导入表"
- [ ] **F3.5.3 前端降级提示**：AI 推荐失败时，前端 Toast 提示"AI 推荐不可用（原因：xxx），已使用基础规则推断"，并提供"重试"按钮

## 4. 业务规则

### 4.1 AiClient 调用规则
- **BR4.1.1** Agent 必须存在且状态为 `0`（正常），否则抛出 `BusinessException("Agent 不存在或已停用: {agentCode}")`
- **BR4.1.2** Provider 解析优先级：请求指定 providerId > Agent 关联 providerId > 默认供应商，均无则抛出 `BusinessException("未配置可用的 AI 供应商")`
- **BR4.1.3** Provider 的 `status` 为 `1`（停用）时，拒绝调用并抛出 `BusinessException("AI 供应商已停用: {providerName}")`
- **BR4.1.4** Provider 的 `baseUrl` 或 `apiKey` 为空时，拒绝调用并抛出 `BusinessException("AI 供应商配置不完整")`
- **BR4.1.5** ChatClient 缓存 key 格式：`providerId:modelName`，缓存最大容量 50，TTL 30 分钟，Provider 配置变更时清除对应缓存
- **BR4.1.6** 流式调用时，对话记录在流完成后（`doFinally`）异步持久化，持久化失败不影响响应

### 4.2 AI 字段推荐规则
- **BR4.2.1** 推荐入参必须包含至少 1 个 GenTableColumn，否则返回空结果
- **BR4.2.2** AI 推荐结果只覆盖用户未手动修改过的字段（即如果用户已手动配置了 javaType，AI 不覆盖），推荐结果标记 `aiRecommended=true`
- **BR4.2.3** AI 推荐的 dictType 必须是 `sys_dict_type` 表中已存在的字典编码，不存在的字典编码不回填
- **BR4.2.4** AI 推荐的 htmlType 必须是 `ColumnConfigModal.vue` 中 htmlTypeOptions 枚举值之一（INPUT/TEXTAREA/SELECT/RADIO/CHECKBOX/DATETIME/IMAGE/FILE/EDITOR），否则回退为 INPUT
- **BR4.2.5** AI 推荐的 javaType 必须是 javaTypeOptions 枚举值之一，否则保留原始值
- **BR4.2.6** 基类字段（createTime/createBy/updateTime/updateBy/remark）的 isInsert/isEdit/isList/isQuery 始终为 0，AI 推荐结果不影响基类字段
- **BR4.2.7** AI 推荐时，将当前租户下 `sys_dict_type` 的字典编码列表注入 Agent 的 userMessage 中，供 AI 参考 dictType 推荐字典列表格式为：`可用字典：sys_normal_disable(正常/停用), sys_user_sex(性别), ...`
- **BR4.2.8** 字典列表注入长度限制：总字符数不超过 2000 字符，超长时按字典编码排序截断，末尾追加"...等共 N 个字典"提示

### 4.3 自然语言到 Schema 规则
- **BR4.3.1** 自然语言输入最大长度 2000 字符，超出截断并提示
- **BR4.3.2** AI 推断的 Schema 中，所有表自动包含基类字段（id/tenant_id/create_by/create_time/create_dept/update_by/update_time），不需要 AI 生成
- **BR4.3.3** AI 推断的 tableName 必须符合 MySQL 表名规范（小写字母+下划线，不以数字开头），否则前端提示修正
- **BR4.3.4** 用户确认 Schema 后，系统调用 `importGenTable` 的逻辑将 Schema 写入 gen_table + gen_table_column，与手动导入表的流程一致
- **BR4.3.5** 追问优化时，每次追问发送当前 Schema + 用户追加描述，AI 返回完整 Schema（非增量），前端整体替换展示

### 4.4 上下文配置规则
- **BR4.4.1** code-style.md 文件不存在时不阻塞，AiClient 跳过 code-style 注入，仅使用 Agent 的 systemPrompt
- **BR4.4.2** SPEC 上下文按 agentCode 关联，一个 Agent 可关联多条上下文片段，按 sort 排序拼接
- **BR4.4.3** 上下文注入格式：systemPrompt 末尾追加 `\n\n---\n【项目编码规范】\n{code-style内容}\n\n【SPEC上下文】\n{SPEC内容}`
- **BR4.4.4** 上下文总长度超过 8000 字符时，截断并记录 WARN 日志，避免 Token 溢出

### 4.5 降级规则
- **BR4.5.1** AI 调用超时时间：非流式 30s，流式首 Token 60s；超时触发降级
- **BR4.5.2** 降级时返回结构包含：`fallback=true`（降级标识）、`reason`（降级原因分类：PROVIDER_NOT_CONFIGURED / PROVIDER_DISABLED / API_ERROR / TIMEOUT）、`data`（降级后的数据）
- **BR4.5.3** 字段推荐降级：返回 `GenUtils.initColumnField()` 的结果，前端展示降级提示
- **BR4.5.4** Schema 推断降级：不返回降级数据，仅提示用户"AI 不可用，请通过导入表方式创建"
- **BR4.5.5** 连续 3 次 AI 调用失败后，5 分钟内不再尝试 AI 调用，直接走降级逻辑（熔断机制）

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|----------|------|
| 新增 | `ai_context_config` | 见下方 | AI 上下文配置表 |
| 修改 | `gen_table_column` | 新增 `validate_rule` VARCHAR(500) | AI 推荐的验证规则（JSON格式，如 `{"required":true,"pattern":"^1[3-9]\\d{9}$","message":"手机号格式不正确"}`） |
| 修改 | `gen_table_column` | 新增 `ai_recommended` TINYINT(1) DEFAULT 0 | 标记该字段配置是否由 AI 推荐（0否 1是），用于推荐结果 Diff 标记 |
| 新增索引 | `ai_context_config` | `idx_agent_code` (agent_code) | 按 Agent 编码查询上下文 |

### 新增表 `ai_context_config` DDL

```sql
CREATE TABLE `ai_context_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint DEFAULT 1 COMMENT '租户ID',
  `agent_code` varchar(64) NOT NULL COMMENT '关联Agent编码',
  `config_name` varchar(128) NOT NULL COMMENT '配置名称',
  `config_content` text NOT NULL COMMENT '上下文内容',
  `config_type` varchar(32) NOT NULL DEFAULT 'SPEC' COMMENT '类型：SPEC/SAMPLE/RULE',
  `sort` int DEFAULT 0 COMMENT '排序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_code` (`agent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI上下文配置表';
```

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 新增 | `POST /ai/client/call` | AiClientController.call | 通用 AI 非流式调用，入参 AiClientRequest，返回 RespInfo<AiClientResponse> |
| 新增 | `POST /ai/client/stream` | AiClientController.stream | 通用 AI SSE 流式调用，入参 AiClientRequest，返回 Flux<ServerSentEvent<String>> |
| 新增 | `POST /generator/ai/recommendColumns` | GenController.recommendColumns | AI 字段推荐，入参 RecommendColumnsRequest(tableId, columns)，返回 RespInfo<List<GenTableColumn>> |
| 新增 | `POST /generator/ai/nlToSchema` | GenController.nlToSchema | 自然语言到 Schema，入参 NlToSchemaRequest(description)，返回 RespInfo<NlToSchemaResult> |
| 新增 | `POST /generator/ai/nlToSchemaRefine` | GenController.nlToSchemaRefine | Schema 追问优化（多轮），入参 NlToSchemaRefineRequest(currentSchema, message, sessionId)，返回 RespInfo<NlToSchemaResult> |
| 新增 | `GET /ai/context/list` | AiContextConfigController.list | 查询上下文配置列表，参数 agentCode |
| 新增 | `POST /ai/context/add` | AiContextConfigController.add | 新增上下文配置 |
| 新增 | `PUT /ai/context/update` | AiContextConfigController.update | 修改上下文配置 |
| 新增 | `DELETE /ai/context/{id}` | AiContextConfigController.delete | 删除上下文配置 |
| 修改 | `POST /ai/generate` | AiChatController.generate | 内部委托 AiClient 调用，接口签名不变，确保大屏生成功能不受影响 |
| 修改 | `POST /ai/generate/stream` | AiChatController.generateStream | 内部委托 AiClient 调用，接口签名不变 |
| 修改 | `POST /ai/chat/stream` | AiChatController.chatStream | 内部委托 AiClient 调用，接口签名不变 |

### 新增 DTO 定义

**AiClientRequest**：
```java
public class AiClientRequest {
    private String agentCode;       // Agent编码，必填
    private String message;         // 用户消息，必填
    private Long providerId;        // 可选，指定供应商
    private String modelName;       // 可选，指定模型
    private Double temperature;     // 可选，温度
    private Integer maxTokens;      // 可选，最大Token
    private Map<String, String> contextVars; // 可选，模板变量（注入systemPrompt占位符）
    private String sessionId;       // 可选，会话ID（启用多轮记忆）
}
```

**AiClientResponse**：
```java
public class AiClientResponse {
    private String content;         // AI响应内容
    private boolean fallback;       // 是否降级
    private String fallbackReason;  // 降级原因
    private String sessionId;       // 会话ID
}
```

**RecommendColumnsRequest**：
```java
public class RecommendColumnsRequest {
    private Long tableId;                   // 表ID
    private List<GenTableColumn> columns;   // 当前列配置
}
```

**NlToSchemaRequest**：
```java
public class NlToSchemaRequest {
    private String description;     // 自然语言描述，必填，最大2000字符
}
```

**NlToSchemaRefineRequest**：
```java
public class NlToSchemaRefineRequest {
    private String currentSchema;   // 当前Schema JSON，必填
    private String message;         // 追问消息，必填
    private String sessionId;       // 会话ID，可选
}
```

**NlToSchemaResult**：
```java
public class NlToSchemaResult {
    private String tableName;           // 推断的表名
    private String tableComment;        // 推断的表描述
    private List<SchemaColumn> columns; // 推断的字段列表
    private String rawResponse;         // AI原始响应（调试用）
}

public class SchemaColumn {
    private String columnName;
    private String columnComment;
    private String columnType;
    private String javaType;
    private String javaField;
    private String htmlType;
    private String dictType;
    private Integer isRequired;
    private String validateRule;        // JSON格式验证规则
}
```

## 7. 影响范围

### 7.1 后端模块影响

| 模块 | 影响类型 | 说明 |
|------|---------|------|
| forge-plugin-ai | 重构 | AiChatService 重构为 AiClient 委托模式；新增 AiClient 接口/实现/缓存；新增 AiClientController；泛化 AiPromptTemplateRenderer；新增 AiContextConfig CRUD |
| forge-plugin-generator | 新增功能 | GenController 新增 3 个 AI 推荐接口；GenTableColumn 实体新增 2 个字段；新增 codegen_column_advisor/codegen_schema_builder Agent 初始化数据 |
| forge-plugin-ai → forge-plugin-generator | 新增依赖 | generator 需依赖 ai 模块的 AiClient 接口 |

### 7.2 前端影响

| 文件/组件 | 影响类型 | 说明 |
|-----------|---------|------|
| ColumnConfigModal.vue | 修改 | 新增"AI 推荐"按钮、AI 推荐结果回填、Diff 标记逻辑 |
| table.vue | 修改 | 新增"AI 建表"操作入口 |
| 新增 AiSchemaModal.vue | 新增 | 自然语言输入 → Schema 预览 → 追问优化 → 确认导入 |
| 新增 AiContextConfig.vue | 新增 | AI 上下文配置管理页面 |
| api/ai.js | 修改 | 新增 AiClient 通用调用接口、上下文配置接口 |
| api/generator.js（或内联） | 修改 | 新增 AI 字段推荐、NL→Schema 接口 |

### 7.3 数据库影响

| 变更 | 说明 |
|------|------|
| 新增表 `ai_context_config` | AI 上下文配置 |
| `gen_table_column` 新增 2 字段 | `validate_rule`/`ai_recommended` |
| 新增 Agent 初始数据 | `codegen_column_advisor`/`codegen_schema_builder` 两条 Agent 记录插入 `ai_agent` 表 |

### 7.4 不受影响的部分

- Velocity 模板文件（`.vm`）：模板本身不变，AI 增强发生在模板填充前的字段配置阶段
- GenTemplate 管理功能：模板 CRUD 不变
- 大屏生成功能：接口签名不变，仅内部实现委托 AiClient
- AI 供应商/模型管理页面：不变

## 8. 风险与关注点

### 8.1 技术风险

| 风险 | 等级 | 说明 | 缓解措施 |
|------|------|------|----------|
| AiChatService 重构引入回归 | 🔴高 | 大屏生成是已有功能，重构可能导致流式输出/对话记录异常 | 重构后必须回归测试大屏生成的流式/非流式/多轮对话三个场景，对比重构前后输出一致性 |
| AI 幻觉导致推荐字段不合理 | 🟡中 | AI 可能推荐不存在的字典编码、不匹配的组件类型 | BR4.2.3/4.2.4/4.2.5 校验层兜底，AI 推荐结果必须通过枚举值校验后才回填 |
| ChatClient 缓存与 Provider 配置不同步 | 🟡中 | 用户修改 Provider 的 apiKey/baseUrl 后，缓存中的 ChatClient 仍用旧配置 | Provider 更新时清除对应缓存 key；ChatClient 构建时校验配置版本 |
| AI 调用延迟影响生成器体验 | 🟡中 | 字段推荐/Schema 推断调用 AI 可能耗时 5-30s | 非流式接口设 30s 超时；前端 Loading 态 + 取消按钮；降级回退 |
| 上下文注入导致 Token 溢出 | 🟡中 | code-style + SPEC 上下文 + systemPrompt + 用户消息可能超出模型 Token 上限 | BR4.4.4 限制上下文总长度 8000 字符；AI 调用前估算 Token 数，超限截断上下文 |
| generator 依赖 ai 模块的循环依赖 | 🟢低 | 两个 plugin 之间的依赖需要 forge-admin 做桥接 | AiClient 定义为接口，generator 仅依赖接口，实现类通过 Spring 注入；或 generator 直接注入 ai 模块的 Bean（admin 已同时依赖两者） |

### 8.2 业务风险

| 风险 | 等级 | 说明 | 缓解措施 |
|------|------|------|----------|
| 用户过度信任 AI 推荐 | 🟡中 | 用户可能不审核直接采用 AI 推荐结果，导致生成代码质量不佳 | Diff 标记让用户明确看到 AI 改了什么；AI 推荐结果默认不自动保存，需用户确认 |
| AI 不可用时用户体验下降 | 🟡中 | 配置了 AI 但经常降级，用户感知"功能不好用" | BR4.5.5 熔断机制避免频繁重试；降级提示中说明具体原因（如"API Key 未配置"），引导用户排查 |
| NL→Schema 推断结果与预期不符 | 🟡中 | 自然语言描述模糊时，AI 推断的表结构与用户期望差异大 | Schema 预览后再确认导入；追问优化支持迭代修正；不自动创建数据库表，仅生成配置 |
| 敏感信息泄露到 AI | 🟢低 | 用户输入的描述可能包含业务敏感信息 | 前端提示"请勿输入敏感信息"；AI 对话记录按租户隔离存储 |

## 8.5 测试策略

- **测试范围**：AiClient 重构、AI 字段推荐、NL→Schema、上下文注入、降级机制
- **覆盖率目标**：AiClient 核心逻辑 80%+，降级分支 100%
- **独立 Test Spec**：是（详见 `test-spec.md`）

### 测试方法

| 测试类型 | 范围 | 方法 |
|---------|------|------|
| 单元测试 | AiClient 接口/实现、ChatClient 缓存、Provider 解析优先级、枚举值校验 | Mock OpenAiApi，验证调用参数和返回值；验证缓存命中/失效；验证降级逻辑各分支 |
| 单元测试 | AiPromptTemplateRenderer 泛化 | 输入含 `{{变量名}}` 的模板 + 变量 Map，验证替换结果 |
| 单元测试 | 字段推荐结果校验 | 构造 AI 返回非法 dictType/htmlType/javaType 的场景，验证校验层过滤 |
| 单元测试 | 上下文长度限制 | 构造超长上下文，验证截断逻辑和 WARN 日志 |
| 单元测试 | 熔断机制 | 连续 3 次调用失败后验证直接降级，5 分钟后验证恢复 |
| 集成测试 | 大屏生成回归 | 重构后调用 `/ai/generate/stream`，验证 SSE 流式输出、对话记录持久化与重构前一致 |
| 集成测试 | AI 字段推荐 E2E | 导入一张真实表 → 调用推荐接口 → 验证推荐结果格式正确 → 保存 → 验证数据库记录 |
| 集成测试 | NL→Schema E2E | 输入自然语言 → 验证返回 Schema 结构完整 → 追问优化 → 确认导入 → 验证 gen_table/gen_table_column 记录 |
| 集成测试 | 降级 E2E | 不配置 Provider → 调用推荐接口 → 验证返回 fallback=true + 降级数据 → 前端提示降级 |
| 前端测试 | ColumnConfigModal AI 推荐 | 点击"AI 推荐"→ Loading → 结果回填 → Diff 标记可见 → 手动修改 → 保存 |
| 前端测试 | AiSchemaModal | 输入描述 → 预览 Schema → 追问优化 → 确认导入 → 列表刷新 |

### 回归测试检查清单

- [ ] 大屏生成（非流式）：`POST /ai/generate` 返回正确内容
- [ ] 大屏生成（流式）：`POST /ai/generate/stream` SSE 输出正常
- [ ] 多轮对话：`POST /ai/chat/stream` 带 sessionId 上下文延续正常
- [ ] 代码生成：导入表 → 配置 → 预览 → 下载，全流程不变
- [ ] AI 供应商管理：CRUD + 测试连接正常
- [ ] AI 模型管理：CRUD 正常

## 9. 待澄清

- [x] Q1：`forge-plugin-generator` 如何依赖 `forge-plugin-ai` 的 AiClient？→ **Admin 桥接**：admin 模块同时引入两个 plugin，generator 通过 Spring 依赖注入获取 AiClient Bean，不直接 Maven 依赖 ai 模块
- [x] Q2：AI 推荐字段的 Agent systemPrompt 中，是否需要注入当前项目已有的字典列表？→ **注入字典列表**：将当前租户下 `sys_dict_type` 字典编码列表注入 AI 请求，提升 dictType 推荐准确率（BR4.2.7/4.2.8）
- [x] Q3：NL→Schema 推断的表，用户确认导入后是否需要自动在数据库中创建物理表？→ **不自动建表**：仅生成 gen_table + gen_table_column 配置记录，不创建物理表

## 10. 技术决策

| 决策 | 选择 | 理由 |
|------|------|------|
| AiClient 抽象范围 | B) 重构统一 | 用户确认，统一 Agent 驱动模式 |
| AI 集成方式 | A) AI 辅助增强 | 用户确认，AI 建议人来确认 |
| Schema 推断方式 | C) 单轮+可选追问 | 用户确认，默认单轮，不满意可追问 |
| 上下文配置方式 | C) 混合方式 | 用户确认，code-style 文件 + SPEC DB |
| 降级策略 | B) 显式降级 | 用户确认，提示用户并回退 |
| ChatClient 缓存方案 | Caffeine 本地缓存 | 简单高效，避免引入 Redis 额外依赖 |
| AiClient 接口位置 | forge-plugin-ai 模块 | AI 能力的自然归属，其他模块通过依赖注入使用 |
| 模块间依赖方式 | Admin 桥接 | Q1确认：admin 同时引入 ai + generator，generator 通过 Spring 注入 AiClient Bean，不直接 Maven 依赖 |
| 字段推荐注入字典列表 | 注入 | Q2确认：将 sys_dict_type 字典编码列表注入 AI 请求，提升 dictType 推荐准确率（BR4.2.7/4.2.8） |
| NL→Schema 是否自动建表 | 不自动建表 | Q3确认：仅生成 gen_table 配置，不创建物理表 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|-------------|------|
| Task 1 | ✅完成 | ai_context_config.sql, gen_table_column_ai_fields.sql, GenTableColumn.java, AiContextConfig.java, AiContextConfigMapper.java | 编译通过 |
| Task 2 | ✅完成 | AiClient.java, AiClientRequest.java, AiClientResponse.java, AiFallbackReason.java | 编译通过 |
| Task 3 | ✅完成 | AiClientImpl.java, ChatClientCache.java, CircuitBreaker.java, ContextInjector.java, pom.xml(Caffeine) | 编译通过 |
| Task 4 | ✅完成 | AiPromptTemplateRenderer.java | 编译通过 |
| Task 5 | ✅完成 | AiChatService.java | 编译通过，委托AiClient |
| Task 6 | ✅完成 | AiClientController.java, AiContextConfigController.java, AiContextConfigService.java | 编译通过 |
| Task 7 | ✅完成 | code-style.md, ai_codegen_agents.sql | 编译通过 |
| Task 8 | ✅完成 | AiClientAdapter.java, AiRecommendService.java, RecommendColumnsRequest.java, GenController.java, AiClientAdapterImpl.java(admin桥接) | 编译通过 |
| Task 9 | ✅完成 | AiSchemaService.java, NlToSchemaRequest.java, NlToSchemaRefineRequest.java, NlToSchemaResult.java, SchemaColumn.java, GenController.java | 编译通过 |
| Task 10 | ✅完成 | ColumnConfigModal.vue, ai.js | AI推荐按钮+Diff标记 |
| Task 11 | ✅完成 | AiSchemaModal.vue, table.vue | NL→Schema弹窗 |
| Task 12 | ✅完成 | context-config.vue, ai.js | 上下文配置管理页面 |

## 12. 审查结论

**审查时间**：2026-04-20
**审查结果**：❌ FAIL

### Spec Compliance（阶段一）

| 级别 | 数量 | 详情 |
|------|------|------|
| Critical | 2 | importSchema 接口缺失；AI 推荐覆盖用户手动修改 |
| Important | 3 | dictType 未校验(BR4.2.3)；字典列表未截断(BR4.2.8)；无超时设置(BR4.5.1) |
| Minor | 3 | tableName 未校验(BR4.3.3)；缓存未清除(BR4.1.5)；Diff 标记行级而非单元格级(F3.2.4) |

### Code Quality（阶段二）

- ChatClientCache sessionId 串扰风险
- 降级原因判断依赖字符串匹配（脆弱）
- AiClientAdapter 缺少 sessionId 透传
- queryDictTypeList() 代码重复

### 修复后需重新审查

## 13. 确认记录（HARD-GATE）

- **确认时间**：
- **确认人**：
