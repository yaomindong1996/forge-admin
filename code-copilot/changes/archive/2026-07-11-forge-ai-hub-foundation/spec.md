# Forge AI 中枢能力底座

> status: done
> created: 2026-07-11
> complexity: 🔴复杂

## 1. 背景与目标

`spring-ai-alibaba-provider-adapter` 与 `ai-model-routing-governance` 已完成模型供应商适配、默认模型权威来源、确定性路由、模型健康和安全调用审计。当前工程仍缺少一个独立于模型供应商、Agent 框架和对外协议的“能力内核”：业务能力没有稳定编码、输入输出 Schema、风险等级、统一结果与调用身份契约，也没有 MCP Server。

本变更建设 Forge AI 中枢阶段 0 基础设施，目标不是直接开放数据库、HTTP、文件或代码执行，而是先建立可以安全演进的两层边界：

1. 新建协议无关的 `forge-plugin-capability`，定义能力、注册、发现、鉴权和执行契约；
2. 新建 `forge-plugin-mcp`，只把能力目录投影为 MCP Tool，并通过最新 Streamable HTTP 暴露；
3. 以进程内 Registry 和静态只读 `capability.ping` 打通 `initialize → tools/list → tools/call`；
4. 固化 JSON Schema、稳定名称映射、requestId、机器身份、租户和组织边界；
5. 验证 Spring AI MCP Server 与 Forge 的 Undertow/WebMVC/WebFlux 依赖共存；
6. 明确当前 SDK 对动态 `tools/list` 和游标分页的真实能力，不能支持时失败关闭，不通过回退旧 SSE 换取演示效果；
7. 收敛 Agent 页面当前硬编码但尚未实现的高风险“MCP 工具”选项，避免形成虚假配置。

### 1.1 成功标准

- `forge-plugin-capability` 不依赖 Spring AI、Spring AI Alibaba、MCP SDK、`forge-plugin-ai` 或具体 Web transport；
- `forge-plugin-mcp -> forge-plugin-capability` 是单向依赖，业务能力不能反向依赖 MCP 类型；
- MCP Server 只注册 Streamable HTTP `/mcp` 单端点，不生成旧 `/sse` 与消息回传双端点；
- 使用显式配置 `spring.ai.mcp.server.protocol=STREAMABLE`，不能依赖该版本默认值；
- 客户端通过 Streamable HTTP 完成 `initialize`、`tools/list` 和 `tools/call(capability.ping)`；
- `capability.ping` 只返回 requestId、服务时间和固定状态，不读取业务数据库、Redis、文件、外部 URL 或模型服务；
- 能力编码、协议工具名、版本、Schema、行为和风险等级在启动期校验，重复或冲突时启动失败；
- 调用身份只能从经过验证的传输上下文解析，arguments 中伪造的 tenantId、userId、activeOrgId、machineClientId 不得改变调用上下文；
- 没有可信 `McpCallerContextResolver` 时生产 MCP 端点默认禁用或启动失败，不能匿名降级；
- MCP 依赖树只存在一个受 BOM 管理的 Spring AI/MCP SDK 版本组合；
- 当前 SDK 若不能按客户端过滤 `tools/list` 和稳定分页，只允许受控环境暴露统一授权的静态 `capability.ping`；任何异构或业务工具上线前必须完成 SDK 升级/隔离适配，禁止静默返回全量目录；
- 后端定向测试、Admin 装配测试、依赖树检查和禁止旧 SSE 扫描通过；测试不访问公网、不调用收费模型。

## 2. 代码现状（Research Findings）

### 2.1 当前 AI 链路

1. `forge-plugin-ai/client/AiClient.java` 与 `AiClientImpl` 已提供统一模型调用入口；模型选择由 `AiInvocationResolver` 和路由治理完成，但不存在 Tool/Function Callback 注册链。
2. `forge-plugin-ai/provider/adapter` 已隔离 `openai_compatible` 与 `dashscope_native`，这解决的是模型供应商差异，不应承担业务能力目录或 MCP 协议职责。
3. `forge-plugin-ai/pom.xml` 已引入 `spring-boot-starter-webflux` 以支持现有流式响应；Admin 主应用仍是 Undertow/WebMVC 部署，因此 MCP Server transport 不能只根据 WebFlux 依赖存在就选择 WebFlux。
4. `forge-admin-server/pom.xml` 当前只聚合 `forge-plugin-ai`，没有 Capability/MCP 插件。

### 2.2 当前 MCP 与前端事实

1. 仓库中没有 MCP Server、`ToolCallback`/Function Calling 注册链、Capability Registry、Nacos MCP Registry/Admin、AgentScope 或 Agent Framework。
2. `forge-admin-ui/src/views/ai/agent.vue` 的 `mcpToolOptions` 硬编码了“数据库查询、HTTP 请求、文件读取、网页搜索、代码执行”，但后端没有相应实现、权限、Schema 或安全沙箱；这些选项当前只是无效且高风险的配置外观。
3. `docs/vendor/spring-ai-extensions-1.1.2.3-release` 同时包含 Streamable HTTP 与旧 SSE 客户端/注册相关源码。存在旧实现不代表 Forge 可以同时启用两种 transport，本变更只允许选择 streamable 相关类和配置。

### 2.3 本地依赖证据

2026-07-11 通过 Maven Central 坐标和本地 JAR/POM 检查确认：

| 证据 | 结论 |
|------|------|
| `org.springframework.ai:spring-ai-starter-mcp-server:1.1.2` | 通用 MCP Server 能力，不包含 Web transport |
| `org.springframework.ai:spring-ai-starter-mcp-server-webmvc:1.1.2` | 包含 WebMVC 自动配置与 `mcp-spring-webmvc:0.17.0`，是 Forge 当前主容器的首选候选 |
| `org.springframework.ai:spring-ai-starter-mcp-server-webflux:1.1.2` | 可解析，但不是仅因 AI 插件已有 WebFlux 依赖就默认选择 |
| `McpServerProperties.ServerProtocol` | 支持 `SSE/STREAMABLE/STATELESS`，元数据默认值是 `SSE` |
| `McpServerStreamableHttpProperties.CONFIG_PREFIX` | `spring.ai.mcp.server.streamable-http`，默认端点 `/mcp` |
| `McpServerStreamableHttpWebMvcAutoConfiguration` | 创建 `WebMvcStreamableServerTransportProvider` 和 WebMVC RouterFunction |
| `WebMvcStreamableServerTransportProvider.Builder#contextExtractor` | 可以从传输请求抽取可信上下文，适合作为机器身份传递入口 |
| MCP SDK `0.17.0` 的 `McpAsyncServer#toolsListRequestHandler` | 从进程内工具列表静态返回全部 Tool，并固定 `nextCursor=null`；当前公共自动配置不能直接实现按请求动态过滤与分页 |

### 2.4 发现与风险

- 如果直接把 Controller 方法或 MyBatis Mapper 包装成 MCP Tool，协议、权限和业务实现会紧耦合，后续内部 Agent Tool Calling、HTTP API 或工作流复用都要复制逻辑；
- Spring AI 1.1.2 的 MCP Server 默认协议仍是 SSE，不显式配置会违反用户确定的 transport 决策；
- 当前 SDK 可把 transport context 传到单个 Tool handler，适合调用时鉴权；但 `tools/list` 是静态全量结果，不能据此宣称已具备按客户端目录裁剪；
- Streamable HTTP 规范内部可以用 `text/event-stream` 承载流式响应，这属于同一 `/mcp` transport 的响应形式，不等同于旧版独立 SSE transport；禁止以扫描媒体类型的方式误判，检查重点是 Bean、端点和配置；
- 过早开放数据库查询、任意 HTTP、文件读取或代码执行会绕过 Forge 数据权限、租户、安全审计和网络边界；阶段 0 必须保持静态只读；
- 如果身份字段来自 Tool arguments，客户端可以跨租户或切换组织；身份必须在 transport/security context 中构造并覆盖所有调用参数；
- Json Schema Draft 2020-12 比 MCP SDK 当前公共投影类型更丰富，不能静默丢弃不支持的关键字；应在发布期失败关闭并报告具体路径。

## 3. 范围与功能点

### 3.1 本变更包含

- [x] **依赖与 transport Spike**：验证 WebMVC Streamable HTTP starter、Undertow 装配、单端点和无旧 SSE Bean；形成可执行 ADR。
- [x] **能力核心契约**：定义能力描述、调用、调用方、统一结果、执行器、来源、查询、行为和风险等级。
- [x] **稳定名称映射**：`capabilityCode` 使用规范化点分编码并一对一映射 `protocolToolName`，非法、超长和冲突启动失败。
- [x] **Schema 契约**：能力内核以 JSON Schema Draft 2020-12 表示输入输出，MCP 适配层只投影验证过的公共子集。
- [x] **内存 Registry**：聚合静态 `CapabilitySource`，建立不可变快照，支持稳定排序、过滤和游标查询。
- [x] **静态只读工具**：实现 `capability.ping`，不接业务数据库和外部系统。
- [x] **身份与授权接口**：定义机器客户端、租户、用户、活动组织、scope 的可信上下文和失败关闭解析契约。
- [x] **MCP 协议适配**：实现 `initialize/tools/list/tools/call` 到 Capability 的映射，统一 requestId 和安全错误结果。
- [x] **动态目录可行性验证**：验证 SDK 是否能在公开扩展点按调用上下文裁剪 `tools/list` 并返回稳定 nextCursor；不支持时记录隔离/升级方案和上线限制。
- [x] **前端预留项收敛**：隐藏或只读标注当前未实现的高风险 MCP 选项，不让用户保存虚假能力配置。

### 3.2 明确不做

- 不接入 Nacos MCP Registry、Nacos MCP Admin、MCP Gateway 或服务发现；
- 不新增正式数据库能力目录、客户端表、密钥表、授权关系表或 Flyway 脚本；
- 不实现数据库查询、任意 SQL、任意 HTTP/URL、文件读写、网页抓取、代码执行、Shell、流程、消息发送等业务工具；
- 不实现写操作、人工审批、幂等补偿或副作用重试；
- 不实现 OAuth2/Client Credentials、API Key 生命周期和正式机器客户端管理；
- 不把能力接入内部 Agent Tool Calling，不引入 AgentScope、RAG、多智能体或长期记忆；
- 不建设旧 SSE transport、`/sse` + message endpoint 双端点或 SSE 兼容层；
- 不因当前版本限制而回退 SSE；
- 不改动已完成的模型路由、供应商适配、健康和调用治理语义。

## 4. 架构与核心契约

### 4.1 模块边界

```text
forge-admin-server
    └── forge-plugin-mcp
            ├── Spring AI MCP Server WebMVC / MCP SDK（仅适配层）
            └── forge-plugin-capability
                    ├── forge-starter-core
                    └── Jackson JsonNode（JSON 契约表示）
```

依赖规则：

1. `forge-plugin-capability` 不依赖 `forge-plugin-mcp`、`forge-plugin-ai` 和 Spring AI；
2. `forge-plugin-mcp` 只负责 transport、MCP Schema/Result 转换、传输身份提取和协议错误映射；
3. 未来业务插件只通过 `CapabilitySource/CapabilityExecutor` 注册能力，不直接创建 MCP Bean；
4. 内部 Agent Runtime 未来直接消费 Capability Registry，不绕回 HTTP 调自己的 MCP Server。

### 4.2 核心 Java 契约

```java
public record CapabilityDefinition(
    String capabilityCode,
    String protocolToolName,
    String version,
    CapabilityBehavior behavior,
    CapabilityRiskLevel riskLevel,
    String description,
    JsonNode inputSchema,
    JsonNode outputSchema
) { }

public record CapabilityCallerContext(
    String machineClientId,
    Long tenantId,
    Long userId,
    Long activeOrgId,
    Set<String> scopes
) { }

public record CapabilityInvocation(
    String requestId,
    String capabilityCode,
    String version,
    CapabilityCallerContext caller,
    JsonNode arguments
) { }

public record CapabilityResult(
    String requestId,
    String capabilityCode,
    CapabilityResultStatus status,
    JsonNode data,
    String errorCode,
    String message,
    long durationMs
) { }

public interface CapabilityExecutor {
    boolean supports(CapabilityDefinition definition);
    CapabilityResult invoke(
        CapabilityDefinition definition,
        CapabilityInvocation invocation);
}

public interface CapabilitySource {
    Collection<CapabilityDefinition> load(CapabilityQuery query);
}

public interface CapabilityRegistry {
    CapabilityPage list(CapabilityQuery query, CapabilityCallerContext caller);
    CapabilityDefinition requireActive(String capabilityCode, String version);
    CapabilityResult invoke(CapabilityInvocation invocation);
}
```

内核使用 Jackson `JsonNode` 只是把输入输出固定为 JSON 契约，不引入 MCP 类型。任何 MCP 的 `McpSchema.Tool`、`CallToolResult`、transport context 都只能出现在 `forge-plugin-mcp`。

### 4.3 能力编码、版本与发布规则

1. `capabilityCode` 和 `protocolToolName` 使用同一规范：小写点分段，每段以字母开头，只允许小写字母、数字和下划线，总长 1–128，例如 `capability.ping`；
2. 阶段 0 默认映射为恒等映射，禁止大小写折叠、截断、哈希缩写或替换非法字符，避免不同编码映射到同一 Tool；
3. `version` 使用明确的三段语义版本，例如 `1.0.0`；Registry 不自动猜测最高版本；
4. 同一 `capabilityCode + version` 只能注册一次，同一 `protocolToolName` 在一个发布快照中只能对应一个 active 定义；
5. Registry 构建不可变快照前完成全部校验；任何重复、冲突、缺少执行器或 Schema 不合法都使应用启动失败；
6. 列表排序固定为 `protocolToolName ASC → version ASC`，游标编码最后一项的稳定键并带快照版本，非法或过期游标失败关闭。

### 4.4 Schema 规则

1. 能力的规范 Schema 使用 JSON Schema Draft 2020-12，并显式包含 `$schema`；
2. 阶段 0 必须覆盖 `type/object`、`properties`、`required`、`additionalProperties=false`、`enum`、嵌套 object、array/items、description 和基础数值/长度约束；
3. nullable 使用 Draft 2020-12 类型联合 `type: ["string", "null"]` 表达；如果 MCP SDK 投影不支持该结构，不得删除 null 语义，应拒绝发布并返回明确 Schema 路径；
4. `$ref`、递归 Schema、动态锚点、条件 Schema 或其它未验证关键字在阶段 0 一律拒绝，不能静默降级；
5. 输入必须先按规范 Schema 校验再调用 Executor；输出也必须校验，Executor 返回不合法结果时转换为安全内部错误；
6. 错误日志只记录 capabilityCode、requestId、Schema 路径和稳定错误码，不记录完整业务参数。

## 5. MCP Streamable HTTP 决策

### 5.1 决策表

| 项目 | 决策 | 说明 |
|------|------|------|
| Server transport | Streamable HTTP | 唯一允许的网络 transport |
| 首选 starter | `spring-ai-starter-mcp-server-webmvc:1.1.2` | 与 Forge Undertow/WebMVC 主容器一致 |
| 协议配置 | `spring.ai.mcp.server.protocol=STREAMABLE` | 必须显式覆盖该版本默认 `SSE` |
| 端点 | 单一 `/mcp` | 支持规范要求的 POST/GET/DELETE 语义，具体方法由 SDK 提供 |
| stdio | `false` | Admin Server 不开放 stdio |
| 旧 SSE | 禁止 | 不注册 `/sse`、message endpoint 和 SSE transport provider |
| Streamable 内流式响应 | 允许 | 同一 `/mcp` 下规范定义的流式响应，不是旧 SSE transport |
| WebFlux Server starter | 暂不选择 | AI 插件存在 WebFlux client/Flux 不代表主 Web 容器是 WebFlux |
| Nacos Registry/Admin | 不接入 | 阶段 0 使用本地静态 Registry |

推荐配置形态在 Spike 验证后落到受环境控制的配置文件：

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: ${FORGE_MCP_ENABLED:false}
        name: forge-ai-hub
        version: 1.0.0
        type: SYNC
        stdio: false
        protocol: STREAMABLE
        streamable-http:
          mcp-endpoint: /mcp
```

### 5.2 HARD-GATE：transport

进入能力实现前必须由最小装配测试证明：

1. WebMVC starter 在 Spring Boot 3.5.13、Java 17、Undertow 下启动成功；
2. ApplicationContext 中存在 `WebMvcStreamableServerTransportProvider`，不存在 `WebMvcSseServerTransportProvider` 及旧 SSE 自动配置 Bean；
3. `/mcp` 可协商 SDK 支持的最新 MCP 协议版本并完成 initialize；
4. 不生成 `/sse` 和 message endpoint；
5. 依赖树中 Spring AI 保持 1.1.2、MCP SDK 保持 BOM 解析出的单一版本，没有 Spring Boot 版本反向降级；
6. WebMVC 与现有 WebFlux/Reactor 类型共存不改变 Admin 主应用类型和已有流式接口。

任一项失败时停止 Task 2 以后实现，产出升级 Spring AI/MCP SDK 或独立 MCP 进程隔离方案；禁止切回 SSE。

### 5.3 HARD-GATE：动态目录

本地 `0.17.0` SDK 已确认 `tools/list` 使用静态进程列表且 `nextCursor=null`。因此阶段 0 采用以下失败关闭规则：

1. 当前端点最多发布所有已认证客户端统一可见、统一可调用的 `capability.ping`；
2. `tools/call` 即使工具已列出也必须再次做调用级授权；
3. `CapabilityRegistry#list` 自身实现按 caller 过滤与稳定游标分页，为将来适配保留正确语义；
4. MCP 端到端按客户端动态目录只有在公开 SDK 扩展点或经批准的隔离 adapter 能承载时才启用；
5. 在该能力完成前，禁止发布权限不同的多工具目录、租户私有工具或任何业务工具；
6. 禁止通过反射修改 SDK 私有 `tools` 集合，禁止每次请求 add/remove 全局工具造成跨客户端竞态。

## 6. 身份、权限与安全边界

### 6.1 可信身份来源

`McpCallerContextResolver` 从经过认证的 HTTP/Sa-Token/网关主体和 `McpTransportContext` 构造 `CapabilityCallerContext`。阶段 0 不建设客户端数据库，生产环境也不提供“信任任意 Header”的默认实现；测试可在 test scope 使用固定 Resolver。

以下字段永远不能从 Tool arguments 获取或覆盖：

- `machineClientId`
- `tenantId`
- `userId`
- `activeOrgId`
- `scopes/roles/permissions`

客户端在 arguments 中提交同名字段时，Schema 应因 `additionalProperties=false` 拒绝；即使未来业务 Schema 恰好含同名业务字段，也不能写入 caller context。

### 6.2 授权与错误

1. HTTP 身份缺失或验证失败在进入 MCP handler 前返回 401/403，并生成/传播 requestId；
2. `tools/list` 调用 `CapabilityAuthorizationPolicy#canDiscover`，`tools/call` 再调用 `canInvoke`，发现权限不等于执行权限；
3. 跨租户、跨组织或 scope 不满足统一失败关闭，不返回目标是否存在等旁路信息；
4. 统一错误码至少包含 `CAPABILITY_NOT_FOUND`、`INVALID_ARGUMENT`、`UNAUTHENTICATED`、`FORBIDDEN`、`TENANT_SCOPE_VIOLATION`、`ORG_SCOPE_VIOLATION`、`SCHEMA_UNSUPPORTED`、`EXECUTION_FAILED`、`TIMEOUT`、`INTERNAL_ERROR`；
5. `CapabilityResult.message` 是面向调用方的安全文案，不拼接 Throwable、SQL、URL、Header、Token 或原始响应正文；
6. 日志只记录 requestId、machineClientId 的安全标识、tenantId、activeOrgId、capabilityCode、结果码和耗时，不记录 arguments/data 全文。

### 6.3 阶段 0 工具安全

`capability.ping` 固定为 `READ_ONLY/LOW`：

- 输入 Schema 是不允许额外字段的空 object；
- 输出只含 `status="ok"`、`requestId`、`serverTime`；
- 不读取数据库、Redis、文件系统、环境密钥或外部网络；
- 不调用 ChatModel，不产生 Token 费用；
- 不接受用户自定义回显字符串，避免把敏感内容带入日志或响应。

## 7. 数据变更

本变更不新增或修改数据库表、索引、字典、资源和 Flyway 脚本。阶段 0 的能力定义来自静态 `CapabilitySource`，Registry 是进程内不可变快照；机器客户端、授权关系和能力目录持久化属于后续独立变更。

## 8. 接口与配置变更

| 类型 | 接口/配置 | 方法 | 变更内容 |
|------|-----------|------|----------|
| 新增 | `/mcp` | MCP Streamable HTTP GET/POST/DELETE | 由 SDK 暴露 initialize、tools/list、tools/call；不作为普通 REST Controller |
| 新增 | `FORGE_MCP_ENABLED` | 环境变量 | 默认 false；只有可信身份 Resolver 与全部硬闸门通过后才允许开启 |
| 新增 | `spring.ai.mcp.server.protocol` | 配置 | 固定显式值 `STREAMABLE` |
| 新增 | `spring.ai.mcp.server.streamable-http.mcp-endpoint` | 配置 | 固定 `/mcp` |
| 调整 | Agent 管理页面 MCP 工具区域 | UI | 隐藏未实现高风险选项或改为明确只读预留提示，不新增虚假保存接口 |

普通业务 REST API 不新增 Capability CRUD；管理目录和客户端授权留到后续变更。

## 9. 影响范围

- Maven 聚合：`forge-plugin-parent`、`forge-dependencies`；
- 新模块：`forge-plugin-capability`、`forge-plugin-mcp`；
- 应用装配：仅 `forge-admin-server` 引入 MCP 插件，Flow/Report/App 服务不自动暴露 `/mcp`；
- 配置：Admin 示例配置增加默认关闭的 MCP 配置；真实本地密钥配置不提交；
- 前端：`agent.vue` 只收敛未实现的 MCP 选项，不重做页面；
- 文档：回填 AI 中枢路线图、MCP transport ADR 和本变更验证证据；
- 现有模型路由、供应商、Agent 对话、Flowable 和数据库结构不应发生行为变化。

## 10. 风险、失败关闭与回滚

| 风险 | 失败关闭条件 | 回滚/处理 |
|------|--------------|-----------|
| Starter 自动启用旧 SSE | 出现 SSE transport Bean、`/sse` 或 message endpoint | 停止实施，排除自动配置或改用明确的 Streamable WebMVC starter；不降级 SSE |
| Undertow/WebMVC/WebFlux 冲突 | 应用类型变化、启动失败、已有流式接口回归 | MCP 插件保持默认禁用并评估独立进程隔离 |
| 依赖版本漂移 | 多个 Spring AI/MCP SDK 版本或 Boot 降级 | 通过 BOM/exclusion 收敛；无法收敛则停止 |
| 动态 tools/list 不可用 | SDK 只能静态返回全量工具 | 只发布统一授权 ping；业务工具发布闸门保持关闭 |
| 身份 Resolver 缺失 | MCP enabled 且没有可信 Resolver | 启动失败，不注册匿名 Resolver |
| Schema 投影丢语义 | MCP SDK 不支持关键 Draft 2020-12 结构 | 拒绝注册该能力并报告 Schema 路径 |
| Registry 冲突 | code/version/toolName 重复或无 Executor | 启动失败，不采用后注册覆盖前注册 |
| 工具副作用漂移 | ping 访问数据库/网络/模型或出现写行为 | 测试和依赖扫描失败，禁止发布 |

MCP 功能通过 `FORGE_MCP_ENABLED=false` 可逆关闭；两个新插件若需整体回滚，从 Admin 聚合依赖移除即可，不涉及数据回滚。

## 10.5 测试策略

- **测试范围**：核心契约、名称与 Schema 校验、Registry/授权/游标、静态 ping、Streamable HTTP 协议、身份隔离、依赖树、无旧 SSE 装配和 Agent 预留项；
- **覆盖率目标**：Capability 核心分支覆盖率 ≥ 90%，名称/Schema/授权/Registry 失败关闭分支全部覆盖；
- **独立 Test Spec**：是，见 `test-spec.md`；
- **测试约束**：不访问公网、不连接真实数据库/Redis/Nacos、不调用模型、不产生费用；时间使用注入 `Clock`，禁止 sleep；
- **阶段验证**：严格按 `code-copilot/rules/automated-testing-standard.md` 记录 Red/Green、命令输出、警告、跳过项和服务清理。

## 11. 待确认与 HARD-GATE

以下不是允许模糊处理的一般待办，而是 `/apply` 必须按顺序验证并记录的停止条件：

1. WebMVC Streamable transport 在 Forge Admin/Undertow 的实际装配结果；
2. Spring AI 1.1.2 与项目 Spring Boot 3.5.13 的最终依赖树是否保持兼容；
3. transport context extractor 与 Forge 认证过滤链的执行顺序，能否得到可信机器主体；
4. MCP SDK 当前/可升级版本是否提供按 exchange 自定义 `tools/list` 和 nextCursor 的公开扩展点；
5. Draft 2020-12 规范 Schema 到 MCP SDK `JsonSchema` 的无损公共子集边界。

Task 1 只验证 1–3；Task 6 验证 4。任何 HARD-GATE 失败都必须先回填 Spec/ADR 并停止受影响的后续任务，不能私自扩大为 SDK fork 或旧 SSE 兼容。

## 12. 技术决策

1. 采用“协议无关 Capability 内核 + MCP 适配插件”，不从 Controller/Mapper 直接生成 Tool；
2. MCP Server 仅使用 Streamable HTTP，首选 WebMVC starter，端点 `/mcp`；
3. 明确禁用旧 SSE transport；Streamable HTTP 内部流式响应不视为旧 SSE；
4. 阶段 0 使用内存静态 Registry，不接 Nacos、不建表；
5. 能力规范 Schema 使用 Draft 2020-12，MCP adapter 只投影验证过的公共子集；
6. 身份来自传输安全上下文，业务参数不能传入或覆盖安全字段；
7. 当前 SDK 动态目录不足时只发布统一授权 ping，异构业务工具失败关闭；
8. `forge-plugin-capability` 不依赖 `forge-plugin-ai`，为内部 Agent Runtime、HTTP 和工作流复用保留边界。

## 13. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Proposal Research | 完成 | `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` | 仅文档；未写业务代码、未改数据库 |
| Task 1–8 | 完成 | `forge-plugin-capability`、`forge-plugin-mcp`、Admin POM/YAML、`agent.vue`、本变更四份文档 | Streamable HTTP HARD-GATE、身份失败关闭、静态 ping、依赖收敛和回归验证均通过；SDK 0.17.0 动态目录限制保留 |
| Review Fix | 完成 | Capability 授权/游标/Schema/审计，MCP 协议闸门/身份过滤/适配与回归测试 | 修复 1 个 Critical 与 6 组 Important 问题；Capability 13、MCP 13、AI 84 测试及 Admin 37 模块聚合均通过 |
| Archive | 完成 | 四份变更文档、路线图、长期决策与踩坑记录 | 用户明确授权归档；阶段 0 完成，SDK 动态目录限制作为阶段 1 首个硬闸门保留 |

## 14. 审查结论

已完成首轮 `/review` 指出的全部阻断修复：MCP 启动期只允许 `STREAMABLE` 且拒绝 stdio，HTTP 身份失败在协议处理前返回 401，授权决策可传播租户/组织错误码，游标绑定查询与调用方并使用 HMAC-SHA256 防篡改，MCP 只投影完整校验后的 Schema，同时补齐安全审计和 P0 回归。用户于 2026-07-11 明确授权归档；验证证据完整，状态更新为 `done`。阶段 1 在解决按客户端动态目录之前仍禁止发布异构业务工具。

## 15. 确认记录（HARD-GATE）

- **确认时间**：2026-07-11，用户执行 `/apply forge-ai-hub-foundation`；
- **确认人**：用户；
- **确认范围**：只批准阶段 0 能力底座与 Streamable HTTP Spike，不代表批准 Nacos、数据库能力目录、高风险业务工具或旧 SSE。
