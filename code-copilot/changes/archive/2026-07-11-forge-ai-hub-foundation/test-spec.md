# 单测 Spec — Forge AI 中枢能力底座

> status: done
> created: 2026-07-11

## 0. 测试原则

- **Red/Green TDD**：每个核心测试先证明在实现前失败，再实现到 Green；
- **First Run the Tests**：开始 `/apply` 前先跑 AI 插件和 Admin 当前测试基线，区分已有失败与本变更回归；
- **展示工作**：所有命令、Tests run、失败摘要和修复后输出写入 `execution-log.md`，禁止只写“已通过”；
- **增量复用**：按 `code-copilot/rules/automated-testing-standard.md` 复用本变更四份文档与已归档 AI 变更基线；
- **失败关闭**：transport、身份、Schema 或依赖版本闸门失败时停止后续实现，不改用 SSE；
- **零外部成本**：测试不访问公网、真实 Nacos、真实数据库/Redis，不调用 ChatModel，不产生 Token 费用；
- **确定性**：时间使用注入 Clock，分页使用稳定快照，不通过 sleep 或真实时间窗口制造结果。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit Jupiter 5（由 `spring-boot-starter-test` 管理） |
| Mock 框架 | Mockito + AssertJ + Spring Boot Test/ApplicationContextRunner |
| 协议测试 | Spring MockMvc/WebTestClient 或 MCP SDK 客户端，最终由 Task 1 选择与 WebMVC transport 匹配的最小工具 |
| 架构/静态检查 | Maven dependency tree、ApplicationContext Bean 断言、`rg`、必要时 ArchUnit/jdeps |
| 已有测试数量 | AI 插件 84；本变更新增 Capability 9、MCP 8（均为最终实际执行数） |
| 已有测试风格 | JUnit 5 + Mockito，AI 插件已有 Service/Client/路由治理单测 |

## 2. 覆盖范围

### P0 — transport 与协议硬闸门

| 场景 | 输入/配置 | 预期结果 |
|------|-----------|----------|
| Streamable 装配 | `protocol=STREAMABLE`, WebMVC starter | 只有 `WebMvcStreamableServerTransportProvider` |
| 默认值防护 | 不显式提供 protocol | 配置约束测试失败，禁止依赖默认 SSE |
| initialize | 认证上下文 + `/mcp` | 协商 SDK 支持协议并返回 `forge-ai-hub` server info |
| tools/list | 初始化后的有效会话 | 只返回 `capability.ping`，名称和 Schema 正确 |
| tools/call | `capability.ping`, `{}` | success，requestId 关联，status=ok |
| 非法输入 | ping 传额外字段 | INVALID_ARGUMENT/isError，不执行 Executor 副作用 |
| 未知工具 | 未注册 toolName | 安全 NOT_FOUND，不泄露内部列表或类名 |
| 无 SSE transport | 完整 Context | 无 SSE provider/route/config Bean，无 `/sse` 双端点 |
| 版本失败关闭 | 版本冲突或无 Streamable provider | 构建/启动失败，不回退 SSE |
| 容器共存 | Admin + Undertow + WebMVC + 现有 WebFlux 依赖 | 主应用类型不变，现有流式接口装配不被覆盖 |

### P0 — 能力编码与注册

| 方法/组件 | 场景 | 输入 | 预期结果 |
|-----------|------|------|----------|
| `CapabilityToolNameMapper` | 合法 | `capability.ping` | 恒等映射且多次结果相同 |
| 同上 | 非法字符/大写/空分段/数字开头 | 多组非法 code | 定义异常，带稳定错误码 |
| 同上 | 超长 | 129 字符 | 拒绝，不截断、不哈希 |
| Registry | 重复键 | 相同 code+version 两次 | 启动失败，不覆盖 |
| Registry | 名称冲突 | 不同定义同 toolName | 启动失败 |
| Registry | 缺 Executor | 定义存在、无 supports | 启动失败 |
| Registry | 多 Source 顺序变化 | 相同定义集合不同加载顺序 | 最终快照和列表顺序一致 |

### P0 — JSON Schema Draft 2020-12

| 场景 | Schema 特征 | 预期结果 |
|------|-------------|----------|
| 必填字段 | `required` | 缺字段拒绝 |
| 封闭对象 | `additionalProperties=false` | 额外字段拒绝 |
| enum | 合法/非法值 | 合法通过、非法拒绝 |
| 嵌套对象 | object 内 object | 路径和校验正确 |
| 数组 | array/items | 每个元素校验 |
| nullable | `type:[string,null]` | 公共投影支持则保留；不支持则注册失败，不删 null 语义 |
| 不支持关键字 | `$ref`/条件/动态关键字 | SCHEMA_UNSUPPORTED，报告安全 JSON path |
| 输出 Schema | Executor 返回非法结构 | INTERNAL/OUTPUT_SCHEMA_INVALID，不把非法 data 发给客户端 |
| 敏感数据 | 非法 arguments 含 secret | 日志不打印完整参数和值 |

### P0 — 身份、租户与授权

| 场景 | 输入 | 预期结果 |
|------|------|----------|
| 缺身份 | 无可信 principal | 401/UNAUTHENTICATED，Registry 不执行 |
| 缺 Resolver | MCP enabled，生产 Context 无 Resolver | 启动失败或端点保持禁用 |
| 裸 Header 伪造 | `X-Tenant-Id/X-User-Id` 无已验证主体 | 不构造 caller，失败关闭 |
| arguments 伪造 | tenantId/userId/activeOrgId/machineClientId | 因封闭 Schema 拒绝，caller 不变 |
| 跨租户 | caller tenant 与定义授权不匹配 | 不可发现、不可调用 |
| 跨组织 | activeOrgId 不允许 | 不可发现、不可调用 |
| scope 不足 | discover 可见、invoke 不允许 | list 可按策略处理，call 必须 FORBIDDEN |
| 安全日志 | 任意失败 | 仅 requestId、安全主体标识、code/errorCode/耗时，无 Token/Header/arguments/data |

### P0 — 动态目录与稳定分页

Capability 内核必须完整覆盖：

| 场景 | 预期结果 |
|------|----------|
| 不同 caller | 只返回各自 `canDiscover` 的定义 |
| pageSize=1 | 返回一项和稳定 nextCursor |
| 使用 nextCursor | 无重复、无遗漏，顺序稳定 |
| 非法/过期游标 | 失败关闭，不从头静默重放 |
| 注册顺序不同 | 同一快照的分页结果一致 |

MCP 端到端按客户端动态裁剪与 nextCursor 是 Spike 断言：

- 若 SDK/公开扩展点支持，则上述场景必须做端到端 P0；
- 若当前 SDK 仍静态全量且 `nextCursor=null`，必须有失败关闭测试阻止注册第二个差异授权工具，并把“仅统一授权 ping”写入执行日志；
- 不允许通过反射或请求期间修改全局工具列表使测试表面通过。

### P0 — 静态 ping 无副作用

| 检查 | 预期结果 |
|------|----------|
| DataSource/Mapper/Redis | Executor 无依赖、无调用 |
| WebClient/HTTP | 无依赖、无网络 |
| ChatModel/ChatClient | 无依赖、无模型费用 |
| 文件/进程 | 无读写、无 Shell |
| 用户回显 | 不接受任意文本，不把客户端内容带回响应 |
| 时间 | 使用 Clock，可确定性断言 |

### P1 — Maven 与应用装配

- `forge-plugin-capability` 编译依赖不出现 Spring AI、MCP SDK、`forge-plugin-ai`；
- `forge-plugin-mcp` 只依赖 capability，不反向形成循环；
- `spring-ai-starter-mcp-server-webmvc:1.1.2` 与 `mcp-spring-webmvc:0.17.0` 仅出现一个解析版本；
- Spring Boot 依赖最终由 Forge BOM/父工程约束为项目兼容版本，不出现 3.5.8/3.5.13 混用导致的运行时冲突；
- Admin 默认关闭 MCP；显式启用且身份 Resolver 齐备时出现 `/mcp`；
- Flow/Report/App 不因为依赖 `forge-plugin-ai` 而出现 MCP endpoint；
- 原有 AI 插件、路由治理和 Admin package 回归通过。

### P2 — 前端与配置

- Agent 页面不再允许选择或新保存未实现的 database/http/file/code 工具；
- 历史 `extraConfig.mcpTools` 值只读标记预留，不被页面加载/保存静默删除；
- MCP 配置默认 `enabled=false`，显式 `protocol=STREAMABLE`；
- 示例配置不包含密钥；
- 目标 ESLint 和生产 build 通过，无额外页面重设计。

### 不测试（明确列出原因）

- Nacos MCP Registry/Admin/Gateway：不在本变更范围；
- 正式 OAuth2、Client Credentials、API Key 生命周期：阶段 0 只定义 Resolver 契约；
- 数据库能力目录和 Flyway：本变更无数据变更；
- 数据库查询、HTTP、文件、代码执行、流程、消息工具：明确禁止实现；
- 模型 Tool Calling 与 Agent Runtime：后续独立变更；
- 公网 MCP Inspector/外部 SaaS：自动化测试必须离线且可复跑；
- 旧 SSE 客户端兼容：产品决策明确禁止。

## 3. 执行计划

- [x] Step 1: 读取四份变更文档与自动化测试标准，运行当前 AI/Admin 基线；
- [x] Step 2: 完成 Task 1 transport 装配 Red/Green，HARD-GATE 未通过即停止；
- [x] Step 3: 名称、Schema、Registry、授权、ping 按 P0 逐组 Red/Green；
- [x] Step 4: 完成 Streamable HTTP initialize/list/call 和身份隔离集成测试；
- [x] Step 5: 完成动态目录 SDK Spike，按“支持或限制”两条分支生成真实证据；
- [x] Step 6: 执行 Maven 依赖树、无 SSE/无 Nacos/无高风险工具扫描；
- [x] Step 7: 执行 Admin 装配、AI 回归、前端 lint/build；
- [x] Step 8: `git diff --check`，回填命令、警告、跳过项、服务清理和 Review 结论。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-11 | Proposal 文档静态检查 | 对四份未跟踪文档逐一执行 `git diff --no-index --check /dev/null <file>` 并检查末尾换行/尾随空格 | 通过 | 不代表业务代码测试 |
| 2026-07-11 | Maven 坐标研究 | `mvn dependency:get -Dartifact=org.springframework.ai:spring-ai-starter-mcp-server-webmvc:1.1.2` 等三个坐标 | 三种 server starter 均可解析 | 只写本地 Maven 缓存 |
| 2026-07-11 | MCP SDK 结构研究 | `javap/jar tf` | WebMVC Streamable provider 存在；SDK tools/list 静态且 cursor 为 null | 不是 Forge Runtime 装配测试 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| Proposal | 仅四份变更文档 | 文件存在、状态、范围、SSE 禁令、路径、diff check | 见 `execution-log.md` | 通过 | 不编译、不启动服务 |
| 2026-07-11 Apply Task 1 | MCP transport Spike | Context、initialize、dependency tree、无 SSE | `mvn -Penable-tests -pl .../forge-plugin-mcp -am test`、`dependency:tree` | 通过：MCP 8、Capability 9，0 失败；协议 `2025-06-18` | 上游无配置默认 SSE，Forge Admin 必须保留显式 `enabled=false` 与 `protocol=STREAMABLE` |
| 2026-07-11 Apply Task 2–8 | Capability/MCP 实现 | P0/P1/P2 与回归 | AI 84 测试、Admin 37 模块 package、目标 ESLint、`pnpm build`、静态扫描、`git diff --check` | 全部通过 | 前端存在仓库既有构建警告；未启动真实服务，未访问外部系统 |
| 2026-07-11 Review Fix | 协议、身份、授权、游标、Schema、审计与 P0 回归 | Java 17 Capability/MCP 完整测试、AI 回归、Admin 聚合、静态安全扫描、diff check | `mvn -Penable-tests -pl .../forge-plugin-mcp -am test`；`mvn -Penable-tests -pl .../forge-plugin-ai test`；`mvn -pl forge-admin-server -am package -DskipTests` | PASS：Capability 13、MCP 13、AI 84，均 0 failure/error/skip；Admin 37/37 SUCCESS | 前端未修改，复用 Apply 阶段目标 ESLint 与生产 build 成功基线；未启动真实服务 |

## 6. 执行证据要求

- `execution-log.md`：必须包含每次 Red/Green 的命令、Tests run、失败原因和修复后输出；
- 关键协议：保存 initialize、tools/list、tools/call 的安全响应摘要，不记录 Authorization；
- 关键配置：保存 protocol/endpoint/Bean 断言和 dependency tree 摘要；
- 关键数据库检查：明确“无数据库/Flyway 变更”，并扫描新迁移文件；
- 服务启动与停止：如启动 Admin，记录 profile、PID/端口、健康结果和停止确认；
- 外部调用：必须为 0；任何公网、Nacos、模型调用都视为测试范围违规。
