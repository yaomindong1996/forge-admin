# 执行日志 — Forge AI 中枢能力底座

> change: `forge-ai-hub-foundation`
> status: done
> created: 2026-07-11

## 1. 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-07-11 | Research | 读取 Forge AGENTS、code-copilot 规则、记忆、Proposal 模板和自动化测试标准 | 本轮遵循 No Spec No Code |
| 2026-07-11 | Research | 读取 AI 中枢战略方案、落地路线图和两个已归档 AI 变更 | 确定阶段 0 边界 |
| 2026-07-11 | Decision | 用户明确 MCP Server 只使用最新 Streamable HTTP | 禁止旧 SSE transport 和双端点兼容层 |
| 2026-07-11 | Research | 检查当前工程依赖与 Agent 页面 | 当前无 MCP/Capability；页面存在未实现高风险选项 |
| 2026-07-11 | Dependency Spike | 解析 Spring AI 1.1.2 三种 MCP Server starter | 通用/WebMVC/WebFlux 坐标均存在 |
| 2026-07-11 | Bytecode Research | 检查 MCP Server properties、WebMVC auto-config 和 MCP SDK 0.17.0 | Streamable provider 存在；默认协议 SSE；tools/list 静态且 nextCursor=null |
| 2026-07-11 | Proposal | 创建 spec/tasks/test-spec/execution-log | 未写业务代码、未改数据库、未启动服务 |
| 2026-07-11 | Apply HARD-GATE | 新建 Capability/MCP 插件并打通 Streamable HTTP | `/mcp` 完成 initialize/list/call；协议 `2025-06-18` |
| 2026-07-11 | Apply Dependency | 消除 Spring Boot 3.2.9/3.5.13 版本漂移 | 最终 Boot 3.5.13、Spring AI 1.1.2、MCP SDK 0.17.0 |
| 2026-07-11 | Apply Security | 接入 transport context 身份解析和调用时二次授权 | 无 Resolver 启用即失败；arguments 不能覆盖调用方身份 |
| 2026-07-11 | Apply Validation | 完成后端、Admin、前端、依赖与静态扫描 | AI 84、Capability 9、MCP 8，均 0 失败 |
| 2026-07-11 | Review Fix | 修复协议外部覆盖、HTTP 身份边界、授权错误码、HMAC 游标防篡改、Schema 无损校验和安全审计 | 1 个 Critical 与 6 组 Important 问题完成修复 |
| 2026-07-11 | Review Fix Validation | Capability/MCP 26 tests、AI 84 tests、Admin 37 模块聚合与静态扫描通过 | 不访问公网、数据库、Nacos 或模型服务 |
| 2026-07-11 | Archive | 用户明确授权归档，复核四份文档、路线图、长期记忆和最终验证证据 | 状态更新为 done；未 commit/push，SDK 动态目录限制转入阶段 1 硬闸门 |

## 2. Research 证据

### 2.1 工程事实

- Spring Boot：3.5.13；
- Spring AI：1.1.2；
- Spring AI Alibaba/Extensions：1.1.2.3；
- Java：17；
- 主 Web 容器：Undertow/WebMVC；
- `forge-plugin-ai` 存在 `spring-boot-starter-webflux`，用于现有流式链路；
- 当前没有 MCP Server、Capability Registry、ToolCallback 注册链、Nacos MCP Registry/Admin 或 Agent Framework；
- `agent.vue#mcpToolOptions` 存在 database/http/file/web/code 等硬编码项，但后端没有实现。

### 2.2 Maven 坐标检查

执行：

```bash
mvn -q dependency:get \
  -Dartifact=org.springframework.ai:spring-ai-starter-mcp-server:1.1.2 \
  -Dtransitive=false
mvn -q dependency:get \
  -Dartifact=org.springframework.ai:spring-ai-starter-mcp-server-webmvc:1.1.2 \
  -Dtransitive=false
mvn -q dependency:get \
  -Dartifact=org.springframework.ai:spring-ai-starter-mcp-server-webflux:1.1.2 \
  -Dtransitive=false
```

结果：三个坐标均解析成功。`spring-ai-autoconfigure-mcp-server:1.1.2` 不存在，实际自动配置模块按 common/webmvc/webflux 拆分，不能在实现中猜坐标。

本命令只更新用户本地 Maven 缓存，没有修改仓库 POM。

### 2.3 JAR/POM 与字节码检查

执行过的只读检查：

```bash
jar tf ~/.m2/repository/org/springframework/ai/spring-ai-autoconfigure-mcp-server-webmvc/1.1.2/spring-ai-autoconfigure-mcp-server-webmvc-1.1.2.jar
javap -classpath <mcp jars> -private -constants \
  org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties \
  org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties
javap -classpath <mcp jars> -c -p \
  io.modelcontextprotocol.server.McpAsyncServer
```

确认结果：

1. `McpServerProperties.CONFIG_PREFIX = spring.ai.mcp.server`；
2. `ServerProtocol` 枚举包含 `SSE/STREAMABLE/STATELESS`；
3. Spring 配置元数据中 `spring.ai.mcp.server.protocol` 默认值为 `sse`；
4. `McpServerStreamableHttpProperties.CONFIG_PREFIX = spring.ai.mcp.server.streamable-http`；
5. `mcp-endpoint` 默认 `/mcp`；
6. WebMVC 自动配置创建 `WebMvcStreamableServerTransportProvider` 和对应 RouterFunction；
7. transport builder 提供 `contextExtractor(...)`，并可把 `McpTransportContext` 传入 Tool exchange；
8. MCP SDK `0.17.0` 的 `tools/list` handler 遍历全局 `CopyOnWriteArrayList tools`，构造 `ListToolsResult(tools, null)`；当前实现不是按请求上下文过滤，也没有游标分页；
9. Streamable provider 内部存在基于同一 transport 的流式响应处理，这是 Streamable HTTP 规范行为，不能误判为旧 SSE transport。

### 2.4 本地 vendor 源码

- `docs/vendor/spring-ai-extensions-1.1.2.3-release/mcp/spring-ai-alibaba-mcp-common/src/main/java/com/alibaba/cloud/ai/mcp/common/transport/builder/WebFluxStreamableClientTransportBuilder.java` 证明 Alibaba Extensions 有 Streamable HTTP 客户端路径；
- Nacos registry 源码引用 `McpServerStreamableHttpProperties`，说明上游扩展认识 streamable server 属性；
- vendor 同时保留旧 SSE 实现，因此 Forge 必须通过依赖、配置和 Bean 断言主动排除，不能“都引入但只用一个”。

## 3. 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
|------|------|------------|------|
| 能力架构 | 协议无关 capability + MCP adapter 两层 | Controller/Mapper 直接变 Tool | 保持复用、安全和演进边界 |
| MCP transport | Streamable HTTP `/mcp` | 旧 SSE 双端点、SSE 兼容层 | 用户明确要求且符合最新协议方向 |
| Server starter 候选 | WebMVC starter | 因存在 Flux 就选 WebFlux server | Forge 主容器是 Undertow/WebMVC |
| 协议配置 | 显式 `STREAMABLE` | 依赖默认值 | 1.1.2 默认仍是 SSE |
| Registry | 内存静态快照 | Nacos/数据库 | 阶段 0 先验证核心契约 |
| 初始 Tool | `capability.ping` | database/http/file/code | 只读无副作用，适合验证协议 |
| 身份 | transport/security context resolver | arguments 或裸租户 Header | 防止跨租户与身份伪造 |
| Schema | Draft 2020-12 规范 + 公共子集投影 | 直接绑定 MCP SDK Schema | 内核协议无关且不静默丢语义 |
| 动态目录 | 内核支持；MCP 当前不足则限制为统一 ping | 反射 SDK/请求时全局增删工具 | 避免跨客户端竞态和虚假能力声明 |

## 4. Proposal 静态 Reader Test

由主 Agent 使用 fresh-reader checklist 复核；本轮没有创建新子 Agent。

| 问题 | 文档可独立回答的结论 | 位置 |
|------|----------------------|------|
| 为什么不能从 Controller 直接生成 MCP Tool？ | 会把协议、权限和业务耦合，无法供内部 Runtime 复用 | `spec.md` 2.4、4.1 |
| 阶段 0 是否接业务数据库？ | 不接；无表、无 Flyway、只用静态 Registry | `spec.md` 3.2、7 |
| MCP transport 是什么，旧 SSE 是否允许？ | 只允许 Streamable HTTP `/mcp`；旧 SSE 禁止 | `spec.md` 5 |
| 当前版本不支持 Streamable 时怎么办？ | 停止后续任务，升级或隔离；禁止回退 SSE | `spec.md` 5.2、10 |
| Capability 内核为什么不能依赖 MCP/AI？ | 它要被 MCP、内部 Agent、HTTP/工作流共同复用 | `spec.md` 4.1 |
| 哪些身份字段客户端不能传？ | machineClientId、tenantId、userId、activeOrgId、scopes | `spec.md` 6.1 |
| 当前 SDK 动态 tools/list 不足怎么办？ | 只发布统一授权 ping，业务工具发布失败关闭 | `spec.md` 5.3 |
| 阶段 0 的退出闸门是什么？ | transport、身份、Schema、ping、依赖与无 SSE 验证全部通过 | `spec.md` 1.1、10.5 |
| 下一阶段才建设什么？ | 持久化目录、客户端授权、动态业务工具、Agent Runtime/Nacos 等 | `spec.md` 3.2、7 |

Reader Test 结论：范围和失败关闭条件明确；没有把 Streamable HTTP 内部流式响应误写成旧 SSE；没有把当前 SDK 静态 tools/list 描述为动态能力。

## 5. Proposal 验证记录

| 检查 | 命令 | 结果 |
|------|------|------|
| 目标目录唯一 | `test ! -e code-copilot/changes/forge-ai-hub-foundation`（创建前） | 通过，未覆盖已有变更 |
| 四份文档存在 | `find code-copilot/changes/forge-ai-hub-foundation -maxdepth 1 -type f` + 文件数断言 | 通过，恰好 4 份 Markdown |
| status/范围检查 | `rg` 检查 propose、Streamable、SSE 禁令、无 DB/Nacos | 通过 |
| Markdown/空白 | 对四份未跟踪文档逐一执行 `git diff --no-index --check /dev/null <file>`，并检查 EOF 换行与尾随空格 | 通过 |
| 业务代码/数据库 | `git status --short` 对比 | 本 Proposal 只应新增四份文档；工作区其它改动为用户既有改动 |

## 6. Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|-----------|----------|----------|
| Proposal 阶段 | 只创建文档 | 已按用户 `/apply` 指令进入实现 | 状态更新为 `apply` |
| SDK 动态目录 | 希望按 caller 过滤与分页 | 本地 SDK 0.17.0 静态列表、cursor=null | 在 Spec 中设为异构业务工具发布闸门，不虚构支持 |
| Admin 默认关闭测试 | 希望验证“默认无 MCP transport” | 上游 Starter 在完全无配置时默认启用 SSE；Forge Admin 通过 YAML 显式 `enabled=false` 覆盖 | 测试验证 Admin 默认值关闭；同时保留显式 STREAMABLE 和缺 Resolver 失败关闭测试 |
| 计划中的细分测试类 | 每项能力可拆独立测试类 | 身份、Schema、动态目录限制等部分场景合并到 Registry/Adapter/Transport 测试 | 保持行为覆盖，避免为文件名机械拆分测试 |

## 7. 明确未执行与范围外事项

- 未新增或修改本变更专属 Flyway；工作区 `V1.0.18__add_ai_model_routing_governance.sql` 属于已完成的前序路由治理变更；
- 未连接数据库、Redis、Nacos、MCP Registry/Admin、模型服务或业务外部 API；
- 未实现数据库、HTTP、文件、网页、代码执行等高风险工具；
- 未启动 Admin、Flow、Report、App 真实服务；协议闭环使用 MockMvc 完成，测试结束无遗留进程和端口；
- 未执行真实模型调用或产生 Token 费用；
- 未 commit/push，未清理或覆盖用户已有脏工作区改动。

## 8. 代码质量备忘

- 实现时不能引用 Spring AI 1.1.2 默认 protocol；必须显式 STREAMABLE；
- 无 SSE 扫描应检查 transport provider/endpoint/config，不应把 Streamable HTTP 内合法 `text/event-stream` 响应当成违规；
- 不能用反射访问 MCP SDK 私有工具列表实现动态目录；
- `CapabilityResult` 不保存 Throwable，日志不打印 arguments/data；
- `capability.ping` 必须保持无业务依赖；
- 当前 dirty worktree 中的既有改动属于用户，实施时只处理本变更明确文件并避免覆盖。

## 9. Apply 实现摘要

### 9.1 协议无关 Capability 内核

- 新增 `forge-plugin-capability`，包含 Definition、Caller、Invocation、Result、Query、Page、Cursor、Source、Executor、AuthorizationPolicy 和 Registry 契约；
- `capabilityCode` 使用小写点分编码并恒等映射到协议工具名，版本固定三段语义格式；
- Registry 在启动期拒绝重复 code/version、重复 toolName、无/多 Executor，使用不可变快照和 SHA-256 快照版本；
- 支持 caller 过滤、稳定游标分页、输入/输出 Schema 双重校验和调用时二次授权；
- `CapabilityResult` 不保存 Throwable，异常只映射安全错误码和通用信息。

### 9.2 Schema 与静态 ping

- 实现 Draft 2020-12 公共子集：object/properties/required/additionalProperties、enum、嵌套对象、array/items、nullable 联合类型、数值/字符串/数组边界；
- `$ref`、条件 Schema、动态关键字等未支持语义失败关闭；MCP SDK 无法无损表达 nullable 时拒绝发布；
- 注册 `capability.ping@1.0.0`，行为 READ_ONLY、风险 LOW，输入只允许空对象，输出仅 status/requestId/serverTime；
- ping 只依赖注入 Clock，不访问数据库、Redis、文件、网络或 ChatModel。

### 9.3 Streamable HTTP 与身份边界

- 新增 `forge-plugin-mcp`，使用 `spring-ai-starter-mcp-server-webmvc:1.1.2` 和 Forge Undertow Web Starter；
- 显式配置 `protocol=STREAMABLE`、`stdio=false`、`mcp-endpoint=/mcp`，不提供旧 `/sse` 和消息回传双端点；
- `ForgeMcpTransportContextExtractor` 只接受 `McpCallerContextResolver` 产生的可信 caller；无 Resolver 时启用 MCP 会启动失败；
- MCP 只静态发布 `capability.ping`。SDK 0.17.0 的 `tools/list` 固定全量且 `nextCursor=null`，因此异构业务工具仍被发布闸门禁止；
- Streamable HTTP 在同一 `/mcp` 上返回 `text/event-stream` 流式响应属于协议合法行为，不是旧独立 SSE transport。

### 9.4 Admin 与前端

- Admin 聚合 `forge-plugin-mcp`，默认 `${FORGE_MCP_ENABLED:false}`，显式锁定 STREAMABLE；
- Agent 页面将未实现 MCP 工具选择器改为只读预留展示，保留历史 `extraConfig.mcpTools`，不允许新增虚假配置，也不自动清空历史值；
- 未做额外 UI 重设计。

## 10. Red/Green 与失败修正记录

| 阶段 | Red/异常 | 修正与最终结果 |
|------|----------|----------------|
| Transport 初始装配 | 缺模块和 MCP starter，Context 无法形成 Streamable provider | 新增 WebMVC MCP 插件并排除 starter 自带 Web 容器，Streamable provider Green |
| 版本装配 | 父层遗留 Boot 3.2.9 与 Spring AI BOM 3.5.x 漂移 | 移除局部 3.2.9 覆盖，Forge BOM 统一为 3.5.13；Admin 37 模块 package Green |
| 默认关闭用例 | 完全无配置时上游自动装配默认 SSE，新增用例首次失败 | 按 Admin 实际默认 `enabled=false` 验证无任何 transport；显式启用仍强制 STREAMABLE |
| Maven 测试命令 | 普通 `mvn test` 和仅 `-DskipTests=false` 仍被项目 `forge.tests.skip/groups` 跳过 | 使用 `-Penable-tests`，得到真实 Tests run 结果 |
| dependency:tree | 首次 zsh 未引用 `*` 导致 shell 展开失败；第二次未加 `-am` 尝试从仓库解析内部 capability 并失败 | 引用 includes 且加入 `-am`，依赖树 Green |
| Review Fix 接口演进 | `CapabilityAuthorizationPolicy` 从 boolean 扩展为结构化决策后，旧测试实现首次 testCompile 失败 | 更新测试策略实现和调用断言；最终 Capability 13、MCP 13 全绿 |

## 11. 最终验证证据

| 范围 | 命令摘要 | 结果 |
|------|----------|------|
| AI 回归 | `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai test` | Tests run: 84，Failures: 0，Errors: 0，Skipped: 0 |
| Capability + MCP（Review Fix） | Java 17 执行 `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-mcp -am test` | Capability 13 + MCP 13，Failures: 0，Errors: 0，Skipped: 0；BUILD SUCCESS |
| Admin 聚合 | `mvn -pl forge-admin-server -am package -DskipTests` | 37/37 模块 SUCCESS，生成可执行 jar；仅原有 `EmployeeServiceImpl` deprecated API 提示 |
| 依赖树 | `mvn -pl .../forge-plugin-mcp -am dependency:tree -Dverbose ...` | Spring AI 单一 1.1.2；MCP SDK 单一 0.17.0；Boot 单一 3.5.13；Web 容器为 Undertow |
| MCP 协议 | `McpStreamableProtocolIntegrationTest` | `/mcp` initialize → initialized → tools/list → tools/call 全部通过，协议 2025-06-18，只返回 capability.ping |
| MCP 装配 | `McpStreamableTransportCompatibilityTest` | Admin 默认值关闭；启用时只有 Streamable provider；无 Resolver 失败关闭；无 SSE provider |
| 前端 lint | `pnpm exec eslint src/views/ai/agent.vue` | 通过，无输出 |
| 前端 build | `pnpm build` | 通过，`built in 2m 28s` |
| 静态扫描 | `rg` 扫描 capability/mcp 生产代码与 Admin 配置 | 无旧 SSE、Nacos MCP、AgentScope、高风险工具实现；Capability 无 Spring AI/MCP/AI 插件引用 |
| 空白检查 | `git diff --check` | 通过 |
| Review Fix 静态安全检查 | `rg` 扫描两个插件生产代码的模块依赖、旧 SSE、Nacos MCP、AgentScope、高风险执行 API 和敏感日志字段 | 禁止项无输出；Projector 对外只接受 `ValidatedCapabilitySchema` |

依赖树中的 `tomcat-embed-el:10.1.53` 是 Validation/Undertow 使用的 EL 实现，不包含 Tomcat Servlet 容器核心；`spring-boot-starter-tomcat`、`tomcat-embed-core` 和 Tomcat Web Server 未进入 MCP 运行链。

## 12. 警告、跳过项与环境清理

- 前端 build 警告均为仓库既有问题：`UserSelectModal` 命名冲突、若干模块同时动态/静态导入、CSS 中使用 `//` 注释；不由本变更引入且不阻断构建；
- Spring 集成测试存在 Mapstruct BeanFactoryPostProcessor 与 MCP annotation scanner 的上游生命周期警告；Admin 配置已显式关闭 annotation scanner，协议测试结果不受影响；
- AI 测试中的模拟网络异常堆栈属于预期失败路径断言，最终 84 项全绿；
- 未配置 JaCoCo，本轮未生成数值覆盖率报告；核心 P0 分支通过行为测试和静态扫描验收；
- Maven 依赖树纠正前曾访问已配置 Maven 仓库解析内部模块，收到私服 401；没有应用级公网调用、模型调用或业务数据传输；
- 本变更目录及 Capability/MCP 源码的工作区、暂存区空白检查均通过；仓库级 `git diff --cached --check` 会命中用户已放入暂存区的两套 `docs/vendor/spring-ai-*` 上游源码原始尾随空格，因此未改写第三方 vendor 文件，也不将其误记为本变更失败；
- 未启动真实服务，无 PID、端口或临时测试数据需要清理；前端只生成被忽略的 `dist` 产物。

## 13. Apply 审查结论

- **Spec Compliance**：阶段 0 范围全部落地；无数据库/Nacos/Agent Framework/高风险工具/SSE 范围漂移；
- **Code Quality**：模块依赖保持 `mcp -> capability` 单向，Schema 与身份均失败关闭，错误结果不泄露 Throwable；
- **HARD-GATE**：transport、身份、Schema、静态 ping、依赖收敛、Admin package 和前端构建全部通过；
- **保留限制**：SDK 0.17.0 不支持按 caller 动态裁剪 MCP `tools/list` 和稳定 nextCursor。正式业务 Tool 上线前必须升级/隔离 SDK 适配，当前只允许统一授权 `capability.ping`。

结论：`/apply forge-ai-hub-foundation` 完成，可进入独立 `/review forge-ai-hub-foundation`；不应直接进入业务 Tool 或 Agent Runtime 实现。

## 14. Review Fix 结论

- **Critical 已关闭**：配置被外部覆盖为 SSE/STATELESS 或打开 stdio 时，启动期硬失败，不能再形成旧 transport；
- **身份与授权边界已关闭**：`/mcp` 身份失败返回 HTTP 401；租户、组织、scope 决策使用稳定错误码传播；
- **目录与 Schema 边界已关闭**：游标绑定查询/调用方、验证真实位置并使用进程内 HMAC-SHA256 防篡改；MCP 只投影经完整校验的 Schema，错误关键字组合与不一致边界启动失败；
- **可观测与测试缺口已关闭**：安全日志只记录必要元数据；Capability/MCP 各 13 项、AI 84 项和 Admin 37 模块聚合全部通过；
- **范围保持不变**：仍只有 Streamable HTTP `/mcp` 与统一授权的 `capability.ping`，未引入 Nacos、业务工具、数据库或 Agent Runtime。

结论：`/fix forge-ai-hub-foundation` 已完成，用户随后明确授权归档。变更状态更新为 `done` 并移动到 `code-copilot/changes/archive/2026-07-11-forge-ai-hub-foundation/`；下一阶段必须先解决动态工具目录与持久化控制面，不能直接开放业务写工具。
