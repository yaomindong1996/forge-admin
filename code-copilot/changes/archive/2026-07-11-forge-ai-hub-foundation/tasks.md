# 任务拆分 — Forge AI 中枢能力底座

> 变更名：`forge-ai-hub-foundation`
> 执行原则：HARD-GATE Spike → 协议无关契约 → Registry → 静态能力 → MCP 适配 → 身份与目录验证 → UI 收敛 → 全量验证
> 每个任务先写 Red 测试，再写最小实现；禁止自动 commit/push

## 前置条件

- [x] 用户明确执行 `/apply forge-ai-hub-foundation`；
- [x] 完整读取本变更 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`；
- [x] 按 `code-copilot/rules/automated-testing-standard.md` 复用历史验证基线；
- [x] 保持 Spring AI 1.1.2、Spring AI Alibaba 1.1.2.3、Java 17 和 Spring Boot 3.5.13 基线；
- [x] 不覆盖当前工作区中用户已有的 AI 路由、供应商、前端和归档改动；
- [x] 不新增 Nacos、数据库表、高风险工具、Agent Framework 或 SSE transport。

## Review Fix 结果

- [x] 新增启动期协议硬闸门，MCP enabled 时只接受 `STREAMABLE`，拒绝 `SSE`、`STATELESS` 和 stdio；
- [x] 新增 `/mcp` 身份过滤器，身份缺失/解析失败在进入 SDK 前返回 HTTP 401，并统一传播 requestId；
- [x] 授权策略返回结构化决策，调用链可区分未认证、禁止、跨租户和跨组织错误码；
- [x] 游标绑定快照、查询条件与调用方，并使用进程内 HMAC-SHA256 签名，拒绝位置篡改、跨查询复用和不可见能力游标；
- [x] MCP Schema Projector 只接受 `ValidatedCapabilitySchema`，补齐关键字/类型组合、边界和 enum 类型一致性校验；
- [x] 增加失败关闭安全审计日志，且不记录 arguments、data、Header、Token 或 Throwable；
- [x] 补齐协议覆盖、HTTP 401、requestId 传播、未知工具、静态发布闸门及授权/游标/Schema P0 测试。

## Archive 结果

- [x] 用户明确执行归档，四份变更文档状态和验证证据一致；
- [x] AI 中枢路线图更新为阶段 0 已完成，并保留 SDK 动态 `tools/list` 限制；
- [x] Streamable HTTP、能力内核安全边界和可复用踩坑沉淀到长期记忆；
- [x] 归档目标为 `code-copilot/changes/archive/2026-07-11-forge-ai-hub-foundation/`。

## Task 1: 完成 MCP Streamable HTTP 依赖与装配 Spike（HARD-GATE）

- **目标**：在不实现能力业务的前提下，证明 WebMVC Streamable HTTP starter 能与 Forge Admin/Undertow 共存且不会装配旧 SSE。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-mcp/pom.xml` — 新建最小 MCP 插件，只引入 WebMVC MCP Server starter 与测试依赖；
  - `forge-server/forge-framework/forge-plugin-parent/pom.xml` — 临时/正式聚合新模块；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-mcp/src/test/java/com/mdframe/forge/plugin/mcp/McpStreamableTransportCompatibilityTest.java` — 最小 ApplicationContext 装配测试；
  - `code-copilot/changes/forge-ai-hub-foundation/execution-log.md` — 记录依赖树、Bean、端点和失败证据；
  - `docs/Forge-AI中枢落地架构与阶段路线图.md` — 回填经过验证的 transport ADR 结论。
- **关键断言**：
  ```java
  assertThat(context).hasSingleBean(WebMvcStreamableServerTransportProvider.class);
  assertThat(context).doesNotHaveBean(WebMvcSseServerTransportProvider.class);
  ```
- **Red/Green 步骤**：
  - [x] 先写装配测试，缺少模块/starter 时 testCompile 或 Context 启动 Red；
  - [x] 只加入 `org.springframework.ai:spring-ai-starter-mcp-server-webmvc`，显式配置 `protocol=STREAMABLE`、`stdio=false`、`mcp-endpoint=/mcp`；
  - [x] 执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-mcp -am dependency:tree -Dincludes=org.springframework.ai,io.modelcontextprotocol.sdk,org.springframework.boot`；
  - [x] 断言只存在 Streamable transport provider，不存在 SSE provider/route；
  - [x] 用测试客户端完成 initialize，验证协商协议版本和 `/mcp` 单端点；
  - [x] 运行 Admin 最小装配，确认 WebApplicationType 与 Undertow 未变化；
  - [x] 失败时停止 Task 2–8，记录升级/独立进程隔离方案，禁止切回 SSE。
- **验收标准**：HARD-GATE 六项全部有命令和断言证据；依赖树无版本冲突；旧 SSE Bean 与端点均不存在。

## Task 2: 创建 `forge-plugin-capability` 与核心契约

- **目标**：建立完全协议无关的能力定义、调用、结果、执行器和来源接口。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/pom.xml` — 新模块，只依赖 `forge-starter-core` 与必要 JSON 类型；
  - `forge-server/forge-framework/forge-plugin-parent/pom.xml` — 聚合 capability 模块；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/model/CapabilityDefinition.java` — 定义 code/toolName/version/behavior/risk/schema；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/model/CapabilityInvocation.java`、`CapabilityCallerContext.java`、`CapabilityResult.java` — 不可变调用契约；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/spi/CapabilityExecutor.java`、`CapabilitySource.java`、`CapabilityAuthorizationPolicy.java` — 执行、来源和授权 SPI。
- **关键签名**：
  ```java
  public interface CapabilityExecutor {
      boolean supports(CapabilityDefinition definition);
      CapabilityResult invoke(
          CapabilityDefinition definition,
          CapabilityInvocation invocation);
  }

  public interface CapabilitySource {
      Collection<CapabilityDefinition> load(CapabilityQuery query);
  }
  ```
- **Red/Green 步骤**：
  - [x] 编写架构 Red 测试，扫描 capability 模块不得引用 `org.springframework.ai`、`io.modelcontextprotocol`、`forge.plugin.ai`；
  - [x] 编写 record 构造校验测试：必填、不可变 scopes、duration 非负、安全 errorCode；
  - [x] 实现 `CapabilityBehavior`、`CapabilityRiskLevel`、`CapabilityResultStatus` 和稳定错误码；
  - [x] 明确 `JsonNode` 只表示 JSON 契约，不引入 MCP Schema；
  - [x] 执行 capability 模块测试 Green 和 `jdeps/rg` 禁止依赖扫描。
- **验收标准**：核心类型可被 MCP 之外的调用方独立使用；无 Spring AI/MCP/AI 插件依赖；调用结果不包含 Throwable。

## Task 3: 实现名称、Schema 校验和 MCP 公共子集投影

- **目标**：让能力在注册前拥有稳定名称和可验证的 Draft 2020-12 输入输出契约。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/naming/CapabilityToolNameMapper.java` — code 与 toolName 恒等映射及格式校验；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/schema/CapabilitySchemaValidator.java` — Draft 2020-12 公共子集校验；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/exception/CapabilityDefinitionException.java` — 带安全路径和错误码的定义异常；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/test/java/com/mdframe/forge/plugin/capability/naming/CapabilityToolNameMapperTest.java`；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/test/java/com/mdframe/forge/plugin/capability/schema/CapabilitySchemaValidatorTest.java`。
- **关键签名**：
  ```java
  public String toProtocolToolName(String capabilityCode) { }

  public ValidatedCapabilitySchema validate(
      JsonNode inputSchema,
      JsonNode outputSchema) { }
  ```
- **Red/Green 步骤**：
  - [x] Red 覆盖合法点分编码、非法字符、空分段、大写、首字符数字、129 字符和恒等稳定性；
  - [x] Red 覆盖重复 toolName 冲突，禁止截断/替换产生碰撞；
  - [x] Red 覆盖 `$schema`、required、`additionalProperties=false`、enum、nested object、array/items、nullable 联合类型；
  - [x] 对 `$ref`、递归/条件/动态关键字和无法投影的 nullable 明确失败，异常指出 JSON path；
  - [x] 在 `forge-plugin-mcp` 增加 `McpToolSchemaProjector` 时只消费 `ValidatedCapabilitySchema`，不得重新猜测语义；
  - [x] 运行名称与 Schema 测试 Green。
- **验收标准**：相同定义跨进程得到相同名称与 Schema；不支持语义不会被静默删除。

## Task 4: 实现内存 Registry、过滤与稳定游标

- **目标**：聚合静态来源形成不可变能力快照，并提供与协议无关的授权发现和调用入口。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/registry/CapabilityRegistry.java`、`InMemoryCapabilityRegistry.java`；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/registry/CapabilityRegistryFactory.java` — 启动期聚合并校验 Source/Executor；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/model/CapabilityQuery.java`、`CapabilityPage.java`、`CapabilityCursor.java`；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/registry/CapabilityInvoker.java` — 输入/输出校验、requestId 与安全结果；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/test/java/com/mdframe/forge/plugin/capability/registry/InMemoryCapabilityRegistryTest.java`。
- **关键签名**：
  ```java
  public CapabilityPage list(
      CapabilityQuery query,
      CapabilityCallerContext caller) { }

  public CapabilityResult invoke(CapabilityInvocation invocation) { }
  ```
- **Red/Green 步骤**：
  - [x] Red 覆盖重复 code+version、重复 toolName、缺 Executor、无效 Schema 启动失败；
  - [x] Red 覆盖 `canDiscover` 和 `canInvoke` 分离，未授权项不出现在内核列表且不能调用；
  - [x] Red 覆盖排序 `toolName/version`、pageSize 边界、nextCursor、非法/过期/跨快照游标；
  - [x] Red 覆盖无 caller、跨租户、跨组织和缺 scope 失败关闭；
  - [x] 使用不可变 Map/List 快照，禁止调用期间修改全局工具集合；
  - [x] 输入校验后执行、输出校验后返回；异常转安全错误，不返回原始 Throwable message；
  - [x] 运行 Registry 并发与确定性测试 Green。
- **验收标准**：相同快照与 query 得到稳定分页；发现和执行均按 caller 过滤；注册冲突不会后写覆盖。

## Task 5: 注册并验证静态 `capability.ping`

- **目标**：用无业务依赖的只读能力打通 Registry 执行闭环。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/builtin/PingCapabilitySource.java` — 提供 `capability.ping@1.0.0` 定义；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/builtin/PingCapabilityExecutor.java` — 返回固定状态、requestId、Clock 时间；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/main/java/com/mdframe/forge/plugin/capability/config/CapabilityAutoConfiguration.java` — 聚合 Source/Executor 构建 Registry；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/test/java/com/mdframe/forge/plugin/capability/builtin/PingCapabilityExecutorTest.java`；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability/src/test/java/com/mdframe/forge/plugin/capability/config/CapabilityAutoConfigurationTest.java`。
- **关键签名**：
  ```java
  public CapabilityResult invoke(
      CapabilityDefinition definition,
      CapabilityInvocation invocation) { }
  ```
- **Red/Green 步骤**：
  - [x] Red 断言输入只允许空 object，任意额外字段为 INVALID_ARGUMENT；
  - [x] Red 断言输出只含 status/requestId/serverTime，requestId 原样关联；
  - [x] 注入 `Clock`，禁止测试 sleep；
  - [x] 使用依赖/Bean 断言证明 Executor 不访问 DataSource、Redis、WebClient、ChatModel、文件系统；
  - [x] 自动配置重复定义或无 Executor 时启动失败；
  - [x] 运行 capability 模块完整测试 Green。
- **验收标准**：ping 无网络、数据库、模型和文件副作用；结果可复现且不回显客户端数据。

## Task 6: 实现 MCP 适配、可信身份与动态目录 Spike

- **目标**：通过 Streamable HTTP 暴露 ping，并如实验证当前 SDK 的身份传递、动态工具目录和分页能力。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-mcp/src/main/java/com/mdframe/forge/plugin/mcp/config/ForgeMcpServerConfiguration.java` — 仅 Streamable WebMVC 配置；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-mcp/src/main/java/com/mdframe/forge/plugin/mcp/security/McpCallerContextResolver.java`、`McpTransportContextExtractor.java` — 可信主体到 caller context；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-mcp/src/main/java/com/mdframe/forge/plugin/mcp/adapter/McpCapabilityAdapter.java`、`McpToolSchemaProjector.java`、`McpCapabilityResultMapper.java`；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-mcp/src/test/java/com/mdframe/forge/plugin/mcp/McpStreamableProtocolIntegrationTest.java`；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-mcp/src/test/java/com/mdframe/forge/plugin/mcp/security/McpCallerContextIsolationTest.java`。
- **关键签名**：
  ```java
  public interface McpCallerContextResolver {
      CapabilityCallerContext resolve(McpTransportContext context);
  }

  public McpSchema.CallToolResult call(
      McpSyncServerExchange exchange,
      McpSchema.CallToolRequest request) { }
  ```
- **Red/Green 步骤**：
  - [x] 先写 Streamable HTTP initialize/list/call Red 集成测试；
  - [x] `contextExtractor` 只携带已验证主体，不信任裸 `X-Tenant-Id/X-User-Id`；测试 Resolver 只放 test scope；
  - [x] 无 Resolver、无身份、跨租户/组织、arguments 伪造身份字段全部失败关闭；
  - [x] `tools/call` 每次重新检查 `canInvoke`，并映射安全 requestId/errorCode/isError；
  - [x] 检查 SDK 公开扩展点能否按 exchange 调用 Registry.list 并设置 nextCursor；禁止反射和全局 add/remove 竞态方案；
  - [x] 若仍是 SDK 0.17 静态列表，记录“仅 ping 可发布”的强制限制，并增加测试阻止第二个差异授权工具注册到 MCP；
  - [x] 执行禁止 SSE Bean/端点/config 扫描。
- **验收标准**：`/mcp` 完成三段协议闭环，身份不可伪造；动态目录能力与限制都有可执行证据，不虚报支持。

## Task 7: 接入 Admin 并收敛 Agent 页面预留项

- **目标**：只在 Admin 服务可控启用 MCP，并移除前端尚未实现的高风险工具假象。
- **涉及文件**：
  - `forge-server/forge-framework/forge-dependencies/pom.xml` — 管理两个 Forge 插件依赖版本；
  - `forge-server/forge-admin-server/pom.xml` — 仅 Admin 聚合 `forge-plugin-mcp`；
  - `forge-server/forge-admin-server/src/main/resources/application.yml` 与 `application-dev.example.yml` — 默认关闭并显式 STREAMABLE；
  - `forge-admin-ui/src/views/ai/agent.vue` — 隐藏硬编码高风险 MCP 选项或显示只读预留提示；
  - `forge-server/forge-admin-server/src/test/java/com/mdframe/forge/admin/ForgeMcpAdminAssemblyTest.java` — 默认禁用/显式启用装配测试。
- **实施步骤**：
  - [x] Red 断言 Admin 默认无 `/mcp`，启用但缺 Resolver 时启动失败；
  - [x] 显式启用 + test Resolver 时只出现 `/mcp`，Flow/Report/App 不因 AI 插件依赖被动出现端点；
  - [x] 配置示例不包含真实 Token、client secret 或可信 Header 旁路；
  - [x] Agent 页面不再允许保存 `database_query/http_request/file_reader/code_executor` 等未实现值；历史值只读显示“预留/不可用”，不自动丢失或映射成 ping；
  - [x] 执行目标 ESLint 与前端 build；不做额外 UI 重设计。
- **验收标准**：MCP 暴露范围只在 Admin 且默认关闭；页面不承诺不存在的工具；历史 extraConfig 不被静默改写。

## Task 8: 完成自动化验证、ADR 与 Review 准备

- **目标**：形成可复跑的阶段 0 证据，并明确是否允许进入下一阶段。
- **涉及文件**：
  - `code-copilot/changes/forge-ai-hub-foundation/spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`；
  - `docs/Forge-AI中枢战略与技术选型方案.md`；
  - `docs/Forge-AI中枢落地架构与阶段路线图.md`；
  - `code-copilot/memory/decisions.md`；
  - `code-copilot/memory/pitfalls.md`（只有产生可复用踩坑时更新）。
- **实施步骤**：
  - [x] 复跑 capability/mcp 定向测试并确认真实 Tests run；
  - [x] 运行两个新插件 `-am test/package` 和 Admin `-am package -DskipTests`；
  - [x] 输出 `dependency:tree` 并检查 Spring AI/MCP SDK/Boot 单版本；
  - [x] 扫描 `SseServerTransport`、`WebMvcSseServerTransportProvider`、`/sse`、`sse-message-endpoint`、旧 SSE starter/config；
  - [x] 扫描 Nacos MCP、AgentScope、数据库/HTTP/文件/代码执行工具和新增 Flyway，确保范围未漂移；
  - [x] 执行前端目标 lint/build；
  - [x] `git diff --check`，记录全部命令、输出摘要、警告、跳过项和服务清理；
  - [x] 对照 Spec 先做 Compliance Review，再做 Code Quality Review；
  - [x] 只有 transport、身份、Schema 和统一 ping 全部通过才允许阶段 0 标记完成；动态目录未完成时必须保留“禁止异构业务工具”的下一阶段闸门。
- **验收标准**：文档、代码和测试一致；没有 SSE/Nacos/高风险工具范围漂移；下一阶段允许项和阻断项清晰。
