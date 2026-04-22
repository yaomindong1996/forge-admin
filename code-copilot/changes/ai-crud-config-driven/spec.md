# 配置驱动 CRUD 页面——用 JSON 配置 + AiCrudPage 运行时渲染替代代码生成
> status: confirmed
> created: 2026-04-20
> complexity: 🔴复杂

## 1. 背景与目标

### 为什么做

当前代码生成器（`forge-plugin-generator`）采用 Velocity 模板填充模式：用户导入表 → 配置字段 → 生成前后端代码文件。对于简单 CRUD 页面，这套流程存在以下问题：

1. **产出物冗余**：每张表生成 Controller/Service/Mapper/Entity/DTO/VO + Vue 页面共 10+ 个文件，但 80% 的 CRUD 页面结构雷同，差异仅在字段配置
2. **变更成本高**：生成后的代码文件需要纳入版本管理，表结构变更时需重新生成或手动同步
3. **AI 价值未最大化**：现有 AI 能力（`codegen_column_advisor`/`codegen_schema_builder`）仅用于"辅助配置字段"，最终产出仍是代码文件；AI 的真正价值应是"替代人工配置元数据"，而非"替代模板填充生成代码"
4. **运行时灵活性缺失**：生成的代码需要编译部署才能生效，无法热更新

前一轮变更（`ai-codegen-enhance`）已建立 AI 底层基础设施（AiClient + 缓存 + 熔断 + 上下文注入 + Agent 体系），本轮在上层重新设计：**用 JSON 配置存 DB + AiCrudPage 运行时渲染，替代代码文件生成**。

### 做完后的效果（可验证）

1. **配置驱动 CRUD**：用户通过 AI 或手动创建 JSON 配置，存入 `ai_crud_config` 表，前端 AiCrudPage 直接从后端拉取配置渲染页面，**零代码文件生成**
2. **AI 一键生成配置**：用户输入自然语言或选择已有表 → AI 输出完整 CRUD 页面配置 JSON（searchSchema + columns + editSchema + apiConfig）→ 预览确认 → 存入 DB → 立即可用
3. **运行时 CRUD Controller**：后端通用 `DynamicCrudController` 根据 `configKey` 读取配置，动态执行增删改查，**无需为每张表生成后端代码**
4. **菜单自动注册**：配置创建后自动在 `sys_resource` 插入菜单记录，刷新即可在侧边栏看到入口
5. **双轨并行**：`ai_crud_config.mode` 字段区分 `CONFIG`（配置驱动）和 `CODEGEN`（代码生成），复杂业务仍可使用代码生成器，两种模式共享 AI 推荐能力

## 2. 代码现状（Research Findings）

### 2.1 已有基础设施（可直接复用）

#### 2.1.1 AI 客户端体系

- **AiClient 接口**：统一 AI 调用抽象，支持 `call()` 非流式和 `stream()` 流式
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiClient.java`
- **AiClientImpl**：核心实现，含 Agent 解析、Provider 解析、ChatClient 缓存、对话持久化
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiClientImpl.java`
  - `call()` 方法（第 45-89 行）：非流式调用，含熔断判断 → Agent/Provider/Model 解析 → ChatClient 缓存获取 → 调用 → 持久化
  - `stream()` 方法（第 92-144 行）：流式调用，SSE 输出
  - `buildSystemPrompt()` 方法（第 217-223 行）：Agent systemPrompt + 上下文变量渲染 + ContextInjector 注入
- **ChatClientCache**：基于 Caffeine 的 ChatClient 缓存池化，key = providerId:modelName，TTL 30 分钟
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/ChatClientCache.java`
- **CircuitBreaker**：熔断器，连续 3 次失败后 5 分钟内直接降级
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/CircuitBreaker.java`
- **ContextInjector**：上下文注入器，从文件读取 code-style + 从 DB 读取 SPEC 上下文
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/ContextInjector.java`
- **AiClientAdapter（接口）**：generator 模块定义的桥接接口，避免直接 Maven 依赖 ai 模块
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiClientAdapter.java`
  - `call()` 方法（第 7 行）：`AiClientResult call(String agentCode, String message, Map<String, String> contextVars)`
- **AiClientAdapterImpl（桥接实现）**：在 forge-admin 中实现，注入 AiClient Bean
  - 文件：`forge/forge-admin-server/src/main/java/com/mdframe/forge/admin/bridge/AiClientAdapterImpl.java`

#### 2.1.2 AI Agent 数据

- **AiAgent**：Agent 实体，含 agentCode/systemPrompt/providerId/modelName/temperature/maxTokens
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/domain/AiAgent.java`
- **已有 Agent**：`codegen_column_advisor`（字段推荐）和 `codegen_schema_builder`（Schema 推断），初始化数据已插入 `ai_agent` 表
  - 出处：`code-copilot/changes/ai-codegen-enhance/spec.md` Task 7 执行日志

#### 2.1.3 AI 上下文配置

- **AiContextConfig**：上下文配置实体，按 agentCode 关联
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/context/domain/AiContextConfig.java`
- **AiContextConfigService**：上下文配置 CRUD 服务
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/context/service/AiContextConfigService.java`
- **AiContextConfigController**：上下文配置 REST 接口（`/ai/context/list`、`/ai/context/add`、`/ai/context/update`、`/ai/context/{id}`）
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/context/controller/AiContextConfigController.java`

#### 2.1.4 AI 通用调用接口

- **AiClientController**：通用 AI 调用入口
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/controller/AiClientController.java`
  - `POST /ai/client/call`（第 22-26 行）：非流式调用
  - `POST /ai/client/stream`（第 28-43 行）：SSE 流式调用

#### 2.1.5 现有 AI 推荐服务（可直接复用核心逻辑）

- **AiSchemaService**：自然语言→Schema 推断服务
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiSchemaService.java`
  - `nlToSchema()` 方法（第 46-68 行）：调用 `codegen_schema_builder` Agent 推断表结构
  - `nlToSchemaRefine()` 方法（第 70-90 行）：多轮追问优化
  - `importSchema()` 方法（第 164-208 行）：将 Schema 导入 gen_table + gen_table_column
  - `queryDictTypeList()` 方法（第 145-162 行）：查询字典列表注入 AI 上下文
- **AiRecommendService**：AI 字段推荐服务
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiRecommendService.java`
  - `recommendColumns()` 方法（第 40-66 行）：调用 `codegen_column_advisor` Agent 推荐字段配置
  - `validateAndMerge()` 方法（第 124-186 行）：校验 AI 推荐结果并合并
  - `fallbackToRuleBased()` 方法（第 188-194 行）：降级回退到规则推断

#### 2.1.6 前端 AI API

- **ai.js**：前端 AI 接口定义
  - 文件：`forge-admin-ui/src/api/ai.js`
  - `aiClientCall()` / `aiClientStream()`（第 125-131 行）：通用 AI 调用
  - `contextConfigList()` / `contextConfigAdd()` / `contextConfigUpdate()` / `contextConfigDelete()`（第 135-149 行）：上下文配置接口

### 2.2 前端 AiCrudPage 组件（核心复用）

- **AiCrudPage.vue**：完整 CRUD 页面组件，支持 JSON 配置驱动渲染
  - 文件：`forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
  - Props 入口（第 279 行）：`defineProps(aiCrudPageProps)`
  - `loadList()` 方法（第 674-778 行）：列表加载，支持 `apiConfig` 配置
  - `parseApiConfig()` 方法（第 641-669 行）：解析 `method@url` 格式的 API 配置，支持 URL 占位符 `:id`
  - `handleModalConfirm()` 方法（第 1085-1158 行）：表单提交
  - `handleDelete()` / `performDelete()` 方法（第 976-1080 行）：删除逻辑
  - 8 个钩子函数：`beforeLoadList`/`beforeRenderList`/`beforeSubmit`/`beforeDelete`/`beforeSearch`/`beforeRenderReset`/`beforeRenderDetail`/`beforeRenderForm`
- **AiCrudPageProps.js**：Props 完整定义
  - 文件：`forge-admin-ui/src/components/ai-form/AiCrudPageProps.js`
  - 核心配置项：`searchSchema`(Array)、`columns`(Array)、`editSchema`(Array)、`apiConfig`(Object)、`api`(String)、`rowKey`(String)
  - `apiConfig` 格式（第 308-321 行）：`{ list: 'get@/api/users', create: 'post@/api/users', update: 'put@/api/users/:id', delete: 'delete@/api/users/:id', detail: 'get@/api/users/:id' }`
- **AiFormItem.vue**：表单项组件，支持 25 种控件类型
  - 文件：`forge-admin-ui/src/components/ai-form/AiFormItem.vue`
  - 支持的 type：input/textarea/number(inputNumber)/select/radio/radioButton/checkbox/switch/date/datetime/daterange/month/year/time/upload/fileUpload/imageUpload/slider/rate/color/cascader/treeSelect/transfer/customSelect/text/slot/divider
  - 出处：第 26-467 行的 `v-if/v-else-if` 链
- **schemaHelper.js**：Schema 辅助工具，支持运行时更新字段选项/配置
  - 文件：`forge-admin-ui/src/components/ai-form/schemaHelper.js`
  - `updateFieldOptions()` / `updateFieldConfig()` / `createAsyncOptionsUpdater()` / `createCascadeUpdater()`

### 2.3 代码生成器现有体系（需重新设计上层）

- **GenController**：代码生成器 REST 接口
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/GenController.java`
  - AI 相关接口（第 155-193 行）：`/generator/ai/recommendColumns`、`/generator/ai/nlToSchema`、`/generator/ai/nlToSchemaRefine`、`/generator/ai/importSchema`
- **GenTable**：表配置实体
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/GenTable.java`
- **GenTableColumn**：字段配置实体，含 `javaType`/`htmlType`/`dictType`/`queryType`/`isRequired`/`validateRule`/`aiRecommended`
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/GenTableColumn.java`
- **SchemaColumn DTO**：AI Schema 输出结构，含 columnName/columnComment/columnType/javaType/javaField/htmlType/dictType/isRequired/validateRule
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/SchemaColumn.java`
- **NlToSchemaResult DTO**：NL→Schema 输出结构，含 tableName/tableComment/columns/rawResponse
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/NlToSchemaResult.java`

### 2.4 菜单/路由系统

- **SysResource**：系统资源实体（菜单/按钮/API），含 resourceName/parentId/resourceType(1目录/2菜单/3按钮/4API)/path/component/perms/icon/sort 等
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysResource.java`
- **ISysResourceService**：资源服务接口，含 `insertResource(SysResourceDTO dto)` / `selectResourceTree()` 等
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/ISysResourceService.java`
- **SysResourceServiceImpl**：资源服务实现，`insertResource()` 方法将 DTO 转换为实体并插入
  - 文件：`forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysResourceServiceImpl.java:116-123`
- **前端路由机制**：使用 `unplugin-vue-router` 自动扫描 `src/views/` 生成路由；菜单数据从后端 `sys_resource` 获取，`permission store` 处理渲染
  - 文件：`forge-admin-ui/src/router/index.js`（第 82-83 行 autoRoutes 导入）
  - 文件：`forge-admin-ui/src/store/modules/permission.js`
  - `processMenuData()` 方法（第 139-237 行）：将后端 `UserResourceTreeVO` 转换为前端菜单格式
  - `generateAccessRoutesFromMenus()` 方法（第 84-136 行）：从菜单数据生成 accessRoutes
  - 关键映射（第 189-201 行）：`component` 字段统一处理为 `/src/views/{comp}.vue` 格式

### 2.5 发现与风险

1. **AiCrudPage 已是 JSON 配置驱动**：组件本身已完全支持通过 searchSchema/columns/editSchema/apiConfig 配置驱动渲染，新增"配置从 DB 加载"而非"配置写在 Vue 文件中"是自然延伸，无架构冲突
   - 出处：`AiCrudPage.vue` 全文 + `AiCrudPageProps.js` 全文
2. **apiConfig 协议已成熟**：`method@url` 格式 + URL 占位符替换机制已实现，配置驱动模式下只需将 api 配置指向通用 DynamicCrudController 的路径
   - 出处：`AiCrudPage.vue:641-669` `parseApiConfig()` 方法
3. **前端路由基于文件扫描**：`unplugin-vue-router` 不支持运行时动态路由，因此配置驱动的 CRUD 页面必须使用通用渲染页 + query 参数方案（已确认），只需新建一个 `crud-page.vue` 文件
   - 出处：`forge-admin-ui/src/router/index.js:82-83` + `permission.js:189-201`
4. **现有 GenTableColumn → 前端 Schema 无转换器**：当前从 GenTableColumn 到前端 searchSchema/columns/editSchema 的转换逻辑分散在 Velocity 模板中（`.vm` 文件），没有 Java 层面的转换器。配置驱动模式需要在后端新增 `SchemaGenerator` 将 GenTableColumn 转换为前端三套 Schema JSON
   - 出处：`VelocityUtils.java:32-69` `prepareContext()` 将 columns 注入模板上下文，转换逻辑在模板内
5. **MyBatis-Plus 动态表名支持**：MyBatis-Plus 3.5.x 支持 `dynamicTableName`，但需要确认当前版本是否可用以及与 `TenantLineInnerInterceptor` 的兼容性
   - 出处：项目 MyBatis-Plus 版本 3.5.7（`project-context.md:21`）
6. **SysResource 租户拦截排除**：`sys_resource` 表不在 `TenantLineInnerInterceptor` 拦截范围内，新增菜单记录时 `tenant_id` 保持 `1` 即可
   - 出处：`code-copilot/rules/coding-style.md:37`
7. **AiClientAdapter 桥接模式已验证**：generator 模块通过接口 + admin 桥接实现的方式使用 AiClient，无需直接 Maven 依赖 ai 模块，新模式可复用此模式
   - 出处：`AiClientAdapter.java` + `AiClientAdapterImpl.java`

## 3. 功能点

### 3.1 配置存储与管理

- [ ] **F3.1.1 ai_crud_config 表与实体**：新建 `ai_crud_config` 表，含 config_key/table_name/table_comment/search_schema(columns)/edit_schema(columns)/api_config(options)/mode/status 等字段，实体类 `AiCrudConfig` 继承 `TenantEntity`
- [ ] **F3.1.2 AiCrudConfigService**：配置 CRUD 服务，含 `getByConfigKey()`/`createConfig()`/`updateConfig()`/`deleteConfig()`/`listPage()` 方法
- [ ] **F3.1.3 AiCrudConfigController**：配置管理 REST 接口，提供 CRUD + 配置加载 API
- [ ] **F3.1.4 前端配置管理页面**：在 AI 管理菜单下新增"CRUD 配置管理"页面，使用 AiCrudPage 展示配置列表，支持新增/编辑/删除/预览

### 3.2 Schema JSON 自动生成器（GenTableColumn → 前端三套 Schema）

- [ ] **F3.2.1 SchemaGenerator 服务**：根据 `GenTableColumn` 列表 + 表元数据，自动生成前端三套 Schema JSON（searchSchema + columns + editSchema），输出可直接被 AiCrudPage 消费的 JSON 结构
  - searchSchema 生成规则：`isQuery=1` 的字段 → 按 queryType 映射搜索控件（EQ→select, LIKE→input, BETWEEN→daterange 等）
  - columns 生成规则：`isList=1` 的字段 → 按 htmlType 映射列渲染（dictType 字段用 DictTag，datetime 字段格式化，image 字段用 AuthImage 等）
  - editSchema 生成规则：`isInsert=1` 或 `isEdit=1` 的字段 → 按 htmlType 映射表单控件，包含验证规则
- [ ] **F3.2.2 apiConfig 自动生成**：根据 `tableName` 和 `configKey` 生成指向 DynamicCrudController 的 apiConfig JSON，格式为 `{ list: 'get@/ai/crud/{configKey}/page', detail: 'get@/ai/crud/{configKey}/{id}', add: 'post@/ai/crud/{configKey}', update: 'put@/ai/crud/{configKey}', delete: 'delete@/ai/crud/{configKey}/{id}' }`

### 3.3 AI 增强配置生成

- [ ] **F3.3.1 AI 全量配置生成接口**：新增 `POST /ai/crud-config/ai/generate` 接口，入参为自然语言描述或已有 tableName，AI 输出完整 CRUD 页面配置 JSON（searchSchema + columns + editSchema + apiConfig + tableName + tableComment），前端预览确认后存入 `ai_crud_config`
- [ ] **F3.3.2 新增 crud_config_builder Agent**：systemPrompt 指导 AI 根据表结构/自然语言生成完整 CRUD 页面配置，输出标准 JSON（包含前端三套 Schema + apiConfig + 推荐的表名/表注释/菜单名称/菜单路径等）
- [ ] **F3.3.3 已有表导入→AI 增强生成流程**：用户选择已有数据库表 → 系统读取表元数据 → 调用 AI 生成完整配置 → 预览确认 → 存入 `ai_crud_config`
- [ ] **F3.3.4 自然语言→AI 增强生成流程**：用户输入自然语言描述 → AI 推断表结构 + 生成完整配置 → 预览确认 → 存入 `ai_crud_config`（可选：同时创建物理表）
- [ ] **F3.3.5 AI 降级回退**：AI 不可用时，已导入表走纯规则 SchemaGenerator 生成基础配置；自然语言模式降级提示"AI 不可用，请通过导入表方式创建"

### 3.4 运行时 CRUD Controller

- [ ] **F3.4.1 DynamicCrudController**：通用 CRUD Controller，路径 `/ai/crud/{configKey}`，根据 configKey 从 `ai_crud_config` 读取 tableName + apiConfig，使用 MyBatis-Plus 动态表名执行 CRUD
  - `GET /ai/crud/{configKey}/page`：分页查询
  - `GET /ai/crud/{configKey}/{id}`：详情查询
  - `POST /ai/crud/{configKey}`：新增
  - `PUT /ai/crud/{configKey}`：更新
  - `DELETE /ai/crud/{configKey}/{id}`：删除
- [ ] **F3.4.2 DynamicCrudService**：通用 CRUD 服务，使用 MyBatis-Plus `dynamicTableName` + `SqlHelper` 执行 SQL，支持：
  - 分页查询（含搜索条件构建：EQ/LIKE/BETWEEN/IN 等）
  - 按 ID 查询详情
  - 新增（自动填充 tenant_id/create_by/create_time 等）
  - 更新（自动填充 update_by/update_time）
  - 删除（逻辑删除或物理删除，根据表是否有 del_flag 字段判断）
- [ ] **F3.4.3 字段白名单校验**：DynamicCrudController 接收前端提交的数据时，仅允许 `ai_crud_config.edit_schema` 中声明的字段通过，防止 SQL 注入和越权写入

### 3.5 AiCrudPage 配置加载

- [ ] **F3.5.1 配置加载接口**：新增 `GET /ai/crud-config/render/{configKey}` 接口，返回完整的 AiCrudPage 渲染配置（searchSchema + columns + editSchema + apiConfig + 其他 props），前端通用渲染页调用此接口获取配置
- [ ] **F3.5.2 通用渲染页 crud-page.vue**：新建 `/src/views/ai/crud-page.vue`，从 URL query 参数获取 `configKey`，调用配置加载接口获取 JSON 配置，传递给 AiCrudPage 渲染
- [ ] **F3.5.3 配置缓存**：前端对已加载的配置做内存缓存（Map<configKey, config>），避免重复请求；配置更新时通过版本号或时间戳比对刷新

### 3.6 菜单自动注册

- [ ] **F3.6.1 配置创建时自动注册菜单**：`AiCrudConfigService.createConfig()` 方法中，配置创建成功后自动往 `sys_resource` 插入一条菜单记录：
  - `resourceType=2`（菜单）
  - `path=/ai/crud-page?configKey={configKey}`
  - `component=ai/crud-page`
  - `resourceName=配置中的 menuName 字段`
  - `parentId=配置中的 menuParentId 字段`（默认挂载到"AI 管理"目录下）
  - `tenant_id=1`
  - `sort=配置中的 menuSort 字段`
- [ ] **F3.6.2 配置删除时联动删除菜单**：`AiCrudConfigService.deleteConfig()` 方法中，根据 configKey 查找并删除对应的 `sys_resource` 记录
- [ ] **F3.6.3 配置更新时联动更新菜单**：`AiCrudConfigService.updateConfig()` 方法中，如 menuName/menuSort/menuParentId 发生变化，同步更新 `sys_resource` 对应记录

### 3.7 双轨并行（配置驱动 + 代码生成）

- [ ] **F3.7.1 mode 字段区分**：`ai_crud_config.mode` 取值 `CONFIG`（配置驱动，走 DynamicCrudController）或 `CODEGEN`（代码生成，走现有 Velocity 流程），默认 `CONFIG`
- [ ] **F3.7.2 管理界面切换**：配置管理页面支持在两种模式间切换；切换到 CODEGEN 时走现有代码生成流程，生成独立前后端文件
- [ ] **F3.7.3 共享 AI 推荐能力**：无论哪种模式，AI 推荐字段配置的能力共享（复用 AiRecommendService/AiSchemaService）

## 4. 业务规则

### 4.1 配置管理规则

- **BR4.1.1** `configKey` 全局唯一，创建后不可修改，格式限定为小写字母+数字+下划线，长度 2-64 字符，不符合格式拒绝创建
- **BR4.1.2** `tableName` 必须是数据库中已存在的物理表（配置驱动模式需要运行时查询），不存在的表拒绝保存配置（验证方式：`JdbcTemplate` 查询 `information_schema.TABLES`）
- **BR4.1.3** 同一 `tableName` 只能关联一个 `mode=CONFIG` 的配置记录，重复创建时返回错误提示"该表已有配置驱动的 CRUD 配置"
- **BR4.1.4** `search_schema`/`edit_schema`/`columns_schema`/`api_config` 字段为 JSON 格式，存储前做 JSON 语法校验，非法 JSON 拒绝保存
- **BR4.1.5** 配置状态 `status`：`0`=启用、`1`=停用；停用的配置前端无法渲染，DynamicCrudController 返回 404

### 4.2 Schema 生成规则

- **BR4.2.1** searchSchema 生成：仅包含 `isQuery=1` 的字段；queryType 映射规则：`EQ`→`select`、`LIKE`→`input`、`BETWEEN`→`daterange`、`GT/LT/GTE/LTE`→`number`、`IN`→`select`(multiple)
- **BR4.2.2** columns 生成：仅包含 `isList=1` 的字段；htmlType 映射规则：`SELECT/RADIO/CHECKBOX` 且有 dictType → 渲染为 `DictTag`；`DATETIME` → 格式化为 `YYYY-MM-DD HH:mm:ss`；`IMAGE` → 渲染为 `AuthImage`；其他 → 文本展示
- **BR4.2.3** editSchema 生成：`isInsert=1` 或 `isEdit=1` 的字段；基类字段（id/tenant_id/create_by/create_time/create_dept/update_by/update_time）不生成到 editSchema 中
- **BR4.2.4** editSchema 验证规则生成：`isRequired=1` → `required: true`；`validateRule` JSON 透传；`String` 类型且 `htmlType=INPUT` 默认 `maxlength: 255`
- **BR4.2.5** apiConfig 自动生成格式：`{ list: 'get@/ai/crud/{configKey}/page', detail: 'get@/ai/crud/{configKey}/{id}', add: 'post@/ai/crud/{configKey}', update: 'put@/ai/crud/{configKey}', delete: 'delete@/ai/crud/{configKey}/{id}' }`

### 4.3 AI 配置生成规则

- **BR4.3.1** AI 生成配置时，新增 `crud_config_builder` Agent，与现有 `codegen_schema_builder` 共享字典列表注入逻辑（`queryDictTypeList()`）
- **BR4.3.2** AI 输出的 JSON 必须经过 SchemaGenerator 校验层：htmlType 必须是 AiFormItem 支持的 25 种 type 之一；dictType 必须是 `sys_dict_type` 中已存在的编码；不符合的回退为默认值
- **BR4.3.3** AI 降级时：已有表导入 → 纯规则 SchemaGenerator 生成基础配置（不调 AI）；自然语言输入 → 提示"AI 不可用，请通过导入表方式创建"
- **BR4.3.4** AI 生成配置预览阶段不写入 `ai_crud_config`，仅返回给前端展示；用户确认后才入库
- **BR4.3.5** AI 生成配置时可推断 `menuName`（菜单名称）和 `menuSort`（排序），AI 未推断时 `menuName` 默认取 `tableComment`，`menuSort` 默认取当前最大 sort+1

### 4.4 运行时 CRUD 规则

- **BR4.4.1** DynamicCrudController 根据 `configKey` 读取配置中的 `tableName`，使用 MyBatis-Plus `dynamicTableName` 动态设置表名，执行 SQL
- **BR4.4.2** 搜索条件构建：根据 `search_schema` 中的字段类型和 `queryType` 自动构建 WHERE 条件；未在 search_schema 中声明的字段忽略，防止参数注入
- **BR4.4.3** 写入字段白名单：POST/PUT 请求的 body 数据，仅允许 `edit_schema` 中声明的字段写入数据库；未声明的字段静默忽略
- **BR4.4.4** 自动填充字段：新增时自动填充 `tenant_id`（从 Sa-Token 上下文获取）、`create_by`/`create_time`/`create_dept`；更新时自动填充 `update_by`/`update_time`
- **BR4.4.5** 逻辑删除支持：如果目标表有 `del_flag` 字段，删除操作执行 `UPDATE SET del_flag='1'`；否则执行物理 `DELETE`
- **BR4.4.6** 租户隔离：DynamicCrudController 的所有查询自动追加 `tenant_id` 条件（`TenantLineInnerInterceptor` 自动处理），确保租户数据隔离
- **BR4.4.7** 配置不存在或已停用时，DynamicCrudController 返回 `RespInfo.error("CRUD 配置不存在或已停用")`，HTTP 404

### 4.5 菜单注册规则

- **BR4.5.1** 配置创建时自动在 `sys_resource` 插入菜单记录，`component` 固定为 `ai/crud-page`，`path` 格式为 `/ai/crud-page?configKey={configKey}`
- **BR4.5.2** `menuParentId` 默认为 AI 管理目录的 `id`（需在 SQL 初始化数据中确保该目录存在）；用户可在配置中指定其他父级
- **BR4.5.3** 菜单的 `perms`（权限标识）格式为 `ai:crud:{configKey}`，自动生成
- **BR4.5.4** 配置删除时联动删除菜单，同时删除菜单关联的角色-资源关联记录
- **BR4.5.5** 配置更新时如 `menuName`/`menuSort`/`menuParentId` 变化，同步更新 `sys_resource` 对应记录

### 4.6 双轨并行规则

- **BR4.6.1** `mode=CONFIG` 的配置：前端通用渲染页渲染，后端 DynamicCrudController 执行 CRUD
- **BR4.6.2** `mode=CODEGEN` 的配置：走现有代码生成器流程，生成独立前后端文件，不使用 DynamicCrudController
- **BR4.6.3** mode 从 CONFIG 切换到 CODEGEN 时：保留 `ai_crud_config` 记录（标记为 CODEGEN），但 DynamicCrudController 不再响应该 configKey 的请求；删除对应的 `sys_resource` 菜单记录
- **BR4.6.4** mode 从 CODEGEN 切换到 CONFIG 时：需要用户确认"将使用配置驱动模式渲染，已生成的代码文件不会被删除但不再使用"

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|----------|------|
| 新增 | `ai_crud_config` | 见下方 DDL | CRUD 配置主表 |
| 新增索引 | `ai_crud_config` | `uk_config_key` (config_key) | configKey 唯一索引 |
| 新增索引 | `ai_crud_config` | `idx_table_name` (table_name) | 按表名查询 |
| 新增索引 | `ai_crud_config` | `idx_tenant_id` (tenant_id) | 租户隔离查询 |
| 新增 | `ai_agent` | 插入 1 条 Agent 记录 | `crud_config_builder` Agent 初始数据 |

### 新增表 `ai_crud_config` DDL

```sql
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
  `menu_resource_id` bigint DEFAULT NULL COMMENT '关联的sys_resource记录ID（菜单注册后回填）',
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
```

### 新增 Agent 初始数据 SQL

```sql
INSERT INTO `ai_agent` (`id`, `tenant_id`, `agent_name`, `agent_code`, `description`, `system_prompt`, `provider_id`, `model_name`, `temperature`, `max_tokens`, `extra_config`, `status`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (替换为雪花ID, 1, 'CRUD配置生成器', 'crud_config_builder', '根据数据库表结构或自然语言描述，生成完整的CRUD页面配置JSON',
'你是一个CRUD页面配置生成器。根据用户提供的数据库表结构或自然语言描述，生成完整的CRUD页面配置JSON。

输出格式要求（严格JSON）：
{
  "tableName": "表名",
  "tableComment": "表描述",
  "menuName": "菜单名称",
  "menuSort": 排序值,
  "searchSchema": [搜索表单配置数组],
  "columns": [表格列配置数组],
  "editSchema": [编辑表单配置数组]
}

字段配置规则：
- searchSchema: 仅需要搜索的字段，type映射：EQ→select, LIKE→input, BETWEEN→daterange, IN→select(multiple), GT/LT→number
- columns: 需要在列表展示的字段，有dictType的用dict渲染，datetime格式化，image用图片渲染
- editSchema: 新增/编辑表单字段，type映射：INPUT→input, TEXTAREA→textarea, SELECT→select, RADIO→radio, CHECKBOX→checkbox, DATETIME→datetime, IMAGE→imageUpload, FILE→fileUpload, NUMBER→number
- 必填字段设置required:true，字符串输入设maxlength
- 基类字段(id/tenant_id/create_by/create_time/create_dept/update_by/update_time)不放入editSchema

可用字典：{{dictList}}',
NULL, NULL, 0.3, 4096, NULL, '0', NULL, NOW(), NULL, NOW());
```

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 新增 | `GET /ai/crud-config/page` | AiCrudConfigController.page | 分页查询配置列表 |
| 新增 | `GET /ai/crud-config/{id}` | AiCrudConfigController.getById | 查询配置详情 |
| 新增 | `GET /ai/crud-config/render/{configKey}` | AiCrudConfigController.render | 获取渲染配置（前端通用渲染页调用，返回 AiCrudPage 完整 props） |
| 新增 | `POST /ai/crud-config` | AiCrudConfigController.create | 新增配置（同时自动注册菜单） |
| 新增 | `PUT /ai/crud-config` | AiCrudConfigController.update | 修改配置（同时联动更新菜单） |
| 新增 | `DELETE /ai/crud-config/{id}` | AiCrudConfigController.delete | 删除配置（同时联动删除菜单） |
| 新增 | `POST /ai/crud-config/ai/generate` | AiCrudConfigController.aiGenerate | AI 生成完整配置（入参：description 或 tableName，返回预览 JSON，不入库） |
| 新增 | `POST /ai/crud-config/ai/generateFromTable` | AiCrudConfigController.aiGenerateFromTable | 已有表导入 → AI 增强生成配置（入参：tableName，读取元数据+AI增强） |
| 新增 | `GET /ai/crud/{configKey}/page` | DynamicCrudController.page | 通用分页查询 |
| 新增 | `GET /ai/crud/{configKey}/{id}` | DynamicCrudController.getById | 通用详情查询 |
| 新增 | `POST /ai/crud/{configKey}` | DynamicCrudController.create | 通用新增 |
| 新增 | `PUT /ai/crud/{configKey}` | DynamicCrudController.update | 通用更新 |
| 新增 | `DELETE /ai/crud/{configKey}/{id}` | DynamicCrudController.delete | 通用删除 |

### 新增 DTO 定义

**AiCrudConfigDTO**（新增/修改配置入参）：
```java
public class AiCrudConfigDTO {
    private Long id;                        // 修改时传入
    private String configKey;               // 配置标识，创建时必填，创建后不可修改
    private String tableName;               // 关联表名，必填
    private String tableComment;            // 表描述
    private String searchSchema;            // JSON 字符串
    private String columnsSchema;           // JSON 字符串
    private String editSchema;              // JSON 字符串
    private String apiConfig;               // JSON 字符串
    private String options;                 // JSON 字符串
    private String mode;                    // CONFIG | CODEGEN，默认 CONFIG
    private String status;                  // 0启用 | 1停用
    private String menuName;                // 菜单名称
    private Long menuParentId;              // 菜单父级ID
    private Integer menuSort;               // 菜单排序
}
```

**AiCrudConfigRenderVO**（渲染配置输出）：
```java
public class AiCrudConfigRenderVO {
    private String configKey;
    private String tableName;
    private String tableComment;
    private Object searchSchema;            // 已解析的 JSON 对象
    private Object columnsSchema;           // 已解析的 JSON 对象
    private Object editSchema;              // 已解析的 JSON 对象
    private Object apiConfig;               // 已解析的 JSON 对象
    private Object options;                 // 已解析的 JSON 对象
    private String rowKey;                  // 默认 "id"
    private String modalType;               // 默认 "drawer"
    private String modalWidth;              // 默认 "800px"
    private Integer editGridCols;           // 默认 1
    private Integer searchGridCols;         // 默认 4
}
```

**AiCrudGenerateRequest**（AI 生成配置入参）：
```java
public class AiCrudGenerateRequest {
    private String description;     // 自然语言描述（与 tableName 二选一）
    private String tableName;       // 已有表名（与 description 二选一）
}
```

**AiCrudGenerateResult**（AI 生成配置输出）：
```java
public class AiCrudGenerateResult {
    private String tableName;
    private String tableComment;
    private String menuName;
    private Integer menuSort;
    private Object searchSchema;            // 完整的 searchSchema JSON
    private Object columnsSchema;           // 完整的 columns JSON
    private Object editSchema;              // 完整的 editSchema JSON
    private Object apiConfig;               // 自动生成的 apiConfig JSON
    private Object options;                 // 推荐的 options JSON
    private String rawResponse;             // AI 原始响应（调试用）
}
```

## 7. 影响范围

### 7.1 后端模块影响

| 模块 | 影响类型 | 说明 |
|------|---------|------|
| forge-plugin-generator | 新增功能 | 新增 AiCrudConfig 实体/Mapper/Service/Controller；新增 SchemaGenerator；新增 DynamicCrudController/DynamicCrudService；GenController 新增 AI 配置生成接口 |
| forge-plugin-system | 间接影响 | AiCrudConfigService 通过 ISysResourceService 接口操作 sys_resource 表，不修改 system 模块代码，仅调用现有接口 |
| forge-plugin-ai | 新增 Agent | 插入 `crud_config_builder` Agent 初始数据到 `ai_agent` 表，不修改 ai 模块代码 |
| forge-admin-server | 新增桥接 | 如需跨模块调用（generator→system），可能需要在 admin 层做协调；如 AiCrudConfigService 直接注入 ISysResourceService 则不需要额外桥接 |

### 7.2 前端影响

| 文件/组件 | 影响类型 | 说明 |
|-----------|---------|------|
| 新增 `ai/crud-page.vue` | 新增 | 通用 CRUD 渲染页，从 query 参数获取 configKey，调用 render 接口加载配置 |
| 新增 `ai/crud-config.vue` | 新增 | CRUD 配置管理页面，使用 AiCrudPage 展示配置列表 |
| 新增 `AiGenerateConfigModal.vue` | 新增 | AI 生成配置弹窗（自然语言输入 / 已有表选择 → 预览 → 确认） |
| `api/ai.js` | 修改 | 新增 crud-config 相关 API 接口定义 |
| `components/ai-form/` | 不修改 | AiCrudPage/AiForm/AiFormItem 组件不做任何修改，纯复用 |

### 7.3 数据库影响

| 变更 | 说明 |
|------|------|
| 新增表 `ai_crud_config` | CRUD 配置驱动表 |
| 新增 `ai_agent` 记录 | `crud_config_builder` Agent 初始数据 |
| 新增 `sys_resource` 目录 | "AI 配置页面"目录菜单（如不存在），用于挂载 crud-page 渲染的菜单项 |

### 7.4 不受影响的部分

- **AiCrudPage/AiForm/AiFormItem 组件**：纯复用，不修改
- **现有代码生成器流程**：Velocity 模板、GenTable/GenTableColumn CRUD、代码预览/下载均不变
- **AI 底层基础设施**：AiClient/ChatClientCache/CircuitBreaker/ContextInjector 不变
- **现有 AI 推荐服务**：AiSchemaService/AiRecommendService 逻辑不变，新增的 SchemaGenerator 是独立服务
- **大屏生成功能**：完全不受影响

## 8. 风险与关注点

### 8.1 技术风险

| 风险 | 等级 | 说明 | 缓解措施 |
|------|------|------|----------|
| MyBatis-Plus 动态表名与租户拦截器冲突 | 🔴高 | `dynamicTableName` 与 `TenantLineInnerInterceptor` 同时使用时，可能因 SQL 改写顺序导致租户条件丢失或重复 | Task 2 首先验证兼容性；如冲突则改用 `JdbcTemplate` + 手动参数化查询，放弃 dynamicTableName 方案 |
| DynamicCrudController SQL 注入风险 | 🔴高 | 动态拼装 WHERE 条件时，如果字段名/值未严格校验，可能导致 SQL 注入 | BR4.4.2/4.4.3 白名单校验：搜索字段必须在 search_schema 声明、写入字段必须在 edit_schema 声明；参数化查询，禁止拼接 SQL |
| JSON Schema 配置与 AiCrudPage props 不兼容 | 🟡中 | SchemaGenerator 生成的 JSON 结构如果与 AiCrudPage 期望的 props 格式不一致，前端渲染会出错 | SchemaGenerator 的输出格式严格对照 `AiCrudPageProps.js` 定义；每个 Task 完成后端到端验证 |
| AI 生成配置 JSON 格式不稳定 | 🟡中 | AI 输出的 JSON 结构可能与预期格式不符（字段名错误、嵌套层级不对等） | BR4.3.2 校验层兜底；解析失败时尝试容错修复（如字段名大小写归一化）；无法修复则返回原始响应供用户手动编辑 |
| 菜单注册失败导致配置与菜单不一致 | 🟡中 | 配置创建成功但菜单注册失败（如 parentId 不存在），导致配置存在但无入口 | 配置创建和菜单注册在同一事务中；菜单注册失败则配置也回滚；记录 WARN 日志 |
| 大表字段过多导致 JSON 配置超大 | 🟢低 | 字段数超过 50 的表，生成的 Schema JSON 可能很大，影响加载速度 | 前端配置缓存（F3.5.3）；render 接口启用 gzip 压缩；大表建议手动精简字段配置 |
| DynamicCrudController 并发性能 | 🟢低 | 所有配置驱动的 CRUD 请求都走同一 Controller，可能成为瓶颈 | DynamicCrudService 内部无状态，仅读配置+执行 SQL，瓶颈在数据库而非 Controller；如需优化可加缓存 |

### 8.2 业务风险

| 风险 | 等级 | 说明 | 缓解措施 |
|------|------|------|----------|
| 配置驱动模式无法满足复杂业务需求 | 🟡中 | 多表关联查询、审批流、复杂计算逻辑等场景无法通过通用 Controller 实现 | BR4.6.1-4.6.4 双轨并行，mode=CODEGEN 仍可走代码生成；配置驱动明确定位为"简单CRUD" |
| 用户误删配置导致页面不可用 | 🟡中 | 删除 `ai_crud_config` 记录后，对应的 CRUD 页面立即 404 | 删除前检查是否有关联的 sys_resource 菜单，提示用户"删除配置将同时删除菜单入口"；逻辑删除优先（status='1'） |
| 不同租户配置隔离问题 | 🟢低 | 配置驱动模式下，A 租户可能看到 B 租户的配置 | `ai_crud_config` 继承 TenantEntity，TenantLineInnerInterceptor 自动追加租户条件；render 接口按当前用户租户过滤 |
| 权限控制粒度不足 | 🟢低 | DynamicCrudController 的权限标识为 `ai:crud:{configKey}`，仅能控制整个页面的访问权限，无法控制行级/列级权限 | V1 不做行级/列级权限；如需细粒度权限控制，切换到 mode=CODEGEN 生成独立代码 |

## 8.5 测试策略

- **测试范围**：SchemaGenerator、DynamicCrudController、AiCrudConfigService（含菜单联动）、AI 配置生成、前端通用渲染页
- **覆盖率目标**：SchemaGenerator 90%+，DynamicCrudService 80%+，菜单联动 100%（分支覆盖）
- **独立 Test Spec**：否（本变更以集成测试为主）

### 测试方法

| 测试类型 | 范围 | 方法 |
|---------|------|------|
| 单元测试 | SchemaGenerator | 构造不同 queryType/htmlType/dictType 的 GenTableColumn 列表，验证生成的 searchSchema/columns/editSchema JSON 结构正确 |
| 单元测试 | SchemaGenerator 边界 | 基类字段不进 editSchema、isRequired 映射为 required、dictType 为空时不生成 dict 渲染 |
| 单元测试 | DynamicCrudService 字段白名单 | 构造含非法字段的请求 body，验证非法字段被过滤；构造合法字段，验证正常写入 |
| 单元测试 | DynamicCrudService 逻辑删除 | 目标表有 del_flag → 验证 UPDATE 逻辑；无 del_flag → 验证 DELETE 逻辑 |
| 单元测试 | AiCrudConfigService 菜单联动 | 创建配置 → 验证 sys_resource 记录存在；删除配置 → 验证 sys_resource 记录已删；更新 menuName → 验证同步更新 |
| 集成测试 | DynamicCrudController E2E | 创建配置 → GET /page 返回分页数据 → POST 新增 → GET /{id} 返回详情 → PUT 更新 → DELETE 删除 |
| 集成测试 | MyBatis-Plus 动态表名+租户拦截器 | 同一数据库两个租户各创建配置 → A 租户查询只能看到 A 的数据 → B 租户查询只能看到 B 的数据 |
| 集成测试 | AI 配置生成 E2E | 输入自然语言 → AI 返回完整配置 → 校验 JSON 结构 → 确认入库 → 验证 ai_crud_config 记录 + sys_resource 记录 |
| 集成测试 | AI 降级 E2E | 不配置 Provider → 调用 AI 生成接口 → 已有表模式返回纯规则配置 / 自然语言模式返回降级提示 |
| 前端测试 | 通用渲染页 | 访问 /ai/crud-page?configKey=xxx → 加载配置 → 渲染搜索+表格 → 新增/编辑/删除 → 验证完整 CRUD 流程 |
| 前端测试 | 配置管理页面 | 列表展示 → AI 生成配置 → 预览 → 确认入库 → 菜单出现入口 |
| 前端测试 | DictTag/AuthImage 渲染 | 配置含 dictType 字段 → 验证 DictTag 渲染；配置含 image 字段 → 验证 AuthImage 渲染 |

### 回归测试检查清单

- [ ] 现有代码生成器：导入表 → 字段配置 → 预览 → 下载，全流程不变
- [ ] AI 字段推荐：ColumnConfigModal 中 AI 推荐按钮正常
- [ ] AI 建表：AiSchemaModal 中自然语言输入 → Schema 预览 → 导入，全流程不变
- [ ] AI 供应商/模型管理：CRUD 正常
- [ ] 大屏生成：流式/非流式正常
- [ ] 系统菜单管理：CRUD 正常，配置驱动的菜单在菜单树中可见

## 9. 待澄清

- [x] Q1：运行时 CRUD Controller 实现方式 → **A) 通用 DynamicCrudController**
- [x] Q2：配置存储方式 → **A) 单表 + JSON 字段**
- [x] Q3：菜单/路由注册方式 → **A) 通用渲染页 + query 参数**
- [x] Q4：复杂业务逃逸通道 → **A) 配置驱动为主 + 代码生成为辅（双轨并行）**
- [x] Q5：已有表导入后的配置生成流程 → **A) AI 增强生成 + 预览确认**
- [ ] Q6：MyBatis-Plus `dynamicTableName` 与 `TenantLineInnerInterceptor` 的兼容性——需在 Task 2 中验证，如不兼容则改用 JdbcTemplate 方案
- [ ] Q7：`ai_crud_config` 表放在哪个模块——当前设计中 DynamicCrudController 和 AiCrudConfigService 都在 `forge-plugin-generator`，需确认是否合理（generator 的语义是"代码生成"，配置驱动不属于"生成"）

## 10. 技术决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 运行时 CRUD Controller | A) 通用 DynamicCrudController | 单个 Controller、零代码生成、热生效、统一管控；用户确认 |
| 配置存储方式 | A) 单表 + JSON 字段 | 简单直接、一读出完整配置、与 AiCrudPage 的 JSON 配置协议对齐；用户确认 |
| 菜单/路由注册 | A) 通用渲染页 + query 参数 | 复用现有菜单体系、无需动态路由、unplugin-vue-router 兼容；用户确认 |
| 复杂业务逃逸 | A) 配置驱动为主 + 代码生成为辅 | 双轨并行、渐进式迁移、不丢能力；用户确认 |
| 配置生成流程 | A) AI 增强生成 + 预览确认 | AI 价值最大化、用户只需确认无需手写；用户确认 |
| SchemaGenerator 位置 | forge-plugin-generator | 与现有 GenTableColumn/AiSchemaService/AiRecommendService 同模块，复用最直接 |
| DynamicCrudController 位置 | forge-plugin-generator | 与配置管理同模块，避免跨模块调用；如 Q7 确认拆分则调整 |
| 字段白名单校验方式 | 基于 edit_schema JSON 声明 | 运行时从配置中提取允许的字段集合，无需硬编码，配置变更时自动生效 |
| 逻辑删除检测 | 查询表结构是否有 del_flag 字段 | 动态检测，无需用户手动配置删除方式 |
| AI 降级策略 | 已有表→纯规则生成；自然语言→提示不可用 | 与 ai-codegen-enhance 一致的降级策略 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|-------------|------|

## 12. 审查结论

> 审查时间: 2026-04-20
> 审查结果: ❌ FAIL

### 阶段一 Spec Compliance: ❌ FAIL

**严重偏差（5项）：**
1. [BR4.4.6] DynamicCrudService 使用 JdbcTemplate 绕过 TenantLineInnerInterceptor，无租户隔离
2. [BR4.4.4] insert/update 未自动填充 tenant_id/create_by/create_time 等审计字段
3. [BR4.1.2] tableName 物理表存在性校验未实现
4. [BR4.4.2] 搜索条件仅用 = 精确匹配，未按 queryType 区分 LIKE/BETWEEN/IN
5. [BR4.1.1] configKey 创建后可被修改

**一般偏差（10项）：**
6. [BR4.2.1] searchSchema EQ→input，Spec 要求 EQ→select
7. [BR4.2.2] columns 缺少 IMAGE→AuthImage 渲染和 DATETIME 格式化
8. [BR4.2.4] editSchema 缺少 validateRule 透传和 maxlength 默认值
9. [BR4.3.2] AI 输出校验层未实现（htmlType/dictType 校验）
10. [BR4.3.5] AiCrudGenerateResult 缺少 menuName/menuSort/options/rawResponse 字段
11. [F3.5.3] 前端配置缓存未实现
12. [BR4.5.2] menuParentId 默认 0L，应为 AI 管理目录 ID
13. [BR4.5.4] 删除菜单未删除角色-资源关联记录
14. [BR4.6.3/BR4.6.4] mode 切换逻辑未实现
15. [SQL] sys_resource 初始化数据 tenant_id=0，应为 1

### 阶段二 Code Quality: ❌ FAIL

**Critical（2项）：**
- C1: DynamicCrudService 租户隔离漏洞
- C2: 审计字段未自动填充

**Important（6项）：**
- I1: 菜单注册失败不回滚事务（catch 吞异常）
- I2: 双重否定可读性差（!isNotBlank → isBlank）
- I3: hasDelFlag 缺少 table_schema 过滤
- I4: updateMenu 可能 null 覆盖其他字段
- I5: 魔法值未定义为常量
- I6: 分页参数未校验上限

### 修复优先级
1. 必须修复: C1 租户隔离 → C2 审计字段 → 严重偏差#3-5
2. 应修复: I1 菜单回滚 → AI预览步骤 → SchemaGenerator 偏差
3. 建议修复: I2-I6 及一般偏差#6-15

## 13. 确认记录（HARD-GATE）

- **确认时间**：
- **确认人**：
