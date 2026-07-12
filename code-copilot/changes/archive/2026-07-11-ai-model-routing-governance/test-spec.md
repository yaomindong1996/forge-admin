# 单测 Spec — AI 模型路由与调用治理

> status: done
> created: 2026-07-11

## 0. 测试原则

- **Red/Green TDD**：Router、HealthRegistry、InvocationRecorder 和 Resolver 每个 Task 必须先运行失败测试，再实现 Green；
- **First Run the Tests**：`/apply` 开始先复跑 `forge-plugin-ai` 已有 51 tests，确认 Java 17 和 `-Penable-tests` 真正执行；
- **一次调用铁律**：Fake ChatModel 必须统计调用次数，任何模型失败场景都断言本请求没有第二次模型调用；
- **安全数据铁律**：测试使用明显假值，断言调用日志/业务日志不包含 Prompt、响应正文、API Key、Authorization 或原始异常 message；
- **增量复用**：每轮先读取本文件和 `execution-log.md`，按自动化测试标准只补差异；
- **不产生费用**：自动测试全部使用 Fake ChatModel/Mock Adapter，不调用公网模型；
- **不污染环境**：只停止本轮启动的前端/后端服务，不清理用户已有 8580、数据库或其他进程。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit | JUnit Jupiter 5（Spring Boot 3.5.13 test starter） |
| Mock | Mockito + MockitoExtension |
| Reactor | reactor-test StepVerifier（如当前依赖树已具备） |
| 断言 | JUnit Assertions / AssertJ |
| 后端基线 | `forge-plugin-ai` 51 tests，0 failure/error/skip |
| 前端 | ESLint、Vite build、Playwright/浏览器主路径 |
| 数据库 | Mapper XML 语法 + Flyway 静态检查；有隔离 dev 库时实跑 |

## 2. 覆盖范围

### P0 — 模型选择模式

#### `AiModelSelectionModeTest`

| 场景 | 输入 | 预期 |
|------|------|------|
| 历史 Agent | null | PINNED |
| 历史空值 | blank | PINNED |
| 显式固定 | PINNED | PINNED |
| 显式策略 | POLICY | POLICY |
| 未知值 | AUTO_MAGIC | BusinessException，不能回退 PINNED |

### P0 — 确定性路由

#### `PolicyBasedAiModelRouterTest`

| 场景 | 候选/健康 | 预期 |
|------|-----------|------|
| 固定模式 | 显式 provider + model | source=REQUEST，策略 Mapper 不调用 |
| 仅显式 provider | providerId 有、modelName 空 | 使用该 provider 权威默认模型，source=REQUEST，不拼接 Agent model |
| 仅显式 model | modelName 有、providerId 空 | PINNED Agent provider 优先，否则系统默认 provider，source=REQUEST |
| POLICY Agent 仅显式 model | modelName 有、providerId 空 | 绕过 POLICY，使用系统默认 provider并校验模型归属 |
| 系统默认 provider 正常 | 当前租户恰好一条启用默认 | 返回该 provider |
| 系统默认 provider 缺失 | 当前租户零条启用默认 | BusinessException，不查其他租户 |
| 系统默认 provider 重复 | 当前租户两条启用默认 | BusinessException，不按时间静默选一条 |
| 默认 provider 停用/删除 | 默认记录 status=1 或 del_flag=1 | 视为缺失并失败 |
| Agent 固定 | Agent provider + model | source=PINNED |
| 默认模型 | Agent 只有 provider | source=PROVIDER_DEFAULT，读取 ai_model 权威默认 |
| 策略排序 | priority 20/id2、priority10/id3、priority10/id1 | 选择 priority10/id1 |
| 能力全包含 | required=[reasoning,tool_calling] | 只保留同时具备两项的候选 |
| 能力部分匹配 | 只具备 reasoning | 排除并返回 CAPABILITY_MISMATCH |
| 模型停用/删除 | status=1 或 del_flag=1 | 排除 |
| 供应商停用 | provider status=1 | 排除 |
| 策略停用/删除 | policy status=1 或 del_flag=1 | BusinessException |
| 目标停用/删除 | target status=1 或 del_flag=1 | 排除 |
| 未知所需能力 | requiredCapabilities 含 unknown | 保存策略失败，运行态失败关闭 |
| 跨租户 | target/model/provider tenant 不一致 | 排除并记录 TENANT_MISMATCH，不返回实体 |
| 第一候选 OPEN | 第二候选显式存在且 HEALTHY | 调用前选择第二候选 |
| 全部 OPEN | 所有显式候选 OPEN | BusinessException，无模型调用 |
| POLICY 无候选 | 空列表 | BusinessException，不回退供应商默认 |
| preview 遇到 HALF_OPEN | 健康快照 HALF_OPEN | 返回预览结果但不调用 tryAcquire、不消费试探令牌 |
| REQUEST/PINNED/default 遇到 OPEN | 固定目标 OPEN | 直接失败，不切换候选 |
| REQUEST/PINNED/default 遇到 HALF_OPEN | acquire=false/true | false 失败；true 允许一次调用 |
| REQUEST 显式模型归属错误 | providerPk 与 providerModelId 不匹配 | BusinessException，模型调用=0 |
| 固定来源跨租户 | provider/model 属于其他租户 | BusinessException，实体不返回 |
| 固定来源模型/供应商停用删除 | 任一状态不可用 | BusinessException，模型调用=0 |
| 重复预览 | 相同请求与健康快照 | 纯数据 RouteDecision 完全一致，不包含 Lease |

### P0 — 模型健康状态机

#### `InMemoryAiModelHealthRegistryTest`

| 场景 | 操作 | 预期 |
|------|------|------|
| 初始状态 | snapshot | UNKNOWN，可 acquire |
| 连续失败 | 3 次健康相关失败 | OPEN |
| 未到恢复窗 | OPEN 后 4 分钟 | 不可 acquire |
| 到恢复窗 | 注入 Clock 前进 5 分钟 | HALF_OPEN |
| 半开并发 | 两线程 acquire | 仅一个成功 |
| 半开成功 | lease.success | HEALTHY，失败计数归零 |
| 半开失败 | lease.failure | 重新 OPEN |
| 半开准备失败 | acquire Lease 后 Adapter/Cache 创建失败 | lease.abort，试探权释放，失败计数不增加 |
| Lease 自动关闭 | 未调用 success/failure/cancel 即 close | 默认执行幂等 abort，释放 HALF_OPEN 试探权 |
| 调用取消 | lease.cancel | 不增加失败计数 |
| 配置更新 | reset | UNKNOWN |
| 供应商配置更新 | resetProvider(tenantId, providerPk) | 只清理该租户/供应商全部 modelPk |
| 键隔离 | 同 Agent 不同 modelPk | 状态互不影响 |

### P0 — 失败分类

#### `AiModelFailureClassifierTest`

| 输入 | 预期分类 | 影响健康 |
|------|----------|----------|
| SocketTimeout/TimeoutException | TIMEOUT | 是 |
| IOException/连接失败 | NETWORK | 是 |
| HTTP 429 | RATE_LIMIT | 是 |
| HTTP 5xx | PROVIDER_5XX | 是 |
| 401/403 | AUTHENTICATION | 是 |
| model_not_found/invalid_model | MODEL_UNAVAILABLE | 是 |
| 本地 BusinessException 参数错误 | VALIDATION | 否 |
| content_filter/safety | CONTENT_POLICY | 否 |
| Reactor CANCEL | CANCELLED | 否 |
| 模型请求发出后的未知异常 | UNKNOWN | 是，记录白名单安全信息 |
| 模型请求前的未知异常 | PRE_CALL/UNKNOWN | 否，不污染模型健康 |

### P0 — 禁止失败后重试

#### `AiClientRoutingGovernanceTest`

| 场景 | Fake 行为 | 预期 |
|------|-----------|------|
| 同步成功 | 第一次返回 Usage | 调用次数=1，成功审计=1，健康成功=1 |
| 同步失败 | 第一次抛网络异常 | 调用次数=1，不选择第二候选，失败审计=1 |
| 流式成功 | 多 chunk，最后 chunk 有 Usage | 模型订阅=1，最终审计=1，Usage 取最后有效值 |
| 流式 ERROR | 中途抛异常 | 模型订阅=1，失败审计=1，健康失败=1 |
| 流式 CANCEL | 客户端取消 | 模型订阅=1，结果=CANCELLED，健康失败=0 |
| Resolver 失败 | 无候选/跨租户 | 模型调用=0，记录路由失败但不记录网络失败 |
| 第一候选预先 OPEN | 第二候选显式可用 | 只调用第二候选一次 |

### P0 — Usage、价格与调用审计

#### `AiModelInvocationRecorderTest`

| 场景 | 输入 | 预期 |
|------|------|------|
| Usage 完整 | prompt=100, completion=30,total=130 | 三字段原样记录，usageAvailable=true |
| Usage 缺失 | metadata/usage null | Token 字段统一为 null，usageAvailable=false，不按字符估算 |
| 价格快照 | 输入/输出单价 | 日志保存当次分/百万 Token 快照 |
| 路由原因 | RouteDecision 有 source/reason | route_source/route_reason 原样持久化 |
| 幂等 | 同 requestId 两次 record | 只有一条，第二次安全忽略或唯一键转换为幂等结果 |
| 安全错误 | 异常含 key/header/body | 只保存 category/httpStatus/白名单 errorCode |
| 敏感内容 | observation 带 prompt/response 测试值 | Entity/VO/日志字符串均不包含该字段 |

#### `AiModelInvocationQueryServiceTest`

| 场景 | 数据 | 预期 |
|------|------|------|
| 成本汇总 | 多条 Token + 不同价格快照 | 先累计分子再统一除以百万并 HALF_UP，返回 long 分 |
| 成本舍入 | 汇总结果为 0.49/0.50/1.50 分 | 分别返回 0/1/2 分，禁止逐调用先取整 |
| 成本溢出 | 大 Token 与大单价 | DECIMAL(38,0) 中间值不溢出，超出 long 返回安全错误 |
| Usage 缺失 | usageAvailable=false | 不计 Token/成本，缺失数单独统计 |
| 价格缺失 | Usage 存在、任一价格快照 NULL | costAvailable=false，不按免费计入成本，costUnavailableCount+1 |
| 租户隔离 | tenant1/tenant2 | 当前租户只返回自身数据 |
| P95 延迟 | N 条固定延迟 | 取排序后 1-based `ceil(0.95×N)`；空集合 NULL，单条返回自身 |

### P0 — 模型能力保存

#### `AiModelServiceTest`

| 场景 | 输入 | 预期 |
|------|------|------|
| 合法能力 | reasoning/tool_calling | 主表+关系同事务保存 |
| 重复能力 | reasoning 两次 | 去重后一条 |
| 未知能力 | invented_code | BusinessException，零写入 |
| 负数价格/窗口 | -1 | BusinessException |
| 关系写入失败 | Mapper 抛异常 | 主表回滚 |
| 模型更新 | 能力变化 | 旧关系逻辑删除、新关系插入、健康状态 reset |

### P0 — 路由策略保存

#### `AiModelRoutePolicyServiceTest`

| 场景 | 输入 | 预期 |
|------|------|------|
| 创建策略 | 合法显式候选 | 主表和候选同事务成功 |
| policyCode 重复 | 同租户未逻辑删除记录（无论启停） | BusinessException |
| 跨租户模型 | 其他 tenant modelId | BusinessException |
| 空候选 | [] | BusinessException |
| 重复候选 | 同 modelId 两次 | BusinessException，不静默去重优先级 |
| 更新候选 | 删除旧、增加新 | 单事务重建，失败回滚 |
| 删除被 Agent 使用策略 | 有 POLICY Agent | BusinessException |

### P0 — Agent 模式互斥

#### `AiAgentServiceTest`

| 模式 | 字段 | 预期 |
|------|------|------|
| PINNED | provider/model 合法 | 成功 |
| PINNED | routePolicyId 同时存在 | 保存时强制清空 routePolicyId |
| POLICY | routePolicyId 合法 | 成功 |
| POLICY | routePolicyId 空 | BusinessException |
| POLICY | 停用/跨租户策略 | BusinessException |
| null | 历史数据 | 查询 VO 显示 PINNED |

### P1 — Mapper XML 与 Flyway

- `AiModelCapabilityMapper.xml`：批量查询、status/del_flag/tenant 条件；
- `AiModelRoutePolicyMapper.xml`：分页、详情、被 Agent 引用检查；
- `AiModelRoutingQueryMapper.xml`：候选一次查询，无 N+1；
- `AiModelInvocationLogMapper.xml`：分页、汇总、时间保留删除；
- V1.0.18：字段、表、索引、生成列、字典、资源、任务防重复；
- 全迁移 `${...}` placeholder 扫描无输出；
- 有隔离 dev 库时重复执行路径和 `forge_schema_history` 验证。

Retention 额外覆盖：默认 90 天、0/负数/非数字参数拒绝、早于截止时间删除、等于截止时间保留。

### P1 — API 与权限

- 策略写接口需要 `@ApiDecrypt`，响应需要 `@ApiEncrypt`；
- policy/preview/invocation 权限资源分别验证；
- 普通租户不能通过 DTO 指定 tenantId；
- preview 只路由不调用模型、不改变健康、不产生调用费用；
- 单模型测试只提交模型数据库主键 modelPk，服务端读取供应商密钥；成功/失败只更新该 tenantId/providerPk/modelPk 健康键；
- 供应商连接测试成功和供应商配置更新后执行 provider 维度 reset，只影响对应租户/供应商；
- 调用记录 VO 不包含 prompt、response、apiKey、headers、nativeUsage。

### P2 — 前端

- 模型能力使用字典多选，不硬编码 options；
- 价格输入单位文案明确为“分/百万 Token”，禁止浮点货币输入；
- Agent PINNED/POLICY 条件字段正确显示、保存、回显；
- 路由策略候选顺序可维护且不能重复；
- preview 展示选中模型和跳过原因，不显示密钥/请求内容；
- 调用记录服务端分页，过滤器使用 pageNum/pageSize；
- 375/768/1024/1440 无页面级横向滚动；
- 浅色/暗色、键盘焦点、loading/disabled 状态可辨认。

### 不测试或条件测试

- **真实公网模型**：默认跳过，避免费用；仅环境变量提供测试 Key 时人工执行；
- **Nacos/Redis HealthRegistry**：不在本 Spec；只验证 SPI 可替换且默认实现不依赖它们；
- **Embedding/Image/Audio 路由**：本期只接 Chat；
- **真实计费出账**：成本是治理估算，不属于资金结算；
- **跨节点即时健康一致性**：默认内存实现不承诺，多实例 Provider 后续单独验证。

## 3. 执行计划

- [x] Step 1：Java 17 + `-Penable-tests` 运行现有 AI 插件 51 tests 基线；
- [ ] Step 2：Task 1–8 分别生成自动化 Red 测试，确认失败原因对应缺失能力；
- [x] Step 3：逐 Task 实现 Green，并复跑相邻回归；
- [x] Step 4：运行 AI 插件完整测试、AI/Admin package；
- [x] Step 5a：运行 Mapper XML 与 Flyway 静态检查；
- [ ] Step 5b：在隔离数据库执行 Flyway 与核心表/字典/权限验证；
- [x] Step 6a：Task 9 使用 Node 20.19.0 执行目标 ESLint 和前端 build；
- [ ] Step 6b：执行浏览器主路径、响应式和键盘交互验证；
- [x] Step 7：执行安全扫描、跨模型 retry 扫描和 `git diff --check`；
- [x] Step 8：Task 10 执行全量验证，并将命令、关键输出、警告、跳过项与服务 PID 追加到 execution-log。

## 4. 标准命令

### 后端基线与全量

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am test
```

### 定向测试

```bash
cd forge-server
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am test \
  -Dtest=PolicyBasedAiModelRouterTest,InMemoryAiModelHealthRegistryTest,AiModelFailureClassifierTest,AiClientRoutingGovernanceTest,AiModelInvocationRecorderTest,AiModelRoutePolicyServiceTest,AiAgentServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

### 主应用装配

```bash
cd forge-server
mvn -pl forge-admin-server -am package -DskipTests
```

### 静态检查

```bash
xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/*.xml
rg -n '\$\{[^}]+\}' forge-server/db/migration
git diff --check
```

### 前端

```bash
source ~/.nvm/nvm.sh
nvm use v20.19.0
cd forge-admin-ui
pnpm exec eslint src/views/ai/provider-model.vue src/views/ai/agent.vue src/views/ai/model-routing.vue src/api/ai.js
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

## 5. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-11 | Provider Adapter 归档基线 | AI 插件完整测试 | 51 tests，0 failure/error/skip | Spring AI 1.1.2、Alibaba 1.1.2.3 |
| 2026-07-11 | Admin 装配 | `forge-admin-server -am package -DskipTests` | 35 模块 SUCCESS | 既有 deprecated/unchecked 警告 |
| 2026-07-11 | 前端 | Node 20.19.0 Vite build | 8485 modules，SUCCESS | 既有组件命名/import/CSS/bundle 警告 |

## 6. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-11 | Proposal 文档 | 模板章节、路径、状态、待澄清、Spec/Task/Test 一致性、Reader Test、diff check | `rg` 契约/占位符扫描；四份文档 `git diff --no-index --check`；独立 Reader Test | PASS | no-index exit 1 仅表示文件相对 `/dev/null` 有差异，无 whitespace 错误；本阶段不编译业务代码 |
| 2026-07-11 | Review Fix | 失败分类、强制装配、管理页缺口、P0 高风险回归 | Java 17 AI 插件完整测试；Node 20.19.0 ESLint/build；Admin package；XML/安全/空白扫描 | PASS：84 tests，0 failure/error/skip；35/35 Admin；8487 modules build | 实库 Flyway、真实模型、浏览器响应式与键盘主路径仍按条件跳过 |

## 7. 执行证据

- `execution-log.md`：`code-copilot/changes/archive/2026-07-11-ai-model-routing-governance/execution-log.md`
- 关键接口：`/ai/client/call`、`/ai/client/stream`、`/ai/model`、`/ai/agent`、`/ai/model-routing/**`
- 关键表：`ai_model`、`ai_model_capability`、`ai_model_route_policy`、`ai_model_route_target`、`ai_model_invocation_log`、`ai_agent`
- 关键安全检查：Prompt/响应/API Key/headers/nativeUsage 不进入治理日志；同请求模型调用次数最多 1；
- 服务清理：只停止本轮启动的服务并记录 PID；
- 公网凭据：只从环境变量读取，不写入文档、日志、测试源码或 SQL。

## 8. 归档验收

- **状态**：done
- **归档时间**：2026-07-11
- **复用基线**：Review Fix 定向 22 tests、AI 插件完整 84 tests、Admin 35 模块 package、Node 20.19.0 ESLint 和 8487 modules 前端生产构建均已有成功证据。
- **本轮增量**：归档只修改 Spec/Tasks/Test Spec/Execution Log 和长期记忆，执行文档状态、路径、空白与目录移动检查，不重复运行无代码差异的 Maven/Vite 全量验证。
- **环境跳过**：未连接隔离数据库、未提供真实供应商验收凭据，且用户此前选择自行验证 UI；Flyway 实库、付费模型与浏览器响应式/键盘主路径继续保留为条件项。
