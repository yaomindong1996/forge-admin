# Spring AI Alibaba 供应商适配层与 DashScope 原生接入

> status: apply
> created: 2026-07-10
> complexity: 🔴复杂

## 1. 背景与目标

Forge 当前通过 Spring AI `ChatClient` 提供统一 AI 调用入口，并在数据库中维护多租户 AI 供应商、模型和 Agent 配置。但底层模型创建固定使用 `OpenAiApi + OpenAiChatModel + OpenAiChatOptions`，`providerType` 只承担展示和筛选作用。阿里百炼虽然可以借助 OpenAI Compatible API 使用，系统却无法利用 Spring AI Alibaba 的原生 DashScope 模型、原生参数、推理内容和后续 Tool Calling 扩展能力。

本变更在不改变 `AiClient.call/stream` 公共协议、不切换 Java/Spring 主干、不破坏现有供应商配置的前提下，完成以下可验证结果：

1. 将模型创建从 OpenAI 具体实现中解耦为显式 Provider Adapter SPI；
2. 引入 Spring AI Alibaba `1.1.2.3` 依赖基线和 DashScope 核心模型模块；
3. 新增 `openai_compatible`、`dashscope_native` 两种适配器，既有数据默认保持原链路；
4. 支持数据库动态 API Key、Base URL、模型、温度和最大 Token 参数构建原生 `DashScopeChatModel`；
5. 修复供应商配置更新后缓存未失效，以及 API Key 列表/详情回传和脱敏值回写覆盖问题；
6. 在当前生效的供应商管理页显式配置连接适配器，并支持安全连接测试；
7. 为后续 Spring AI Alibaba Agent Framework、MCP Registry、Nacos 和 Studio 接入保留稳定扩展点，但不在本变更引入这些运行时。

### 1.1 成功标准

- 既有供应商迁移后全部使用 `openai_compatible`，行为和接口路径保持不变；
- 新建 `dashscope_native` 供应商后，普通调用和流式调用均通过 `DashScopeChatModel` 完成；
- `AiClientImpl`、`ChatClientCache`、`AiProviderService` 不再直接构造具体供应商模型；
- DTO 归一化后进入 Registry 的适配器必须为已知非空值；持久化 null/blank/unknown 一律失败关闭，不静默切换协议；
- 供应商列表、详情和连接测试链路不向浏览器返回或要求回传真实 API Key；
- 修改 API Key、Base URL 或适配器后，后续调用不再复用旧 `ChatClient`；
- Maven 依赖树中 Spring AI 收敛到 `1.1.2`，Spring AI Alibaba 与 Extensions 收敛到 `1.1.2.3`；
- 后端目标模块测试、主应用装配编译、Flyway 静态检查和前端构建通过。

## 2. 代码现状（Research Findings）

> 每个结论均给出当前工程或本地官方源码出处。`code-copilot/rules/project-context.md` 仍记录旧 Spring Boot `3.2.9`，本变更以实际 Maven POM 为版本事实来源。

### 2.1 相关入口与链路

1. **统一调用入口已存在**：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiClient.java` 定义同步与流式调用协议；`AiClientImpl.call/stream` 负责解析 Agent/供应商/模型、创建 `ChatClient`、注入会话记忆并持久化会话。
2. **供应商与 Agent 解析集中在一处**：同模块 `client/AiInvocationResolver.java` 的 `resolve` 方法决定显式请求、Agent 配置和默认供应商之间的优先级；`resolveProvider` 当前统一要求 `baseUrl/apiKey` 非空。
3. **模型缓存集中在一处**：同模块 `client/ChatClientCache.java` 的 `getOrCreateBase` 缓存基础 `ChatClient`，`createSessionClient` 叠加 `MessageChatMemoryAdvisor`。
4. **供应商管理入口已存在**：同模块 `provider/controller/AiProviderController.java` 提供 `/ai/provider/page`、详情、新增、修改、删除、连接测试和设为默认接口；`provider/service/AiProviderService.java#testConnection` 直接创建模型进行探活。
5. **当前实际菜单绑定组合页**：`forge-server/forge-admin-server/sql/初始化脚本.sql` 的“供应商管理”资源组件路径为 `/ai/provider-model`；对应页面是 `forge-admin-ui/src/views/ai/provider-model.vue`。`forge-admin-ui/src/views/ai/provider.vue` 是未被当前菜单绑定的旧页面，本变更不删除它。

### 2.2 现有实现

1. **版本主轴基本对齐**：`forge-server/pom.xml` 当前为 Spring Boot `3.5.13`、Spring AI `1.1.2`，并仅声明未使用的 `spring-ai-alibaba.version=1.1.2.0`。本地官方 `docs/vendor/spring-ai-alibaba-1.1.2.3-release/pom.xml` 使用 Spring AI `1.1.2`、Boot `3.5.8`；`docs/vendor/spring-ai-extensions-1.1.2.3-release/pom.xml` 使用 Spring AI `1.1.2`、Boot `3.5.10`。
2. **当前只依赖 Spring AI OpenAI 实现**：`forge-plugin-ai/pom.xml` 显式依赖 `spring-ai-openai`、`spring-ai-client-chat` 并逐项写版本，没有导入 Spring AI、Alibaba Extensions、Alibaba 三个 BOM。
3. **所有供应商都走 OpenAI 模型**：`client/ChatClientCache.java#buildBaseChatClient` 和 `provider/service/AiProviderService.java#testConnection` 都直接创建 `OpenAiApi`、`OpenAiChatModel`、`OpenAiChatOptions`；`provider/domain/AiProvider.java#providerType` 未参与模型路由。
4. **模型参数类型泄漏到上层**：`AiClientImpl#buildOptions` 返回 `OpenAiChatOptions`，导致通用调用层无法传递 DashScope 原生 `DashScopeChatOptions`。
5. **缓存失效方法没有调用者**：`ChatClientCache#evict/evictByProvider` 已实现，但工程内除定义外没有引用；`AiProviderController#update/delete` 更新或删除配置后不会主动失效旧客户端。
6. **连接测试依赖浏览器回传密钥**：`forge-admin-ui/src/views/ai/provider-model.vue#handleTestConnection` 从列表行读取 `row.apiKey` 并传给 `/ai/provider/test`。改为安全脱敏后，该链路必须以供应商 ID 在服务端解析真实密钥。
7. **现有“脱敏”方法未实际脱敏**：`AiProviderController#fillAndMaskProvider` 只调用 `fillModelsFromAiModel`，没有处理 `apiKey`；`@ApiEncrypt` 只负责报文加密，不能替代字段脱敏。
8. **现有 SQL 对阿里供应商类型存在历史差异**：`forge-plugin-ai/src/main/resources/sql/ai_model.sql` 使用 `alibaba`，`forge-admin-server/sql/初始化脚本.sql` 使用 `dashscope`。因此 `providerType` 不适合作为底层协议路由键。
9. **现有阿里模板使用 OpenAI Compatible 地址**：`AiProviderController#templates` 返回 `https://dashscope.aliyuncs.com/compatible-mode`；原生 DashScope 源码 `DashScopeApiConstants` 使用 Base URL `https://dashscope.aliyuncs.com` 和路径 `/api/v1/services/aigc/text-generation/generation`，两者不能混用。

### 2.3 官方源码验证

1. `spring-ai-alibaba/examples/chatbot/pom.xml` 同时导入 `spring-ai-bom:1.1.2`、`spring-ai-alibaba-extensions-bom:1.1.2.3`、`spring-ai-alibaba-bom:1.1.2.3`。
2. `docs/vendor/spring-ai-extensions-1.1.2.3-release/models/dashscope/src/main/java/com/alibaba/cloud/ai/dashscope/chat/DashScopeChatModel.java` 实现 Spring AI `ChatModel`；其 Builder 支持动态传入 `DashScopeApi` 和 `DashScopeChatOptions`。
3. `DashScopeApi.java#Builder` 支持运行时设置 `apiKey`、`baseUrl`、Header、Workspace 和 HTTP Client，不要求使用 Spring Boot 全局配置 Bean。
4. `DashScopeChatOptions.java` 实现 `ToolCallingChatOptions`，支持 `model`、`temperature`、`maxToken`、`enableThinking`、工具回调等原生选项。
5. `DashScopeChatModel` 将推理内容写入 AssistantMessage metadata 的 `reasoningContent`；现有 `AiClientImpl#extractReasoningContent` 已识别该键。
6. `spring-ai-alibaba-starter-dashscope` 会引入自动配置；`DashScopeChatAutoConfiguration` 与 `ConditionalOnDashScopeEnabled` 均为 `matchIfMissing=true`，并尝试从 `spring.ai.dashscope.*` 或环境变量创建全局模型。Forge 使用租户数据库动态配置，因此本变更选择核心模块 `spring-ai-alibaba-dashscope`，不引入 Starter。

### 2.4 发现与风险

- Spring Boot 只存在补丁版本差异，但仍需通过 `dependency:tree`、目标模块测试和主应用装配验证，不以“同一 minor”代替证据；
- `forge-dependencies/pom.xml` 内仍保留旧 Boot `3.2.9` BOM，根 POM 先导入 Boot `3.5.13`；本变更不顺带重构全局 BOM，但必须检查 effective dependency tree 是否被旧 BOM 污染；
- Maven 本地仓库当前没有 `com.alibaba.cloud.ai` 产物，本地 vendor 源码只作审计参考，正式构建仍应使用 Maven 发布坐标；仓库不可达时才在开发机临时安装对应 release，不把 vendor 源码加入 Forge modules；
- 适配器切换会改变请求协议和计费路径，禁止在运行失败时从原生 DashScope 自动降级到 OpenAI Compatible，避免一次业务调用产生双重请求；
- API Key 当前存在返回浏览器风险，本变更触及供应商配置链路，必须同步完成脱敏和脱敏占位回写保护；数据库静态加密与密钥轮换属于独立安全能力，不在本次范围。

## 3. 功能点

- [ ] **依赖基线**：根 POM 统一管理 Spring AI `1.1.2`、Spring AI Alibaba `1.1.2.3`、Extensions `1.1.2.3`，AI 插件只声明无版本依赖。
- [ ] **适配器数据契约**：为 `ai_provider` 增加非空 `adapter_code`，既有数据全部回填 `openai_compatible`，通过新字典维护展示值。
- [ ] **Provider Adapter SPI**：按 `adapterCode` 选择模型实现，未知值抛出 `BusinessException`，不依据 `providerType` 或 URL 猜测。
- [ ] **OpenAI Compatible Adapter**：承接现有 `OpenAiApi/OpenAiChatModel` 行为，覆盖 OpenAI、DeepSeek、智谱、Moonshot、Ollama、自定义及旧阿里兼容模式。
- [ ] **DashScope Native Adapter**：使用数据库配置动态创建 `DashScopeApi/DashScopeChatModel/DashScopeChatOptions`，支持同步、流式、推理内容和 Tool Calling 基础协议。
- [ ] **调用层解耦**：`AiClientImpl` 和 `ChatClientCache` 只使用协议无关运行参数与 `ChatModel`，不再导入供应商 Options 类型。
- [ ] **统一连接测试**：连接测试与正式调用复用同一 Adapter Registry；已保存供应商只传 ID，服务端读取真实凭据。
- [ ] **缓存一致性**：供应商修改、删除成功后按 providerId 失效缓存；缓存键包含 adapter、模型及非敏感运行参数，不包含明文 API Key。
- [ ] **密钥保护**：列表和详情返回前 4 位 + `****` + 后 4 位；短密钥全部掩码；提交未变化的脱敏值时保留数据库原值。
- [ ] **管理端配置**：当前生效的 `provider-model.vue` 使用字典展示和选择适配器；新建默认为 `openai_compatible`，选择原生 DashScope 时使用原生 Base URL。
- [ ] **回归验证**：覆盖适配器选择、参数映射、未知适配器失败关闭、密钥脱敏回写、缓存失效和既有 OpenAI Compatible 调用。

## 4. 业务规则

### 4.1 适配器路由

1. `providerType` 继续表示供应商品牌/分类，`adapterCode` 唯一决定底层协议；二者不得混用。
2. `adapterCode` 仅允许字典中的稳定值：`openai_compatible`、`dashscope_native`。
3. 历史记录无条件回填 `openai_compatible`，升级过程不自动把任何现有阿里供应商切为原生协议。
4. DTO 字段未提供或 JSON `null` 时：新增按 `openai_compatible`，更新保留数据库原值；显式提交空字符串或纯空白一律拒绝，禁止把原生供应商静默改回 Compatible。
5. DTO 归一化之后，`AiProviderAdapterCode.require` 与 Registry 遇到 null、blank、unknown 或重复注册必须失败关闭，不允许回退到默认 Adapter。
6. 一次请求只能选择一个 Adapter；调用失败不得跨 Adapter 自动重试。
7. `AiProviderAdapterRegistry#createChatModel` 是正式模型创建的唯一入口，固定执行 `getRequired → validate → createChatModel`；正式调用、连接测试和缓存层不得直接调用具体 Adapter 绕过校验。

### 4.2 参数与端点

1. 通用运行参数固定为 `model/temperature/maxTokens`，各 Adapter 负责映射供应商 Options；
2. OpenAI Compatible 要求 `apiKey/baseUrl/model`；DashScope Native 要求 `apiKey/model`，Base URL 为空时使用官方默认值 `https://dashscope.aliyuncs.com`；
3. Base URL 先执行 `trim`、URI 解析、HTTP/HTTPS scheme 校验并移除比较用尾斜杠；禁止 query、fragment 和 userInfo。归一化只处理格式，不猜测或改写自定义代理域名；
4. `dashscope_native` 在官方域名下只接受空路径/根路径，拒绝 `/compatible-mode`；自定义域名只做通用 URI 校验，由其代理实现负责原生 DashScope Path；
5. `openai_compatible` 的 Base URL 必填；当 host 为 `dashscope.aliyuncs.com` 时只接受 `/compatible-mode`（允许尾斜杠），拒绝原生根地址和 `/compatible-mode/v1`；自定义域名只做通用 URI 校验，不自动追加 `/compatible-mode`；
6. DashScope 最大 Token 映射到 `DashScopeChatOptions.Builder#maxToken`，不能误用 OpenAI Builder；
7. 本阶段不开放任意 `extraBody`、Header、Workspace 配置，避免在没有字段级治理前形成自由透传入口。

### 4.3 API Key

1. API Key 只能从加密请求体或数据库实体进入模型构建，不写日志、不进入异常消息、不进入缓存键；
2. 列表和详情只返回脱敏值：长度大于 8 时保留前 4 后 4，中间为 `****`；长度不大于 8 时返回固定掩码；
3. 更新时若提交值等于当前数据库值的脱敏结果，保留原密钥；提交新的非空明文才替换；
4. 连接测试使用严格 one-of 契约：`id != null` 时只允许提交 ID，出现 adapter/baseUrl/apiKey/model 等配置字段即拒绝；`id == null` 时必须提交 adapterCode、apiKey、model 及 Adapter 要求的其余完整配置；两种模式不得合并字段；
5. 新增、更新、连接测试请求必须经过前端加密标记和后端 `@ApiDecrypt`；连接测试 SDK 异常统一转换为不含密钥、Header、完整请求体的安全错误，日志只记录 providerId、adapterCode 和异常类型；
6. 不得将浏览器返回的脱敏值传给模型 SDK。

### 4.4 缓存与租户

1. `getOrCreateBase` 只接收完整 `AiProvider` 与运行参数，缓存键 tenantId 必须在内部取自 `AiProvider.tenantId`，不能由调用方单独传入或从可能为空的线程会话推断；
2. 供应商配置更新或删除成功后通过 after-commit 调度清理该租户/providerId 的所有客户端；事务回滚不得清缓存，无活动事务时在持久化成功返回后立即清理；
3. 缓存移除日志只能记录 tenantId、providerId、model、adapterCode 和 cause；
4. `ai_provider` 继续受 `TenantLineInnerInterceptor` 管理，测试连接按当前租户加载记录，不能接受请求体覆盖 tenantId。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增字段 | `ai_provider` | `adapter_code varchar(32) NOT NULL DEFAULT 'openai_compatible'` | 显式保存模型连接协议；Flyway 使用 `information_schema` 防重复 |
| 数据回填 | `ai_provider` | 空值补为 `openai_compatible` | 首次加列由默认值覆盖历史记录；部分部署重跑只处理 NULL/blank，不覆盖已有 Native |
| 新增字典类型 | `sys_dict_type` | `ai_provider_adapter_type` | tenant_id 固定为 `1`，使用 `NOT EXISTS` 防重复 |
| 新增字典数据 | `sys_dict_data` | `openai_compatible`、`dashscope_native` | tenant_id 固定为 `1`，值与后端 Adapter Code 完全一致 |

迁移脚本固定为 `forge-server/db/migration/V1.0.17__add_ai_provider_adapter_code.sql`。字段首次新增时由默认值把历史记录置为 Compatible；若字段已由部分部署创建，只允许更新 `adapter_code IS NULL OR TRIM(adapter_code)=''` 的记录，不得覆盖任何已有非空值。脚本重跑或修复执行不得把 `dashscope_native` 重置为 Compatible。不修改已执行历史 SQL；旧初始化脚本产生的数据库在执行 Flyway 后达到相同结构。

数据库结构采用前向兼容，不执行破坏性降级，也不删除已写入的 `adapter_code` 和字典。应用版本回退存在明确前置条件：必须先查询并确认不存在 `dashscope_native` 记录；若存在，需先由管理员切回 `openai_compatible` 并恢复 Compatible Base URL、完成连接测试后才能回退旧应用。未经该检查直接回退会让旧代码把原生 URL 当作 OpenAI Compatible 使用。需要物理回退时另行编写受审查的前向修复脚本。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 兼容增强 | `/ai/provider/page` | GET | 响应新增 `adapterCode`；`apiKey` 改为脱敏值 |
| 兼容增强 | `/ai/provider/{id}` | GET | 响应新增 `adapterCode`；`apiKey` 改为脱敏值 |
| 兼容增强 | `/ai/provider` | POST | 请求新增 `adapterCode`，缺省按 `openai_compatible`；校验 Adapter 与 Base URL |
| 兼容增强 | `/ai/provider` | PUT | 请求新增 `adapterCode`；缺失时保留原值；支持脱敏值不覆盖原密钥；成功后失效缓存 |
| 安全调整 | `/ai/provider/test` | POST | 严格 one-of：已保存记录只能传 `id`；未保存测试只能传无 ID 的完整配置；混合请求拒绝，SDK 错误安全化 |
| 兼容增强 | `/ai/provider/templates` | GET | 模板新增 `adapterCode`；新增/调整 DashScope Native 模板使用原生 Base URL |

接口 URL、HTTP Method、`RespInfo` 外层协议和前端 API 函数名保持不变。`AiClient` 的公开 Java 接口不变。

## 7. 影响范围

### 后端

- `forge-server/pom.xml`：AI 依赖版本与三个 BOM；
- `forge-plugin-ai/pom.xml`：增加 DashScope 核心模型依赖，移除显式版本；
- `forge-plugin-ai/client`：通用运行参数、缓存与调用层解耦；
- `forge-plugin-ai/provider`：Adapter SPI、原生 DashScope、供应商生命周期、DTO/VO 和密钥保护；
- `forge-server/db/migration`：字段与字典迁移；
- `forge-admin-server`：通过现有插件聚合加载新依赖，无新模块。

### 前端

- `forge-admin-ui/src/views/ai/provider-model.vue`：适配器字段、标签、默认值、原生 URL 和 ID 连接测试；
- `forge-admin-ui/src/api/ai.js`：接口路径不变，仅补充请求语义注释或辅助参数。

### 不在本次范围

- Spring AI Alibaba Agent Framework、Graph、A2A、Nacos、MCP Registry、Gateway、Studio；
- Embedding、Image、Audio、Rerank、Multimodal 等非 Chat 模型 Adapter；
- API Key 数据库存储加密、KMS 和密钥轮换；
- 全局 Spring Boot 版本升级或 `forge-dependencies` BOM 重构；
- 删除未绑定菜单的 `forge-admin-ui/src/views/ai/provider.vue`；
- 将 `docs/vendor` 源码加入 Maven reactor 或生产制品。

## 8. 风险与关注点

1. **依赖收敛风险**：三个 BOM 与 Boot/Forge BOM 顺序可能导致版本漂移，必须用 effective dependency tree 验证 Spring AI 只有 `1.1.2`；
2. **启动风险**：若误引入 `spring-ai-alibaba-starter-dashscope`，默认自动配置可能因没有全局 API Key 导致启动失败；POM 测试必须确认只依赖核心模型模块；
3. **协议地址风险**：Compatible 与 Native URL/Path 不同，官方 DashScope 域名执行双向协议校验；自定义代理域名只做 URI 安全校验，不自动拼接或静默改写；
4. **密钥覆盖风险**：前端回显脱敏值后保存可能覆盖真实 API Key，必须先加载持久值并比较其脱敏结果；
5. **缓存陈旧风险**：更新事务失败时不能提前清缓存；使用 after-commit 调度，回滚不清理，避免并发线程在提交前重新缓存旧配置；
6. **外部网络风险**：真实 DashScope 验证依赖网络和用户自行提供的环境变量，自动测试不得硬编码或提交密钥；
7. **成本风险**：连接测试会产生极少量模型调用，固定低 `maxTokens`，前端明确提示；测试失败不跨 Adapter 重试；
8. **版本文档风险**：官方根 README 中存在旧示例版本，依赖坐标以 release POM 和 BOM `1.1.2.3` 为准；
9. **安全标注**：本变更不涉及资金、业务状态流转或权限放开；涉及 API Key 敏感信息处理，需按安全红线人工审查。

## 8.5 测试策略

- **测试范围**：Adapter Registry、OpenAI Compatible、DashScope Native 参数映射、调用层解耦、缓存失效、密钥脱敏回写、Flyway、供应商管理 UI、主应用装配；
- **覆盖率目标**：Adapter 选择、未知值失败关闭、密钥保留/替换、缓存失效等 P0 决策分支 100% 场景覆盖；新增核心类行覆盖目标不低于 85%，不以全模块覆盖率作为阻断条件；
- **独立 Test Spec**：是，见同目录 `test-spec.md`；
- **自动测试**：不调用真实公网模型；Adapter 测试验证模型类型和 Options，`AiClientImplTest` 使用真实 `ChatClientCache`、Mock Registry 和可控 Fake `ChatModel`，离线强制覆盖 Native 供应商的同步、流式和 `reasoningContent` 输出链路；
- **可选集成测试**：仅在人工提供 `AI_DASHSCOPE_API_KEY` 且网络可用时运行一次 `qwen-plus` 同步与流式探活，日志不得记录密钥；
- **增量原则**：执行阶段先复用 `AiInvocationResolverTest` 基线，再跑目标模块、主应用装配、Flyway 静态检查和前端构建。

## 9. 待澄清

- 无。以下默认决策随本提案进入 HARD-GATE：既有数据全部保持 `openai_compatible`；本次只接入 Chat/DashScope Native；使用核心模块而非 Starter；Agent/MCP/Nacos 另立变更。

## 10. 技术决策

| 决策 | 选择 | 放弃方案 | 原因 |
|------|------|----------|------|
| 框架关系 | Spring AI 为通用接口，Spring AI Alibaba 作为增强层 | 删除 Spring AI、整体换框架 | Alibaba 本身建立在 Spring AI `ChatModel/ChatClient` 上，替换会制造无意义重写 |
| DashScope 依赖 | `spring-ai-alibaba-dashscope` 核心模块 | `spring-ai-alibaba-starter-dashscope` | Forge API Key 来自租户数据库，不适合全局自动配置单例 |
| 路由依据 | 新增显式 `adapter_code` | 根据 `providerType` 或 Base URL 推断 | 现有 `alibaba/dashscope` 和 Compatible/Native 地址存在历史差异，推断不稳定 |
| 历史迁移 | 全部回填 `openai_compatible` | 自动把阿里记录切为 Native | 保证升级零行为变化，由管理员显式切换并测试 |
| Adapter 失败策略 | Fail-closed | 自动换另一个 Adapter 重试 | 防止重复调用、重复计费和隐藏配置错误 |
| API Key 回显 | 脱敏 + 同值保留 | 返回明文或固定占位直接保存 | 满足安全规范并避免脱敏占位覆盖真实值 |
| Vendor 源码用途 | 版本/API 审计参考 | 加入 Forge reactor | 保持依赖边界清晰，使用正式 Maven 坐标 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Research / Proposal | 完成 | 本变更四份文档 | 已核对 Forge 当前实现及两个 `1.1.2.3` 官方源码目录 |
| Task 1 | 完成 | `forge-server/pom.xml`、`forge-plugin-ai/pom.xml` | 三个 BOM 已导入；DashScope Core `1.1.2.3` 编译通过，未引入 Starter |
| Task 2 | 完成 | `V1.0.17__add_ai_provider_adapter_code.sql`、`AiProvider.java`、`AiProviderAdapterCode.java`、对应测试 | 历史数据保持 Compatible；字典 tenant_id=1；Adapter Code 失败关闭测试通过 |
| Task 3-8 | 未开始 | - | 按 Tasks 顺序继续执行 |

## 12. 审查结论

- Spec 合规审查：模板、Research 出处、任务/测试映射和范围边界自检通过；
- 独立读者测试：第三轮复审 PASS；前两轮发现的 7 项阻断/高风险语义均已修订；
- 代码质量审查：尚未进入编码阶段；
- 安全审查重点：API Key 脱敏回写、日志和缓存键不泄密；
- 依赖审查重点：不引入 DashScope Starter，Spring AI 版本必须收敛。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-10 22:04:42 +08:00
- **确认人**：用户（当前会话）
