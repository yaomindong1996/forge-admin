# AI供应商与模型管理
> status: review
> created: 2026-04-17
> complexity: 🟡中等
## 1. 背景与目标

当前 forge-plugin-ai 模块的 AI 模型数据存储在 `ai_provider` 表的 `models`（JSON字符串）字段中，无法对模型进行独立的增删改查管理。随着接入的 AI 供应商和模型类型增多，需要：

1. **供应商管理**：提供完整的 AI 供应商 CRUD 管理页面，支持测试连接
2. **模型管理**：将模型从供应商的 JSON 字段中拆分为独立实体，支持按供应商筛选、配置模型类型/名称/描述/模型ID 等核心参数
3. **接口兼容**：旧接口返回的 `models` 和 `defaultModel` 字段保持可用，通过双写同步策略实现

**完成后可验证的结果**：
- 管理端侧边栏新增"AI管理"菜单，包含"供应商管理"和"模型管理"两个子页面
- 供应商页面支持增删改查 + 测试连接
- 模型页面支持增删改查 + 按供应商/类型筛选
- 旧接口 `GET /ai/provider/{id}` 和 `GET /ai/provider/page` 仍返回 `models` 和 `defaultModel` 字段

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

**后端模块**：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/`

| 层 | 类 | 路径 | 说明 |
|---|---|---|---|
| Entity | `AiProvider` | `provider/domain/AiProvider.java` | 供应商实体，表 `ai_provider` |
| Entity | `AiAgent` | `agent/domain/AiAgent.java` | Agent实体，表 `ai_agent`，通过 `providerId` 关联供应商 |
| Controller | `AiProviderController` | `provider/controller/AiProviderController.java` | 供应商 API，路径 `/ai/provider` |
| Service | `AiProviderService` | `provider/service/AiProviderService.java` | 供应商服务，含 `testConnection()`、`setDefault()` |
| Mapper | `AiProviderMapper` | `provider/mapper/AiProviderMapper.java` | 纯 BaseMapper，无自定义SQL |
| Service | `AiChatService` | `chat/service/AiChatService.java` | AI对话服务，含供应商/模型解析逻辑 |

**前端**：无任何 AI 管理页面、API 定义或路由配置

### 2.2 现有实现

**`AiProvider` 实体字段**（`provider/domain/AiProvider.java`）：
| 字段 | 类型 | 数据库列 | 说明 |
|------|------|---------|------|
| `id` | `Long` | `id` | PK |
| `providerName` | `String` | `provider_name` | 供应商名称 |
| `providerType` | `String` | `provider_type` | 类型(openai/azure/dashscope/ollama) |
| `apiKey` | `String` | `api_key` | API Key |
| `baseUrl` | `String` | `base_url` | API Base URL |
| `models` | `String` | `models` | 可用模型列表JSON |
| `defaultModel` | `String` | `default_model` | 默认模型名称 |
| `isDefault` | `String` | `is_default` | 是否默认(0否1是) |
| `status` | `String` | `status` | 状态(0正常1停用) |
| `remark` | `String` | `remark` | 备注 |

**`AiProviderController` 现有接口**（`provider/controller/AiProviderController.java`）：
| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `templates()` | GET | `/ai/provider/templates` | 内置供应商模板 |
| `page()` | GET | `/ai/provider/page` | 分页查询 |
| `getById()` | GET | `/ai/provider/{id}` | 详情 |
| `create()` | POST | `/ai/provider` | 新增 |
| `update()` | PUT | `/ai/provider` | 修改 |
| `delete()` | DELETE | `/ai/provider/{id}` | 删除 |
| `test()` | POST | `/ai/provider/test` | 测试连接 |
| `setDefault()` | PUT | `/ai/provider/{id}/default` | 设为默认 |

**`AiProviderService.testConnection()`**（`provider/service/AiProviderService.java`）：
- 构建 `OpenAiApi` + `OpenAiChatModel`，发送 "hi" 消息测试连通性
- 当前使用 `defaultModel` 作为测试模型

**`AiChatService` 模型解析逻辑**（`chat/service/AiChatService.java`）：
- `resolveProvider()`: 请求传入 providerId → Agent 关联 providerId → 默认供应商
- `resolveModel()`: 请求传入 modelName → Agent 配置 modelName → Provider defaultModel → "gpt-3.5-turbo"

**前端 CRUD 页面模式**：
- 所有管理页面使用 `AiCrudPage` 组件（`components/ai-form/AiCrudPage.vue`）
- 通过 `api-config`、`searchSchema`、`columns`、`editSchema` 四个核心配置驱动
- 路由由 `unplugin-vue-router` 从 `src/views/` 目录结构自动生成

### 2.3 发现与风险

1. **模型数据无独立管理能力**：`ai_provider.models` 为 JSON 字符串，无法对单个模型进行增删改查
2. **双写同步复杂度**：新 `ai_model` 表与 `ai_provider.models` 字段需保持一致，写入模型时需同步更新供应商的 models JSON
3. **Agent 关联模型**：`AiAgent.modelName` 存储模型名称字符串，拆分后需确认是否改为关联 modelId 或保持字符串匹配
4. **前端从零搭建**：当前无任何 AI 管理前端代码

## 3. 功能点

### 3.1 AI 供应商管理页面
- [ ] 供应商列表：分页展示，支持按供应商名称、类型、状态搜索
- [ ] 新增供应商：填写名称、类型、API Key、Base URL、备注；类型可选内置模板自动填充
- [ ] 编辑供应商：修改供应商配置信息
- [ ] 删除供应商：删除前校验是否有关联模型，有关联则提示不可删除
- [ ] 测试连接：调用 `POST /ai/provider/test` 验证供应商连通性，展示成功/失败结果
- [ ] 设为默认：将供应商设为默认供应商

### 3.2 AI 模型管理页面
- [ ] 模型列表：分页展示，支持按模型名称、供应商、模型类型搜索
- [ ] 新增模型：选择供应商、配置模型类型（chat/embedding/image/audio）、模型ID、模型名称、描述
- [ ] 编辑模型：修改模型配置
- [ ] 删除模型：删除后自动同步更新关联供应商的 `models` JSON 字段
- [ ] 模型与供应商联动：新增/编辑/删除模型时，双写同步更新 `ai_provider.models` 和 `ai_provider.defaultModel`

### 3.3 旧接口兼容
- [ ] `GET /ai/provider/page` 和 `GET /ai/provider/{id}` 返回结果中仍包含 `models`（JSON）和 `defaultModel` 字段
- [ ] 同步策略：模型变更时，自动将关联的 `ai_model` 记录聚合回写至 `ai_provider.models` 和 `ai_provider.defaultModel`

## 4. 业务规则

1. **供应商删除约束**：供应商下存在关联模型时，禁止删除，需先删除所有关联模型
2. **模型ID唯一性**：同一供应商下 `modelId` 不可重复
3. **默认供应商唯一**：同一租户下仅可有一个默认供应商（已有逻辑保证）
4. **双写同步一致性**：
   - 新增模型 → 追加到供应商 `models` JSON
   - 删除模型 → 从供应商 `models` JSON 移除
   - 修改模型的 modelId → 更新供应商 `models` JSON 中对应条目
   - 修改模型为供应商默认模型 → 更新 `ai_provider.default_model`
5. **模型类型**：支持 chat（对话）、embedding（向量化）、image（图像生成）、audio（语音）四种类型
6. **权限控制**：AI 管理页面仅管理员可见，使用菜单权限控制

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|----------|------|
| 新建 | `ai_model` | `id` BIGINT PK | 雪花ID |
| | | `provider_id` BIGINT NOT NULL | 关联供应商ID，FK → ai_provider.id |
| | | `model_type` VARCHAR(32) NOT NULL | 模型类型: chat/embedding/image/audio |
| | | `model_id` VARCHAR(128) NOT NULL | 模型标识（如 gpt-4o, text-embedding-3） |
| | | `model_name` VARCHAR(128) NOT NULL | 模型显示名称 |
| | | `description` VARCHAR(512) | 模型描述 |
| | | `is_default` CHAR(1) DEFAULT '0' | 是否为该供应商默认模型(0否1是) |
| | | `status` CHAR(1) DEFAULT '0' | 状态(0正常1停用) |
| | | `sort_order` INT DEFAULT 0 | 排序号 |
| | | `tenant_id` BIGINT | 租户编号 |
| | | `create_by` BIGINT | 创建人 |
| | | `create_time` DATETIME | 创建时间 |
| | | `create_dept` BIGINT | 创建部门 |
| | | `update_by` BIGINT | 更新人 |
| | | `update_time` DATETIME | 更新时间 |
| | | `remark` VARCHAR(512) | 备注 |
| | 索引 | `idx_ai_model_provider_id` (provider_id) | 供应商关联查询 |
| | 索引 | `idx_ai_model_type` (model_type) | 按类型查询 |
| | 唯一索引 | `uk_ai_model_provider_model` (provider_id, model_id) | 同一供应商下模型ID唯一 |

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|---------|
| 新增 | `/ai/model/page` | GET | 模型分页查询，支持 providerId/modelType/modelName 筛选 |
| 新增 | `/ai/model/list` | GET | 按供应商ID查询所有模型列表（用于下拉选择） |
| 新增 | `/ai/model/{id}` | GET | 模型详情 |
| 新增 | `/ai/model` | POST | 新增模型，双写同步 ai_provider.models |
| 新增 | `/ai/model` | PUT | 修改模型，双写同步 ai_provider.models |
| 新增 | `/ai/model/{id}` | DELETE | 删除模型，双写同步 ai_provider.models |
| 修改 | `/ai/provider/page` | GET | 返回结果中 models/defaultModel 由 ai_model 表聚合填充（保持响应格式不变） |
| 修改 | `/ai/provider/{id}` | GET | 返回结果中 models/defaultModel 由 ai_model 表聚合填充（保持响应格式不变） |
| 保留 | `/ai/provider/test` | POST | 复用现有测试连接逻辑，不变 |
| 保留 | `/ai/provider/templates` | GET | 内置模板不变 |

## 7. 影响范围

### 后端
- **新增模块**：`forge-plugin-ai` 下的 `model/` 包（domain/mapper/service/controller）
- **修改文件**：`AiProviderController.java` — page/getById 返回时聚合 ai_model 数据填充 models/defaultModel
- **修改文件**：`AiProviderService.java` — 删除供应商时增加关联模型校验
- **新增文件**：`AiModel.java`、`AiModelMapper.java`、`AiModelService.java`、`AiModelController.java`
- **SQL 脚本**：新建 `ai_model` 表的 DDL + 菜单数据插入

### 前端
- **新增页面**：`src/views/ai/provider.vue` — 供应商管理页面
- **新增页面**：`src/views/ai/model.vue` — 模型管理页面
- **新增 API**：`src/api/ai.js` — AI 相关 API 定义
- **菜单数据**：通过后端 SQL 插入菜单记录，前端路由自动生成

### 不影响
- `AiChatService` 的对话逻辑不变（仍通过 providerId + modelName 解析）
- `AiAgent` 的模型关联方式不变（仍存储 modelName 字符串）
- 前端 Chat 相关功能不受影响

## 8. 风险与关注点

1. **⚠️ 双写同步一致性**：模型 CRUD 操作需同步更新 `ai_provider.models` JSON 字段，需在同一事务内完成，避免数据不一致
2. **⚠️ 旧数据迁移**：现有 `ai_provider.models` 中已有的 JSON 数据需要迁移到 `ai_model` 表，需编写数据迁移脚本
3. **并发安全**：多用户同时操作同一供应商下的模型时，双写同步需考虑并发场景（MyBatis-Plus 行级锁或乐观锁）
4. **API Key 安全**：供应商管理页面展示 API Key 时需脱敏显示，编辑时若未修改则不传回后端

## 8.5 测试策略

- **测试范围**：后端模型 CRUD 接口、双写同步逻辑、供应商删除约束、旧接口兼容性；前端页面交互
- **覆盖率目标**：双写同步核心逻辑 100% 覆盖
- **独立 Test Spec**：否（跟随主 Spec）

## 9. 待澄清

- [x] ~~现有 `ai_provider.models` 中的数据格式是什么？需要确认 JSON 结构以便编写迁移脚本~~ → 已确认：后端代码中未解析 models JSON，仅前端展示用。`models` 字段为字符串数组 JSON（如 `["gpt-4o","gpt-4o-mini"]`），迁移脚本在 Task 1 实现时根据实际数据格式编写
- [x] ~~是否需要在供应商管理页面中直接内嵌管理该供应商下的模型？~~ → 已确认：完全独立页面，模型管理页面通过下拉选择供应商筛选

## 10. 技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 模型存储方式 | 独立 `ai_model` 表 | 模型需要独立增删改查，JSON 字段无法满足 |
| 旧接口兼容策略 | 双写同步 | 旧接口不需要修改代码，响应格式完全不变 |
| 模型类型范围 | chat/embedding/image/audio | 覆盖主流 AI 能力类型 |
| 权限粒度 | 管理员级别 | AI 管理属于系统配置，无需细粒度权限 |
| 前端页面布局 | 两个独立页面 | 清晰独立，符合系统管理页面组织方式 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|-------------|------|
| Task 1 | ✅ 完成 | `forge-plugin-ai/src/main/resources/sql/ai_model.sql` | 含建表DDL+菜单+迁移脚本 |
| Task 2 | ✅ 完成 | `model/domain/AiModel.java`, `model/mapper/AiModelMapper.java`, `model/service/AiModelService.java` | 含双写同步 |
| Task 3 | ✅ 完成 | `model/controller/AiModelController.java` | 6个REST接口 |
| Task 4 | ✅ 完成 | `provider/service/AiProviderService.java`, `provider/controller/AiProviderController.java` | 删除校验+聚合填充 |
| Task 5 | ✅ 完成 | `forge-admin-ui/src/api/ai.js`, `forge-admin-ui/src/views/ai/provider.vue` | API+供应商页面 |
| Task 6 | ✅ 完成 | `forge-admin-ui/src/views/ai/model.vue` | 模型管理页面 |
| Task 7 | ✅ 完成 | (合并到Task 1) | 菜单数据在 ai_model.sql 中 |

## 12. 审查结论

待审查

## 13. 确认记录（HARD-GATE）
- **确认时间**：2026-04-17
- **确认人**：用户
