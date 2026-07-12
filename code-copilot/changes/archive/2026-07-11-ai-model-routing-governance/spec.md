# AI 模型路由与调用治理

> status: done
> created: 2026-07-11
> complexity: 🔴复杂

## 1. 背景与目标

`spring-ai-alibaba-provider-adapter` 已完成 Spring AI 统一接口、OpenAI Compatible 与 DashScope Native 显式适配、默认模型权威来源和供应商连接安全边界。当前调用仍只能按“请求显式模型 → Agent 固定模型 → 供应商默认模型”解析，系统不知道模型是否支持推理、Tool Calling、视觉或结构化输出，也无法说明一次调用为什么选择某个模型、消耗多少 Token、耗时多久。

本变更在不引入 Nacos、MCP Registry/Admin、Agent Framework，不改变 `AiClient.call/stream` 公共入口的前提下，完成一个确定性、可审计的模型治理闭环：

1. 为模型维护可扩展能力目录、上下文窗口和按百万 Token 计价信息；
2. 为 Agent 增加“固定模型 / 路由策略”两种选择模式，历史 Agent 默认保持固定模型行为；
3. 通过显式候选模型、能力要求和优先级进行确定性路由；
4. 调用前可以跳过已经熔断的显式候选，请求发出后失败不得在同一请求内换模型重试；
5. 真实业务调用与手动连接测试共同维护模型健康状态，默认不执行产生费用的定时探测；
6. 记录路由来源、模型、Token、耗时、错误分类和价格快照，不记录 Prompt、响应正文或密钥；
7. 提供最小管理界面，完成模型能力配置、路由策略管理、Agent 绑定和调用记录查询；
8. 通过 `AiModelRouter`、`AiModelHealthRegistry` SPI 为后续 Redis/Nacos、MCP 与 Agent Runtime 保留替换点。

### 1.1 成功标准

- 显式 `providerId/modelName` 和历史 Agent 的固定模型行为保持兼容；
- 路由模式 Agent 只能从策略显式配置的候选模型中选择，不做全库猜测；
- 候选模型必须启用、供应商启用、能力满足、未逻辑删除且未处于 OPEN 状态；
- 选择顺序固定为 `priority ASC → target.id ASC`，同样输入和健康快照得到同样结果；
- 已知 OPEN 候选可以在调用前被跳过；实际网络调用失败后本次请求立即结束，不自动重试或跨供应商补发；
- 同步和流式调用都能记录唯一 requestId、路由结果、耗时、Token 和安全错误分类；
- 熔断键包含 tenantId、providerPk、modelPk（数据库主键），不能继续按 agentCode 或供应商模型字符串混合状态；
- 后端单测、AI 插件完整测试、Admin 主应用装配、Flyway 静态/实库条件验证和前端构建通过。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

1. 统一调用入口位于 `forge-plugin-ai/client/AiClient.java` 和 `AiClientImpl#call/#stream`；两条链路都先调用 `AiInvocationResolver#resolve`，再通过 `ChatClientCache` 创建会话客户端。
2. `AiInvocationResolver#resolveModel` 当前优先使用请求 `modelName`、`AiAgent.modelName`，最后调用 `AiModelService#requireEnabledDefaultModelId`；没有路由策略、能力校验或模型实体级决策结果。
3. `AiModelService#requireEnabledDefaultModelId` 与 `AiModelMapper.xml#selectEnabledDefaultModelId` 已把 `ai_model` 中启用且默认的模型作为权威来源，可作为固定模式兼容兜底。
4. `AiProviderAdapterRegistry#createChatModel` 已是 ChatModel 创建的唯一入口；本变更只决定“选哪个 AiModel”，不把路由逻辑下沉到供应商 Adapter。
5. 管理入口分别是 `forge-admin-ui/src/views/ai/provider-model.vue` 和 `agent.vue`；前者维护供应商/模型，后者当前直接选择 providerId 与 modelName。

### 2.2 现有数据与实现

1. `model/domain/AiModel.java` 只有 providerId、modelType、modelId、maxTokens、默认标志和状态，没有能力、上下文窗口或价格字段。
2. `agent/domain/AiAgent.java` 只有 providerId、modelName、temperature、maxTokens 和 extraConfig，没有模型选择模式或路由策略引用。
3. `client/CircuitBreaker.java` 使用 `ConcurrentHashMap`，阈值固定为 3 次、恢复时间固定 5 分钟，调用方传入的 key 是 agentCode；同一 Agent 切换模型会共享熔断状态，不同 Agent 调用同一故障模型又互不感知。
4. `AiClientImpl#call` 使用 `ChatClient.CallResponseSpec#content()`，无法取得 `ChatResponseMetadata.getUsage()`；流式链路虽然消费 `ChatResponse`，但没有聚合最终 Usage。
5. Spring AI `1.1.2` 的 `ChatResponse#getMetadata`、`ChatResponseMetadata#getUsage` 和 `Usage#getPromptTokens/#getCompletionTokens/#getTotalTokens` 已提供统一 Token 元数据；本地 Maven 坐标为 `org.springframework.ai:spring-ai-model:1.1.2`。
6. `chat/domain/AiChatRecord.java` 只有单个 tokenUsage 字段，且按 user/assistant 消息保存，不能表达路由来源、模型、输入/输出 Token、耗时和错误，因此不能承担调用治理日志。
7. `AiClientImpl` 当前 INFO 日志输出 systemPrompt、userPrompt 和截断后的 assistantContent；这与治理层“不持久化或日志打印业务 Prompt/响应正文”的安全目标冲突。
8. `AiModelController`、`AiAgentController` 和部分 Service 仍使用 LambdaQueryWrapper；本变更新增的候选查询、统计与分页 SQL 必须写入 Mapper XML，不扩散 Java 动态复杂 SQL。

### 2.3 已有可复用能力

1. `provider/adapter/AiProviderAdapterRegistry`、`AiModelRuntimeOptions`、`ChatClientCache` 已把模型创建与上层调用解耦，路由结果只需输出 provider、modelId 和运行参数。
2. `AiProviderFailureDiagnostics` 已能安全提取 HTTP 状态和白名单错误码，可复用于调用记录和健康失败分类。
3. Spring AI 自带 ChatModel Observation 与 Usage Metrics 类型；本变更保留 Micrometer 指标，不用自定义日志替代框架指标，同时增加 Forge 租户级数据库审计。
4. `forge-starter-job` 提供 `@ScheduledJob`，可用于调用日志保留清理；该任务只物理删除超期技术日志，不删除模型、策略或业务配置。
5. `AiProviderService#getDefaultProvider` 当前委托 `AiProviderMapper.xml#selectDefaultProvider`，查询会受 TenantLineInnerInterceptor 限制并过滤 `is_default='1'/status='0'/del_flag='0'`，但通过 `ORDER BY create_time DESC LIMIT 1` 隐藏多默认脏数据；本变更将其收敛为当前租户恰好一个启用默认供应商的失败关闭契约。

### 2.4 发现与风险

- 如果把能力写死成多个 boolean 字段，每新增一种能力都要改表；因此能力采用字典代码 + 关系表，模型主表只保存数值型治理信息；
- 如果路由器扫描所有启用模型，会形成不可预测的跨供应商行为；因此每个策略必须维护显式候选集合；
- 如果调用失败后自动尝试下一个模型，可能重复计费、重复 Tool 副作用或产生两份回答；因此只允许调用前跳过已知 OPEN 候选；
- 健康状态默认是单实例内存快照，多实例间不会立即共享；必须通过 SPI 隔离，后续可接 Redis/Nacos，但本变更不提前引入控制面；
- Token Usage 由供应商决定，可能为空；缺失时 Token 字段统一记录 NULL 且 `usageAvailable=false`，禁止把字符数伪装成官方 Token；
- 模型价格会变化；调用日志保存当次价格快照，汇总成本使用 Token × 每百万 Token 单价计算，金额最终以分为单位返回；
- 调用日志是高增长技术日志，允许按保留策略物理清理；模型、能力、策略和候选仍遵循逻辑删除。

## 3. 功能点

- [x] **模型能力目录**：管理员为模型配置能力代码、上下文窗口、输入/输出每百万 Token 单价；字典初始包含 `streaming/reasoning/tool_calling/vision/structured_output`。
- [x] **路由策略**：管理员创建租户内未逻辑删除记录唯一的 policyCode，配置所需能力和显式候选模型优先级；候选可以来自不同供应商，但不会自动发现或自动加入。
- [x] **Agent 选择模式**：Agent 可选择 `PINNED` 固定模型或 `POLICY` 路由策略；历史 null 值按 PINNED 解析。
- [x] **确定性路由**：路由器按显式优先级过滤停用、能力不足和已熔断候选，返回包含 source/policy/reason 的 RouteDecision。
- [x] **调用前健康跳过**：已处于 OPEN 状态的候选不接收新请求；下一个候选仅在策略中显式存在时才可被选中。
- [x] **禁止失败后重试**：一旦调用已经发送给模型，任何错误都结束本次调用，不在本请求中选择第二模型。
- [x] **被动健康治理**：同步/流式成功记录成功；分类后的网络、鉴权、模型不存在、限流和 5xx 错误记录失败；业务参数校验、内容安全和调用方取消不污染模型健康。
- [x] **手动健康重置**：供应商连接测试成功、模型/供应商配置更新成功后清理对应模型健康状态。
- [x] **单模型手动测试**：管理员可对任意启用 Chat 模型执行低 Token 测试，服务端读取持久化凭据并更新该模型健康状态，浏览器不回传 API Key。
- [x] **调用审计**：记录 requestId、tenant/user、agent/session、路由来源、策略、provider/model、adapter、成功状态、错误分类、HTTP 状态、安全错误码、耗时、Token 和价格快照。
- [x] **安全日志收敛**：AiClient 日志不再打印 systemPrompt、userPrompt 或 assistantContent，只记录 requestId、长度和非敏感路由元数据。
- [x] **最小管理界面**：供应商模型弹窗维护能力/价格；Agent 配置选择固定模型或策略；新增“模型治理”页面维护路由策略并查询调用记录。
- [x] **日志保留**：默认每天清理 90 天前调用日志，保留天数作为任务参数可调整；只物理删除技术调用日志。

## 4. 业务规则

### 4.1 解析优先级与兼容

1. 历史 Agent 的 modelSelectionMode 为 null/blank 时按 PINNED 处理；未知非空值失败关闭。
2. Agent `modelSelectionMode=PINNED` 保存时必须把 routePolicyId 归一化为 null；`POLICY` 必须配置 routePolicyId，历史 providerId/modelName 可以保留用于切回固定模式，但运行时忽略。
3. POLICY 模式找不到满足条件的候选时抛出 `BusinessException("没有满足路由策略的可用模型")`，不回退供应商默认模型。

显式请求按以下完整决策表解析，任何显式字段都会绕过 Agent POLICY，但仍接受模型归属、租户、启停和健康校验：

| 请求 providerId | 请求 modelName | 最终 provider | 最终 model | source / reason |
|------------------|-----------------|---------------|-------------|-----------------|
| 有 | 有 | 请求 provider | 请求 modelName，必须属于该 provider | REQUEST / REQUEST_EXPLICIT_PAIR |
| 有 | 无 | 请求 provider | 该 provider 的权威默认模型；不得拼接 Agent model | REQUEST / REQUEST_PROVIDER_DEFAULT |
| 无 | 有 | PINNED Agent provider；没有则系统默认 provider；POLICY Agent 直接使用系统默认 provider | 请求 modelName，必须属于最终 provider | REQUEST / REQUEST_MODEL_WITH_RESOLVED_PROVIDER |
| 无 | 无 | PINNED Agent provider | PINNED Agent model；为空则 provider 权威默认模型 | PINNED 或 PROVIDER_DEFAULT |
| 无 | 无 | POLICY 路由决定 | POLICY 路由决定 | POLICY / POLICY_PRIORITY |

`providerId` 指供应商数据库主键；请求/Agent 的 `modelName` 对应 `AiModel.modelId` 的供应商模型字符串，不是 `ai_model.id`。

“系统默认 provider”的权威来源固定为 `AiProviderService#requireEnabledDefaultProvider`：Mapper XML 在当前租户内查询最多两条 `is_default='1' AND status='0' AND del_flag='0'` 记录；恰好一条时返回，零条提示“未配置可用的默认 AI 供应商”，多条提示“当前租户存在多个默认 AI 供应商”。停用、已删除和其他租户记录都不参与；禁止继续通过 ORDER BY/LIMIT 静默选择一条。

### 4.2 候选过滤与选择

1. 路由策略、目标模型、目标供应商必须属于当前租户且状态启用、未逻辑删除。
2. 策略 required capability 必须全部包含在模型有效能力集合中；能力缺失直接排除。
3. 候选排序固定为 target.priority ASC、target.id ASC，不引入随机、权重、实时价格打分或 LLM 自主选择。
4. 健康状态 UNKNOWN/HEALTHY/DEGRADED 可以被选中，OPEN 不允许正式调用；恢复窗口到期后变为 HALF_OPEN，只允许一次试探调用。
5. HALF_OPEN 并发控制使用 `AiModelHealthLease`：Router 的正式 route 原子获取 Lease，成功调用 `success()` 恢复 HEALTHY，dispatched 后失败调用 `failure()` 重新 OPEN，取消调用 `cancel()`，模型创建/缓存/Adapter 准备阶段失败调用 `abort()` 释放试探权且不增加失败计数。
6. 纯数据 `RouteDecision` 必须包含选择来源 `REQUEST/PINNED/PROVIDER_DEFAULT/POLICY`、`AiModelRouteReason`、policyId 和被跳过候选原因；正式调用使用 `RoutedInvocation(RouteDecision, AiModelHealthLease)`，preview 只返回 RouteDecision。
7. 策略 preview 只读取健康快照，不获取 HALF_OPEN 试探令牌、不改变健康状态；正式调用在模型创建前才执行原子 acquire。
8. REQUEST/PINNED/PROVIDER_DEFAULT 同样受健康门控：OPEN 直接失败，HALF_OPEN 未取得试探令牌直接失败；只有 POLICY 可以在调用前继续检查下一个显式候选。
9. requiredCapabilities 和模型 capabilityCode 必须来自稳定能力代码集合；未知非空代码在保存时拒绝，运行时历史脏数据失败关闭。

### 4.3 失败、熔断与费用

1. 调用发送前跳过 OPEN 候选不算网络重试；调用发送后发生任何错误均不切换模型。
2. `AiInvocationPhase` 固定为 RESOLUTION/PREPARATION/DISPATCHED/STREAMING/COMPLETED；只有 DISPATCHED/STREAMING 后的连接超时、网络异常、429、供应商 5xx、鉴权失败、模型不存在和 UNKNOWN 异常影响健康。RESOLUTION/PREPARATION 失败必须 abort Lease，不增加失败计数。
3. 流式调用在收到正常完成信号后记录成功；ERROR 记录失败；CANCEL 只记录取消，不增加模型失败计数。
4. 价格字段单位为“分/百万 Token”，类型为 long；调用日志保存价格快照。成本先在查询周期内汇总 `Σ(promptTokens×inputPrice + completionTokens×outputPrice)`，再统一除以 1,000,000 并按 HALF_UP 舍入到整数分，禁止逐调用先舍入；SQL 乘法先转 DECIMAL(38,0) 防止 long 溢出。
5. Usage 缺失时 prompt/completion/total_tokens 统一保存 NULL，usageAvailable=false；不能用 0 同时表达“真实零消耗”和“未知”。
6. 供应商配置更新和供应商连接测试成功调用 `resetProvider(tenantId, providerPk)`；模型配置更新只 reset 对应 `AiModelHealthKey`，不通过 Service 互相注入枚举模型。
7. costAvailable 只有在 usageAvailable=true 且输入/输出价格快照均非 NULL 时为 true；任一价格缺失时不按 0 元计算，不进入成本合计，并计入 costUnavailableCount。
8. P95 延迟使用 nearest-rank：按 latencyMs 升序，取 1-based `ceil(0.95×N)`；空集合返回 NULL，单条记录返回自身值。

### 4.4 安全与租户

1. tenantId、userId 只来自 Session/已验证机器上下文，客户端参数不能覆盖。
2. 调用记录禁止保存 Prompt、上下文变量、响应正文、API Key、Authorization Header、原始异常 message 或供应商响应正文。
3. errorCode 继续使用 `AiProviderFailureDiagnostics` 白名单化结果；未知值统一为 null/UNKNOWN。
4. 模型治理和调用记录接口使用独立权限资源；写接口使用 `@ApiDecrypt`，响应按项目策略使用 `@ApiEncrypt`。
5. 路由策略跨供应商只是显式管理配置，不允许运行时代码根据品牌、URL、价格或错误自动构造候选。
6. `AiInvocationObservation` 中 requestId、tenantId、agentCode、phase、dispatched、outcome、latencyMillis 必填；userId/sessionId/routeSource/routeReason/policyId/providerPk/modelPk/providerModelId/adapterCode/errorCategory/httpStatus/errorCode/Token/价格快照可按解析阶段为空。该类型不接收 Throwable，并且从数据结构上不提供 Prompt、响应、Header、API Key 或 nativeUsage 字段。

## 5. 数据变更

迁移脚本固定为 `forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql`，所有 DDL/字典/资源使用 information_schema 或 NOT EXISTS 防重复保护，业务内置数据 tenantId 为 1。

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增字段 | `ai_model` | `context_window int`、`input_price_per_million_cent bigint`、`output_price_per_million_cent bigint` | 数值型模型治理信息，价格单位为分/百万 Token |
| 新增表 | `ai_model_capability` | model_id、capability_code、config_json、status、标准审计字段、del_flag、logic_delete_active | 模型能力关系；唯一键 `(tenant_id, model_id, capability_code, logic_delete_active)` |
| 新增表 | `ai_model_route_policy` | policy_code、policy_name、required_capabilities(JSON)、status、标准审计字段、del_flag、logic_delete_active | 可复用路由策略；租户内未逻辑删除记录 policyCode 唯一，status 不改变唯一性 |
| 新增表 | `ai_model_route_target` | policy_id、model_id（`ai_model.id` 内部主键）、priority、status、标准审计字段、del_flag、logic_delete_active | 显式候选列表；唯一键 `(tenant_id, policy_id, model_id, logic_delete_active)` |
| 新增字段 | `ai_agent` | `model_selection_mode varchar(16) DEFAULT 'PINNED'`、`route_policy_id bigint` | 历史记录保持 PINNED；POLICY 模式绑定策略 |
| 新增表 | `ai_model_invocation_log` | request_id、user_id、agent_code、session_id、phase、dispatched、route_source、route_reason、route_policy_id、provider_id（供应商 PK）、model_id（模型 PK）、provider_model_id（`AiModel.modelId` 字符串）、adapter_code、outcome、error_category、http_status、error_code、latency_ms、prompt/completion/total_tokens、usage_available、cost_available、价格快照、标准审计字段 | 路由失败时 provider/model 允许 NULL；追加型技术日志；requestId 唯一；按 tenant/time、model/time、agent/time 建索引 |
| 新增字典 | `sys_dict_type/data` | `ai_model_capability_type`、`ai_agent_model_selection_mode`、`ai_model_health_status`、`ai_invocation_outcome` | 前端禁止硬编码状态与选项 |
| 新增资源 | `sys_resource` | 模型治理菜单、策略 CRUD、预览、调用记录查询权限 | NOT EXISTS 防重复，tenantId=1 |

`ai_model_invocation_log` 属于运行技术日志，不提供普通行级删除；保留任务按时间物理删除超期记录，符合项目日志留存例外。其余新增设计态配置表全部逻辑删除。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 扩展 | `/ai/model` | POST/PUT/GET | 模型 DTO/VO 增加能力、上下文窗口、价格和只读健康状态；能力关系在 Service 事务内保存 |
| 新增 | `/ai/model/{id}/test` | POST | 服务端加载模型与供应商凭据执行低 Token 手动测试，更新模型健康状态 |
| 新增 | `/ai/model-routing/policy/page` | GET | 路由策略分页 |
| 新增 | `/ai/model-routing/policy/{id}` | GET/DELETE | 策略详情与逻辑删除 |
| 新增 | `/ai/model-routing/policy` | POST/PUT | 新增/修改策略及显式候选，事务内校验租户、模型和能力 |
| 新增 | `/ai/model-routing/policy/preview` | POST | 只读取路由与健康快照，不获取 HALF_OPEN 令牌、不调用模型；返回选中项和跳过原因 |
| 新增 | `/ai/model-routing/invocation/page` | GET | 按时间、Agent、供应商、模型、结果分页查询安全调用记录 |
| 新增 | `/ai/model-routing/invocation/summary` | GET | 返回调用数、成功率、Token、P95 延迟和估算成本（分） |
| 扩展 | `/ai/agent` | POST/PUT/GET | 增加 modelSelectionMode、routePolicyId，保存前校验固定/策略模式互斥 |
| 兼容 | `/ai/client/call`、`/ai/client/stream` | POST | 请求协议不删除字段；响应保持兼容，内部增加 requestId 和路由/计量记录 |

## 7. 影响范围

- 后端：`forge-plugin-ai` 的 model、agent、client、provider、routing、invocation 包；AI 插件 POM 增加 `forge-starter-job`；
- 数据库：V1.0.18 迁移、字典、菜单权限和日志保留任务；
- 前端：`provider-model.vue`、`agent.vue`、新增 `model-routing.vue`、`src/api/ai.js`；
- 调用方：现有 AiClientRequest 保持兼容，固定模式行为不变；
- 运维：新增调用日志容量和每天一次保留清理；默认不增加模型探测请求或费用；
- 后续：MCP、Nacos、Agent Runtime 只依赖 Router/Health SPI，不直接读取当前数据库表。

### 7.1 明确不在本次范围

- Nacos MCP Registry、Admin、MCP Server/Client、Capability Registry；
- Spring AI Alibaba Agent Framework、AgentScope、A2A、多智能体编排；
- 失败后同请求自动重试、隐式跨供应商降级、加权随机和 AI 自主选模型；
- 定时付费健康探测、自动价格同步、质量评测和智能成本优化；
- Embedding/Image/Audio 的实际调用路由；数据结构可表达，但首期只接 Chat 调用链；
- 配额、面向客户的计费出账和资金结算；本期成本仅为治理估算值。

## 8. 风险与关注点

1. **重复计费/副作用风险**：严格禁止调用失败后换模型重试；后续若开放必须另立 Spec 并区分纯生成与 Tool 调用。
2. **健康误判风险**：错误分类必须单测覆盖；调用方取消和本地校验不能触发熔断。
3. **多实例一致性**：默认内存 HealthRegistry 只保证单实例；SPI 必须保持无 Spring/Nacos 具体类型，后续可以替换为 Redis/Nacos 实现。
4. **日志增长**：调用日志只保存元数据，建立时间索引，并由 90 天保留任务物理清理；截止时间之前的数据删除，等于截止时间的数据保留。
5. **Token 缺失**：供应商不返回 Usage 时只能标记缺失，不能伪造成本精度。
6. **价格精度**：单价和汇总成本使用 long 分；调用级价格快照保留，避免价格更新改变历史汇总。
7. **敏感内容**：本变更会删除现有 Prompt/响应正文 INFO 日志；这是安全收敛，不保留兼容开关。
8. **权限变更**：新增模型治理菜单和调用审计权限，必须由管理员显式授权；不默认开放给普通角色。
9. **状态流转**：健康状态只能由 HealthRegistry 状态机改变，禁止 Controller 直接 set；模型/策略启停仍走现有配置状态。

## 8.5 测试策略

- **测试范围**：候选排序与能力过滤、兼容优先级、健康状态机、失败分类、禁止失败后重试、同步/流式 Usage、调用记录安全字段、成本汇总、Mapper XML、Flyway、管理端交互；
- **覆盖率目标**：Router、HealthRegistry、InvocationRecorder、Resolver 新增分支行覆盖率不低于 85%，关键业务规则 P0 场景 100% 覆盖；
- **独立 Test Spec**：是，见 `test-spec.md`；
- **网络原则**：自动测试使用 Fake ChatModel，不调用真实公网模型；真实供应商仅作为有凭据时的人工可选验收；
- **回归范围**：`spring-ai-alibaba-provider-adapter` 的 51 个 AI 插件测试必须继续通过。

## 9. 待澄清

无。以下决策已由用户确认：

1. 使用显式候选切换：调用前可跳过已熔断候选，调用失败后不自动重试；
2. 本期交付后端闭环和最小管理界面，不建设完整治理大屏；
3. 健康状态采用真实调用被动统计 + 手动测试，不启用定时付费探测；
4. Nacos/MCP/Agent Framework 继续按后续阶段闸门独立建设。

## 10. 技术决策

| 决策 | 选择 | 放弃方案 | 原因 |
|------|------|----------|------|
| 路由算法 | 显式候选 + 能力全包含 + priority 确定性排序 | 全库扫描、权重随机、AI 自主选择 | 可解释、可测试、避免隐式跨供应商 |
| 路由来源 | `AiModelRouteSource` 枚举 | 任意 String | 防止审计值拼写漂移 |
| 降级时机 | 仅调用前跳过 OPEN 候选 | 失败后自动请求下一模型 | 避免重复计费和 Tool 副作用 |
| 健康来源 | 真实调用 + 手动测试 | 周期付费探测 | 不产生无业务费用 |
| 健康存储 | `AiModelHealthRegistry` SPI + 默认内存实现 | 直接绑定 Redis/Nacos | 当前单体可用，后续替换不改 Router |
| 能力模型 | 字典代码 + `ai_model_capability` 关系表 | 多个 boolean、单 JSON 字段 | 新增能力不改主表，便于约束和查询 |
| 计量来源 | Spring AI ChatResponse Usage | 字符数估算 | 只使用供应商/框架真实元数据 |
| 成本单位 | 分/百万 Token + 调用价格快照 | 浮点金额、调用级小数金额 | 符合 Forge long/分金额规则 |
| 调用审计 | 独立 append-only 技术日志 | 复用聊天消息表 | 聊天消息无法表达路由和错误元数据 |
| Agent 兼容 | null 选择模式视为 PINNED | 批量切换现有 Agent | 不改变上线行为 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Proposal Research | 完成 | 本 Spec、`execution-log.md` | 已核对模型、Agent、Resolver、AiClient、CircuitBreaker、Spring AI Usage API 和战略边界 |
| Task 1–9 | 完成 | 见 `tasks.md` 与 `execution-log.md` | 数据契约、路由、健康、审计、Agent 和最小管理端已实现 |
| Task 10 | 完成（条件项除外） | 测试、装配与静态扫描 | Review 阻断已修复；实库 Flyway 和浏览器交互继续作为条件项保留 |

## 12. 审查结论

- 已完成后端治理闭环、V1.0.18、权限资源、最小管理界面和调用链接入；
- Review 阻断已经修复：内容安全/取消分类完整，治理核心组件强制装配，管理页补齐候选启停、跳过原因、完整筛选和目录加载；
- AI 插件 84 tests 全部通过，Admin 35 模块主应用装配和前端 8487 modules 生产构建通过；
- Mapper XML、Flyway placeholder、敏感日志、固定模型名、跨模型 retry 与 whitespace 静态扫描通过；
- Spec Compliance：PASS；Code Quality：PASS；安全与依赖边界：PASS；
- 实库 Flyway、真实供应商调用和浏览器主路径属于条件/人工验证，本轮未执行并如实保留；当前状态为 `done`，已归档、未推送。

## 13. 确认记录（HARD-GATE）

- **设计决策确认时间**：2026-07-11
- **设计决策确认人**：用户（显式候选切换、最小管理界面、被动健康治理）
- **完整提案确认**：用户已于 2026-07-11 执行 `/apply ai-model-routing-governance`

## 14. 归档记录（HARD-GATE）

- **归档时间**：2026-07-11
- **归档人**：code-copilot（用户明确执行归档）
- **归档路径**：`code-copilot/changes/archive/2026-07-11-ai-model-routing-governance/`
- **知识沉淀**：`code-copilot/memory/decisions.md` 第 17 条、`code-copilot/memory/pitfalls.md` 第 105、106 条
- **归档验收结论**：Spec Compliance、Code Quality、安全与依赖审查均通过；AI 插件 84 tests、Admin 35 模块装配、Node 20 前端生产构建、Mapper XML/Flyway/安全/空白扫描已有成功证据；实库 Flyway、真实供应商和浏览器响应式/键盘主路径继续作为环境或人工条件项保留。
