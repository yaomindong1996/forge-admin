# 变更日志 — AI 模型路由与调用治理

> 本文件持续记录提案、实现、验证和审查证据。变更已于 2026-07-11 归档。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-07-11 | Research | 读取根 AGENTS、code-copilot 规则、相关知识、战略方案和已归档 Provider Adapter Spec | 以实际代码和 Spring AI 1.1.2 本地发布 Jar 为事实来源 |
| 2026-07-11 | Research | 检查 AiModel、AiAgent、AiInvocationResolver、AiClientImpl、CircuitBreaker、模型/Agent 管理页面 | 确认当前只有固定优先级，无能力目录、策略路由和完整调用审计 |
| 2026-07-11 | Research | 核对 Spring AI 1.1.2 ChatResponse/Metadata/Usage API | `CallResponseSpec#chatResponse`、`ChatResponseMetadata#getUsage` 可提供统一 Token 数据 |
| 2026-07-11 | Decision | 用户选择显式候选切换 | 调用前跳过已知 OPEN 候选；调用发出后禁止自动重试 |
| 2026-07-11 | Decision | 用户选择后端闭环 + 最小管理界面 | 不建设治理大屏，交付模型能力、策略、Agent 绑定、调用记录 |
| 2026-07-11 | Decision | 用户选择真实调用被动统计 + 手动测试 | 默认不执行定时付费探测，预留 HealthRegistry/Probe 扩展点 |
| 2026-07-11 | Proposal | 创建 spec.md、tasks.md、test-spec.md、execution-log.md | 等待静态自审和 Reader Test |
| 2026-07-11 | Reader Test 1 | 未通过：显式 provider/model 决策表和调用观察契约存在阻断 | 补齐四种显式组合、UNKNOWN dispatched 边界、Observation 可空性与安全字段 |
| 2026-07-11 | Reader Test 2 | 未通过：默认供应商和 HALF_OPEN 试探权契约存在阻断 | 固定 `requireEnabledDefaultProvider` 恰好一条契约，引入幂等 `AiModelHealthLease` |
| 2026-07-11 | Reader Test 3 | 未通过：路由原因未落审计，RouteDecision 与 Lease 耦合 | 增加 source/reason 与 `route_source/route_reason`；拆分纯数据 RouteDecision 和 RoutedInvocation |
| 2026-07-11 | Reader Test 4 | PASS | 独立读者确认最新四份文档无阻断级歧义、矛盾或占位符 |
| 2026-07-11 | Proposal Validation | 完成静态一致性与空白检查 | P95 统一为 nearest-rank；四份文档 whitespace check 无错误；占位符扫描无输出 |
| 2026-07-11 | Apply | 完成模型能力、显式策略路由、模型级健康状态、调用审计、Agent 模式与最小管理端 | 未引入 Nacos/MCP/Agent Framework，未增加调用失败后换模型 |
| 2026-07-11 | Apply Fix | 修复策略分页 N+1、策略候选 tenantId 丢失、HALF_OPEN 准备阶段 Lease 泄漏与流式异步审计上下文 | 增加跨租户候选回归测试；删除旧 agentCode CircuitBreaker |
| 2026-07-11 | Apply Validation | AI 插件 63 tests、Admin package、前端 ESLint/build、XML/SQL/安全扫描通过 | 实库 Flyway、真实模型和浏览器主路径按条件跳过，状态进入 review |
| 2026-07-11 | Review Fix | 修复内容安全/取消分类、治理组件可选装配、管理页功能缺口和 P0 测试缺失 | Resolver 只走 Router；核心依赖强制构造器注入；页面补齐完整目录、候选状态、跳过原因和筛选权限 |
| 2026-07-11 | Review Fix Validation | AI 插件 84 tests、Admin 35 modules、前端 ESLint/build、XML/安全/空白扫描通过 | 不调用公网模型；实库 Flyway 与浏览器主路径继续保留为人工条件项 |
| 2026-07-11 | `/archive` 归档 | 核对四份变更文档、长期记忆、Review Fix 结论、验证证据和跳过项 | 状态更新为 done；知识已沉淀到 decisions 17、pitfalls 105/106；条件验证项如实保留，未推送 |

## Research 证据

| 结论 | 代码出处 | 影响 |
|------|----------|------|
| 模型解析只有请求、Agent、供应商默认三级 | `client/AiInvocationResolver.java#resolve/#resolveModel` | 路由需要独立 Router，不应继续扩张 Resolver 条件分支 |
| 模型没有能力和价格字段 | `model/domain/AiModel.java` | 新增能力关系表，主表只加数值治理字段 |
| Agent 只能固定 provider/model | `agent/domain/AiAgent.java`、`views/ai/agent.vue` | 增加 PINNED/POLICY 模式，历史 null 兼容 PINNED |
| 熔断按 agentCode 且只在本机内存 | `client/CircuitBreaker.java`、`AiClientImpl#call/#stream` | 改为 tenantId/providerPk/modelPk HealthRegistry SPI |
| 同步调用只取 content | `AiClientImpl#call` | 改取 ChatResponse 才能得到 Usage |
| 流式已消费 ChatResponse 但未聚合 Usage | `AiClientImpl#stream` | 捕获最后一个有效 Usage，保证只审计一次 |
| 聊天记录不足以承载治理审计 | `chat/domain/AiChatRecord.java` | 新建 append-only 调用日志，不把业务消息表继续扩张 |
| 当前 INFO 日志打印 Prompt/响应内容 | `AiClientImpl#call/#stream` | 本变更删除内容日志，只保留长度和 requestId |
| Provider Adapter 已统一模型创建 | `provider/adapter/AiProviderAdapterRegistry.java#createChatModel` | Router 只选择目标，不侵入 Adapter |
| 战略路线要求 Nacos/MCP 分阶段 | `docs/Forge-AI中枢战略与技术选型方案.md`、`memory/decisions.md#16` | 本变更不引入 Nacos、MCP 或 Agent Framework |

## 技术决策

| 决策 | 选择 | 放弃方案 | 原因 |
|------|------|----------|------|
| 路由范围 | 策略显式候选 | 扫描所有启用模型 | 防止不可解释跨供应商 |
| 路由算法 | 能力全包含 + priority/id 排序 | 权重随机/动态评分 | 确定性、可复跑 |
| 失败后处理 | 不重试 | 请求失败后换模型 | 避免重复计费和 Tool 副作用 |
| 健康实现 | SPI + 默认内存状态机 | 当前直接接 Redis/Nacos | 控制当前依赖并保留替换点 |
| 健康采集 | 被动调用 + 手动测试 | 定时付费探测 | 无额外费用 |
| 能力存储 | 关系表 + 字典代码 | boolean/单 JSON | 新能力可扩展且可查询 |
| Token | Spring AI Usage | 字符数估算 | 不伪造精度 |
| 成本 | 分/百万 Token + 价格快照 | double/小数货币字段 | 遵循 Forge 金额规则 |
| 管理端 | 最小配置和调用记录 | 完整可观测大屏 | YAGNI，不延误后续 MCP |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
|------|------|----------|--------|
| 失败后切换候选容易被误称为普通重试 | LLM 调用可能计费且 Tool 调用可能有副作用 | 只允许调用前跳过已知 OPEN；请求发出后禁止切换 | 待实现验证后沉淀 |
| 用 AiChatRecord 兼做治理日志会混淆消息与调用 | 一次调用包含两条消息且缺少路由/错误元数据 | 独立 invocation log | 待实现验证后沉淀 |
| 字符数不能替代供应商 Token Usage | 编码和 tokenizer 不同 | Usage 缺失时明确标记不可用 | 待实现验证后沉淀 |
| agentCode 熔断不能表达模型健康 | 一个 Agent 可换模型、多个 Agent 可共享模型 | 健康键改为 tenantId/providerPk/modelPk | 待实现验证后沉淀 |

## 知识发现

- [x] **确定性模型路由边界**：调用前健康跳过与调用后自动重试必须区分，后者会带来费用和副作用风险。
- [x] **模型能力目录**：能力代码应独立于供应商品牌和 Adapter，供 Agent/MCP/路由共同消费。
- [x] **调用治理数据**：聊天内容、框架 Micrometer 指标和租户调用审计是三种不同数据，不应复用一张表。
- [x] **控制面可替换性**：Router/Health SPI 是以后接 Redis/Nacos 的稳定接缝，业务调用不应读取注册中心 SDK。

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|-----------|----------|----------|
| 自动化场景覆盖 | Test Spec 计划完整 P0 场景 | Review Fix 已补齐点名缺失的四个测试类及失败分类、OPEN 跳过、默认供应商、健康隔离等高风险边界；仍未逐条穷举 Test Spec 全矩阵 | 已覆盖本轮阻断，未执行项继续保持未勾选，不把条件项标记为已验证 |
| 条件验证 | Flyway 实库与浏览器响应式主路径 | 本轮未启动数据库、后端或前端服务 | 保留为 review/人工验收项，不阻断代码进入审查 |

## 验证记录

| 时间 | 变更范围 | 命令 | 结果 | 警告/跳过 | 服务清理 |
|------|----------|------|------|-----------|----------|
| 2026-07-11 | Research | `rg`/`sed` 检查 AI 插件、管理端、战略文档；`javap` 检查 Spring AI 1.1.2 ChatResponse/Usage | 已确认现状与可用 API | 本阶段不运行 Maven/前端构建，不调用模型 | 无服务启动 |
| 2026-07-11 | Proposal 静态校验 | `git diff --no-index --check /dev/null <spec/tasks/test-spec/execution-log>` | 四个文件均无 whitespace 错误输出；exit 1 仅表示 no-index 存在预期差异 | 本轮仅文档，不运行 Maven、前端构建或公网模型测试 | 无服务启动 |
| 2026-07-11 | Proposal 占位符/契约扫描 | `rg -n -e 'T[B]D' -e 'TO[D]O' -e '待[补]充' -e '稍后[实]现' -e 'fill[ ]in' -e 'implement[ ]later' -e '归一化或明确[拒]绝' -e 'null[/]0' -e '近似[ ]P95' code-copilot/changes/ai-model-routing-governance`；RouteDecision/RoutedInvocation/routeReason/Retention/P95 交叉扫描 | 禁止占位符无输出；关键契约在 Spec/Tasks/Test Spec 命名一致 | 未执行业务测试，因为 `/apply` 尚未开始且无业务代码变更 | 无服务启动 |
| 2026-07-11 | 最终 Reader Test | 独立无上下文读者复核最新 spec.md、tasks.md、test-spec.md、execution-log.md | PASS | HARD-GATE 仍等待用户通过 `/apply ai-model-routing-governance` 明确进入编码 | 无服务启动 |
| 2026-07-11 | Apply 前基线 | Java 17 执行 `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am test` | 51 tests，0 failure/error/skip，BUILD SUCCESS | 不调用公网模型 | 无服务启动 |
| 2026-07-11 | Apply 后 AI 插件回归 | 同一 Java 17 命令复跑 AI 插件及依赖测试 | 最终 63 tests，0 failure/error/skip，BUILD SUCCESS；基线 51 tests 全部保留 | Spring JCL/Commons Logging 提示为既有非阻断警告 | 无服务启动 |
| 2026-07-11 | 主应用装配 | `mvn -pl forge-admin-server -am package -DskipTests` | 35/35 reactor modules SUCCESS，生成 `forge-admin-server.jar` | generator/admin 既有 deprecated/unchecked 警告；本命令按设计跳过测试 | 无服务启动 |
| 2026-07-11 | 前端静态与生产构建 | Node v20.19.0 执行目标 ESLint `--fix`；`pnpm build` | ESLint 通过；8487 modules transformed，约 1m40s 构建成功 | 既有 UserSelectModal 命名、动静态 import、CSS `//`、bundle size 警告，不阻断 | 无服务启动 |
| 2026-07-11 | XML/Flyway 静态检查 | `xmllint --noout .../mapper/*.xml`；`rg -n '\$\{[^}]+\}' forge-server/db/migration`；V1.0.18 tenantId/版本扫描 | XML 解析通过；placeholder 与 tenantId=0 扫描无输出；V1.0.18 版本唯一 | 未连接隔离 MySQL，未写 `forge_schema_history` | 无服务启动 |
| 2026-07-11 | 安全与重试扫描 | 扫描 `gpt-3.5-turbo`、Prompt/响应正文日志、API Key/Header/nativeUsage、retry/CircuitBreaker；`git diff --check` 与行尾空白扫描 | 禁止项无输出；旧 agentCode CircuitBreaker 已删除；空白检查通过 | 治理实体仍保存 adapterCode/providerModelId 等允许的非敏感元数据 | 无服务启动 |
| 2026-07-11 | 条件项 | 未执行 Flyway 实库、真实供应商模型测试、浏览器响应式/键盘主路径 | 明确跳过 | 避免污染用户数据库、产生模型费用；用户此前要求 UI 自测 | 无服务启动 |
| 2026-07-11 | Review Fix 定向回归 | `AiModelFailureClassifierTest,AiInvocationResolverTest,AiClientRoutingGovernanceTest,AiModelRoutePolicyServiceTest,AiAgentServiceTest,AiInvocationLogRetentionJobTest,PolicyBasedAiModelRouterTest` | 22 tests，0 failure/error/skip，BUILD SUCCESS | Fake/Mock ChatModel，不访问公网、不产生费用 | 无服务启动 |
| 2026-07-11 | Review Fix AI 插件完整回归 | Java 17 执行 `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am test` | 84 tests，0 failure/error/skip，BUILD SUCCESS | 流式错误测试会输出预期异常栈；Spring JCL/Commons Logging 为既有提示 | 无服务启动 |
| 2026-07-11 | Review Fix 主应用装配 | `mvn -pl forge-admin-server -am package -DskipTests` | 35/35 reactor modules SUCCESS，生成可执行 Jar | generator/admin 既有 deprecated/unchecked 警告；测试按命令跳过 | 无服务启动 |
| 2026-07-11 | Review Fix 静态检查 | 目标 ESLint `--fix`；`xmllint --noout`；扫描 optional injection、`gpt-3.5-turbo`、Flyway placeholder；tracked/untracked whitespace check | 全部通过，禁止项无输出 | 未启动浏览器与数据库 | 无服务启动 |
| 2026-07-11 | Review Fix 前端最终回归 | Node v20.19.0 执行目标 ESLint `--fix`；`NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | ESLint 通过；8487 modules transformed；生产构建约 1m38s 成功 | 既有组件命名、动静态 import、CSS `//` 和 bundle size 警告，不阻断 | 无服务启动 |

## 代码质量备忘

- Router 不得循环查询候选模型、供应商和能力；使用 Mapper XML 批量/关联查询；
- Service 不得互相注入形成循环依赖；协调逻辑使用单向依赖或 Manager；
- RouteDecision 必须携带 source/policy/reason，不能只返回模型字符串；
- 健康状态机使用 Clock 测试，禁止 sleep；
- 同步/流式必须保证一次模型调用、一次最终审计；
- 调用日志和普通日志均禁止 Prompt、响应正文、密钥、Header、nativeUsage；
- Nacos、MCP、Agent Framework 不得因“预留扩展点”被顺带引入。

## Review 结论

- Spec Compliance：PASS；
- Code Quality：PASS，Review 阻断修复后 AI 插件 84 tests 全部通过；
- 安全与依赖：PASS，无 Prompt/响应/API Key 日志，无跨模型失败后补发，治理核心组件不再可选装配；
- 保留警告：JVM CDS、commons-logging、既有 deprecated/unchecked Java 警告和既有 Vite 组件/import/CSS/bundle 警告均不阻断；
- 保留跳过项：Flyway 实库、真实供应商模型、浏览器响应式与键盘主路径未执行；
- 服务清理：归档轮次未启动服务；
- 当前状态：done，已归档、未推送。
