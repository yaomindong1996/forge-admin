# 测试 Spec — MCP 用户委托身份

> status: done
> created: 2026-07-12

## 1. P0：规范与授权码

### 1.1 实现 HARD-GATE 基线

- Identity 依赖树中不得出现 `spring-security-oauth2-authorization-server` 或 `sa-token-oauth2`；
- Forge OAuth 2.1 Profile 契约测试必须覆盖 PUBLIC + PKCE、CONFIDENTIAL、`resource`、精确 redirect、单次 code 与未知 grant 失败关闭；
- 后续 OAuth 行为测试必须覆盖 metadata → authorize → token → `/mcp` 闭环，不能以类存在或可编译代替协议证明。

- RFC 9728 资源元数据包含 canonical resource 和 authorization server；
- 401 包含正确 `WWW-Authenticate` resource metadata；
- RFC 8414 元数据只声明真实支持的 grant、PKCE 和客户端认证方法；
- authorization request/token request 缺少或改变 resource 时拒绝；
- 只允许 PKCE S256，plain/空 challenge/verifier 不匹配拒绝；
- redirect URI 必须逐字匹配，前缀、子域、端口、path、fragment 变化拒绝；
- code 120 秒过期且只能使用一次，20 路并发兑换只有一个成功；
- state 原样返回但不进入服务端身份、日志或授权判断。

## 2. P0：Token 与双身份

- authorization_code 只生成 USER Token，actorUserId 为授权用户 A；
- client_credentials 只生成 SERVICE Token，actorUserId 等于 serviceUserId；
- USER/SERVICE 均保留 clientId、serviceUserId、tenantId、activeOrgId；
- Token 至少 256 bit、TTL ≤ 15 分钟，数据库无原始 Token/code/verifier；
- HMAC 常量时间比较；未知 keyId 仍走 dummy HMAC；
- Token 过期、撤销、客户端禁用、密钥轮换、credentialVersion 变化立即拒绝；
- 用户禁用/删除、租户成员失效、组织解绑、强制改密、超级管理员服务账号拒绝；
- 用户加载发生组织回退时拒绝，不能静默使用主组织。

## 3. P0：MCP 与路径隔离

- `/mcp` 从通用 Sa-Token 拦截器排除后，无 OAuth Token 仍由专用 Filter 返回 401；
- 长期 `fcp_...` Secret 不能直接调用 `/mcp`；
- `fdu_...` Token 只能从 Authorization Bearer 读取，query/cookie/arguments 不接受；
- MCP Token 请求普通 `/system/**`、`/ai/**` 管理接口被拒绝；
- 错误 audience、scope、resource 返回 401/403，内部异常不泄露；
- 每个 Streamable HTTP POST/GET 都重新认证，Mcp-Session-Id 不能替代 Token；
- Origin 存在时必须匹配 allowlist；恶意 Origin 拒绝；
- initialize → initialized → tools/list → tools/call 仍使用协议 `2025-06-18`；
- 不出现旧 `/sse`、message endpoint、SSE transport Bean、STATELESS 或 stdio。

## 4. P0：Forge 执行上下文

- USER Token 建立 A 的 LoginUser/tenant/activeOrg/scopes；
- SERVICE Token 建立绑定服务账号上下文；
- ORM insert 自动填充 `create_by/update_by=A` 和 `create_dept=activeOrgId`；
- ORM update 自动填充 `update_by=A`；
- OperationLog 的 userId 为 A，不是服务账号或 null；
- 正常、异常、401、403、SDK 抛错和异步路径 finally 后无身份/Tenant/MDC 泄漏；
- 嵌套上下文退出后恢复外层用户；线程池连续执行 A/B 不串身份；
- 普通 Forge Sa-Token 登录的 SessionHelper 行为保持不变。

## 5. P1：数据与接口

- `V1.0.22` 唯一，表/实体/XML/逻辑删除一致；
- redirect URI 和 access token 查询均显式 tenant 条件；仅 token keyId 认证定位允许受控跨租户查找；
- PUBLIC 客户端禁止 client_credentials；CONFIDENTIAL 客户端错误 Secret 拒绝；
- OAuth 未启用、无 redirect、无 scope 或客户端无 grant 时拒绝；
- revoke 幂等，普通行删除逻辑删除；
- 管理 API 不返回 tokenHash、原始 Token、code 或 verifier。

## 6. 回归矩阵

| 范围 | 命令 | 预期 |
|---|---|---|
| Core/ORM/Log | `mvn -Penable-tests -pl forge-starter-log -am test` | 显式/普通身份上下文全绿 |
| Identity | `mvn -Penable-tests -pl .../forge-plugin-capability-identity -am test` | OAuth、Token、上下文 P0 全绿 |
| MCP | `mvn -Penable-tests -pl .../forge-plugin-mcp -am test` | Streamable HTTP 13+ tests 全绿 |
| Control Plane | `mvn -Penable-tests -pl .../forge-plugin-capability-control-plane -am test` | 20+ tests 全绿 |
| AI | `mvn -Penable-tests -pl .../forge-plugin-ai test` | 84 tests 全绿 |
| Admin | `mvn -pl forge-admin-server -am package -DskipTests` | 全部预期 reactor modules SUCCESS |
| Frontend | `nvm use v20.19.0 && pnpm build` | production build PASS |

## 7. 跳过条件

- 没有受控 MySQL/Redis 时，只能跳过真实 Flyway、Redis code 原子兑换和完整服务联调；必须记录具体原因；
- 单元测试和 MockMvc 不能冒充真实 Redis 原子性或真实数据库迁移；
- 本变更不调用真实模型，不产生模型 Token 费用；
- 本变更不测试业务写副作用和 Flowable 办理，因为明确不在范围内。

## 8. Apply 增量验证结果（2026-07-12）

| 范围 | 结果 | 证据摘要 |
|---|---|---|
| Identity | PASS | 18/18；Token codec/service/controller、PKCE/redirect/resource、Redis HMAC+Lua、MCP resolver、上下文、双身份审计、依赖隔离 |
| Capability | PASS | 13/13；Observer 接入后注册、授权、Schema 和调用回归 |
| MCP | PASS | 13/13；协议版本 `2025-06-18`、单 Streamable HTTP、裸请求 401 和 transport guard |
| Control Plane | PASS | 20/20；客户端、grant、审计、Schema 回归 |
| Admin | PASS | 39/39 reactor `package -DskipTests`；单模块启用测试生命周期通过，旧 `fcp_` Resolver 测试已清除 |
| Dependency | PASS | Identity dependency tree、全部 POM、Admin JAR 均无 SAS/Sa-Token OAuth2 |
| SQL/XML | PASS | Mapper XML `xmllint`；V1.0.22 唯一；无 Flyway placeholder、tenant 0、物理 DELETE |
| Frontend | PASS | Node 20.19.0 目标 ESLint 无输出；Vite production build PASS |

条件跳过：未对用户现有数据库执行 V1.0.22；未用真实 Redis 做 20 路并发兑换；未启动 Admin 做真实浏览器授权与完整 metadata → authorize → token → `/mcp initialize` E2E。以上不计为 PASS，留待 Review/受控环境验收。

## 9. Review 增量验证（2026-07-12）

本轮未修改实现，只复跑高风险模块和静态边界：

| 检查 | 结果 |
|---|---|
| Identity tests | PASS，18/18 |
| MCP tests | PASS，13/13 |
| Identity dependency tree | PASS，无 SAS、无 Sa-Token OAuth2 |
| `git diff --check`、Mapper `xmllint`、V1.0.22 placeholder/tenant 0/物理 DELETE | PASS |
| MCP 集成测试身份来源核查 | FAIL HARD-GATE：使用测试 Resolver，不是 `fdu_`/Identity 聚合链 |
| client grant + 用户权限交集核查 | FAIL HARD-GATE：运行时仅检查 Token scope |
| PUBLIC/USER revoke | FAIL：当前端点只接受 CONFIDENTIAL client secret |
| 当前组织前置校验 | FAIL：授权阶段未比较 user/client activeOrgId |

Review 结论：已有单元测试继续通过，但不能覆盖或推翻 R1–R5；Fix 后必须为每项增加 Red/Green 证据。

## 10. Fix 增量验证（2026-07-12）

| Review 项 | Red/Green 证据 | 结果 |
|---|---|---|
| R1 聚合链 | `McpDelegatedIdentityIntegrationTest`：合法 fdu、完整 Sa-Token 排除链、initialize/list/call、真实审计 Observer、Session/Tenant/MDC 清理 | PASS，3/3 |
| R2 权限交集 | `ForgeCapabilityAuthorizationPolicyTest` + AutoConfiguration Test：scope、grant、permission、tenant/org、ping 显式例外、Bean 替换 | PASS |
| R3 用户撤销 | Token Service/Controller：当前 A、tenant、client、USER 类型、幂等、跨用户/跨客户端拒绝 | PASS |
| R4 组织前置 | Authorization Controller：预览与确认均在 validator/code store 前失败 | PASS |
| R5 错误分类 | OAuth DB 故障为 503；MCP 401/Origin 403/基础设施 503；安全日志不含凭据 | PASS |
| R6 加固 | 长度上限、issuer query、SYNC-only、LoginUser 快照、last_used_at 节流、30 天留存清理 | PASS |

### 10.1 回归结果

| 范围 | 结果 |
|---|---|
| Identity | PASS，34 tests（最终复跑后以 execution-log 为准） |
| Capability | PASS，13 tests |
| MCP | PASS，14 tests |
| Control Plane | PASS，20 tests |
| AI | PASS，84 tests；全部为 mock/stub 单测，未调用真实模型 |
| Admin | PASS，39/39 reactor `package -DskipTests` |
| Frontend | PASS，目标 ESLint 无输出；Node 20.19.0 production build 成功 |
| Dependency | PASS，无 SAS、无 Sa-Token OAuth2 |
| XML/Flyway | PASS；Mapper XML 可解析，无 placeholder/tenant 0；唯一物理 DELETE 是带代码注释和 Spec 回滚说明的短期 Token 超期留存任务 |

### 10.2 继续条件跳过

- 未在用户真实 MySQL 执行 V1.0.22；
- 未使用真实 Redis 做 20 路 code 并发兑换；
- 未启动真实 Admin/浏览器执行 metadata → authorize → code → token 全闭环；
- 上述项目不记为 PASS，也不影响本轮 R1–R6 代码修复结论；复审通过前不进入 secure actions。

## 11. 归档验收（2026-07-12）

- Identity：PASS，34/34；
- MCP：PASS，14/14，协议版本 `2025-06-18`，单 `/mcp` Streamable HTTP；
- Admin：PASS，JDK 17 `clean install` 39/39；
- 依赖隔离：PASS，无 Spring Authorization Server、无 Sa-Token OAuth2；
- Mapper 资源唯一性：PASS，`AiCapabilityClientMapper.xml` 只存在于 control-plane；
- 条件跳过保持不变：真实 MySQL、真实 Redis 并发、真实浏览器 OAuth E2E 未执行；
- 结论：归档验收通过，允许进入 `forge-ai-hub-secure-actions` Proposal/Apply。
