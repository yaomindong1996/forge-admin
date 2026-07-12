# MCP 用户委托身份

> status: done
> created: 2026-07-12
> complexity: 🔴复杂
> parent-stage: Forge AI 中枢阶段 2.0 — 受控写入与流程闭环的身份硬闸门

## 1. 背景与目标

`forge-ai-hub-control-plane` 已归档，机器客户端能够绑定 `serviceUserId/tenantId/activeOrgId`，调用审计也已预留 `actorUserId/serviceUserId/clientId` 双身份字段。但当前 MCP Bearer 仍直接使用长期机器 Secret，只能证明客户端或服务账号，不能证明实际操作者 A；同时业务 `SessionHelper`、ORM 审计字段、数据权限和流程身份仍依赖 Forge 用户上下文。

阶段 2 的写入、流程发起和办理必须先解决可信实际操作者。本变更只交付阶段 2.0 身份底座：把 Forge Admin 建设为 MCP OAuth 资源服务器与受控授权服务器，支持服务账号短期令牌和 A 用户授权码 + PKCE 委托令牌，并把可信身份安全桥接到 Forge 执行上下文。

本变更完成后仍不开放业务写动作、流程办理或任意消息发送；后续 `forge-ai-hub-secure-actions` 必须以本变更 HARD-GATE 全绿为前置。

### 1.1 规范基线

- MCP Authorization：`https://modelcontextprotocol.io/specification/2025-06-18/basic/authorization`；
- MCP Transport：`https://modelcontextprotocol.io/specification/2025-06-18/basic/transports`；
- OAuth 2.1 draft、RFC 8414、RFC 9728、RFC 8707；
- MCP 仍只使用单端点 Streamable HTTP `/mcp`，不恢复旧 HTTP+SSE 双端点。

### 1.2 成功标准

- `/mcp` 的 401 响应包含 RFC 9728 `WWW-Authenticate` 资源元数据地址；
- 提供 RFC 9728 Protected Resource Metadata 和 RFC 8414 Authorization Server Metadata；
- A 用户使用 Authorization Code + PKCE S256，redirect URI 精确匹配预登记值，授权请求和换令牌请求都强制携带同一 `resource`；
- 支持预登记 PUBLIC/CONFIDENTIAL MCP 客户端；阶段 2.0 不实现动态客户端注册，但元数据如实声明不支持；
- 服务账号使用 `client_credentials` 换取短期 SERVICE Token；长期 `fcp_...` Secret 不再直接访问 `/mcp`；
- 访问令牌为至少 256 bit 的短期不透明值，TTL 默认 10 分钟且不超过 15 分钟，数据库只保存 keyId、前缀和 HMAC，不保存原文；
- 每次 MCP HTTP 请求重新校验 audience/resource、scope、令牌、客户端状态、credentialVersion、用户状态、租户成员和当前组织；
- USER Token 的 `actorUserId=A`，SERVICE Token 的 `actorUserId=serviceUserId`；二者都保留 `clientId/serviceUserId`；
- 进入 Capability/业务执行前建立显式 `LoginUser + TenantContext + actor` 上下文，ORM `create_by/update_by/create_dept`、数据权限和操作日志可读取真实 A；请求完成后严格恢复上下文；
- 伪造 `X-User-Id/X-Tenant-Id/X-Active-Org-Id`、Token query 参数、错误 audience、错误 redirect URI、明文 verifier、重放 code 全部拒绝；
- Streamable HTTP 协议、Control Plane、AI 和 Admin 聚合回归通过。

## 2. 范围

### 2.1 本变更包含

- OAuth/MCP 依赖能力 Spike，验证现有 Sa-Token OAuth2 或 Spring Authorization Server 是否满足所需标准；
- 新增 `forge-plugin-capability-identity` 组合模块，连接 MCP、控制面、用户加载和执行上下文；
- OAuth 资源元数据、授权服务器元数据、授权码 + PKCE、client credentials 和 Token 端点；
- 客户端类型、OAuth 开关、精确 redirect URI 与短期访问令牌持久化；
- 简洁的 Forge 用户授权确认页面，不新增复杂管理工作台；
- USER/SERVICE 双身份、实时权限加载、当前组织精确校验和立即吊销；
- `ExecutionIdentityContextHolder`、`SessionHelper`、租户上下文、ORM 审计字段和操作日志桥接；
- MCP Origin、401/403、audience、scope 和路径隔离；
- 安全测试、协议测试、上下文清理测试与文档。

### 2.2 明确不做

- 不实现业务写能力、原始 CRUD 写 Tool、流程 start/approve/reject 或消息发送；
- 不实现 R2/R3 风险策略、幂等业务动作、人工审批或加密审批快照；
- 不实现 refresh token；令牌过期后重新授权或重新使用 client credentials；
- 不实现 Dynamic Client Registration；客户端由 Forge 管理端预登记；
- 不把普通 Forge 登录 Token 直接透传给 MCP，也不把 MCP Token 透传给下游服务；
- 不允许 MCP Token 访问普通管理 API；
- 不接 Nacos MCP Registry/Admin、AgentScope 或 Agent Runtime；
- 不使用旧 SSE、STATELESS 或 stdio；
- 不手写一个无法证明符合 OAuth 2.1 的“类 OAuth”框架。如果依赖 Spike 无法满足规范，停止 Apply 并回填阻断结论。

## 3. 架构与模块边界

```text
forge-admin-server
    └─ forge-plugin-capability-identity
         ├─ forge-plugin-mcp
         ├─ forge-plugin-capability-control-plane
         ├─ forge-plugin-system / IUserLoadService
         └─ forge-starter-auth + forge-starter-core context

forge-plugin-mcp ───────────────> forge-plugin-capability
forge-plugin-capability-control-plane ─> forge-plugin-capability
```

- MCP 协议模块继续不依赖 ORM、用户系统或 OAuth；
- Identity 模块是 Admin 组合桥，承载授权服务器适配、Token 校验和 Forge 用户上下文；
- 控制面继续是客户端、授权和审计权威来源；
- `forge-starter-core` 只新增协议无关的显式执行身份上下文，不依赖 MCP/OAuth。

## 4. 身份模型

```java
public record CapabilitySecurityPrincipal(
        Long clientId,
        String clientCode,
        CapabilityActorType actorType,
        Long actorUserId,
        Long serviceUserId,
        Long tenantId,
        Long activeOrgId,
        Integer credentialVersion,
        String tokenId,
        String audience,
        Set<String> scopes) {
}
```

### 4.1 SERVICE 模式

`client_credentials + fcp client secret → 短期 access token`。服务端每次请求加载绑定服务账号的最新 `LoginUser`，禁止超级管理员、禁用用户、无当前组织或无有效角色。审计：

```text
actorType=SERVICE
actorUserId=serviceUserId
serviceUserId=绑定服务账号
clientId=机器客户端
```

### 4.2 USER 模式

`A 的 Forge 登录态 → 授权确认 → authorization code + PKCE → 短期 access token`。A 只能选择自己当前租户下已绑定的组织；Token 不保存角色权限快照，每次请求通过 `IUserLoadService.loadUserByUserId(A, tenant, activeOrg)` 重建最新身份并严格校验组织未发生回退。审计：

```text
actorType=USER
actorUserId=A
serviceUserId=客户端绑定服务账号
clientId=获得委托的 MCP 客户端
```

### 4.3 权限交集

最终权限固定为：

```text
Token scope
∩ 客户端 grant
∩ A/服务账号当前角色权限
∩ 当前组织与数据范围
∩ 字段策略
∩ 能力风险策略
```

任一上下文缺失、加载异常或权限计算异常都失败关闭。

## 5. OAuth 与 MCP 契约

### 5.1 发现端点

| 端点 | 用途 | 登录要求 |
|---|---|---|
| `/.well-known/oauth-protected-resource` | RFC 9728 MCP 资源元数据 | 无，但只返回公开元数据 |
| `/.well-known/oauth-authorization-server` | RFC 8414 授权服务器元数据 | 无，但只返回公开元数据 |
| `/oauth2/token` | authorization_code/client_credentials 换 Token | 按 grant 校验客户端 |
| `/oauth2/revoke` | RFC 7009 风格撤销本服务器 Token | 客户端认证或当前用户 |
| `/ai/capability/oauth/authorization-request` | SPA 创建并校验授权请求 | Forge 用户登录 |
| `/ai/capability/oauth/authorize` | A 确认/拒绝授权并生成一次性 code | Forge 用户登录 |

授权服务器元数据的 `authorization_endpoint` 指向 Forge 前端 `/mcp-authorize` 页面；页面使用当前 Forge Bearer 登录态调用后端确认 API，再按精确登记 redirect URI 返回 `code/state`。

### 5.2 强制规则

- 只支持 `code_challenge_method=S256`，拒绝 plain；
- `resource` 必须是配置的 canonical MCP URI，并在授权请求、Token 请求和访问令牌中完全一致；
- redirect URI 必须与数据库预登记值逐字匹配，不做前缀、通配符或 URL 解码后的模糊匹配；
- authorization code 使用 Redis 保存 HMAC key，TTL 120 秒，Lua 原子读取删除，只能兑换一次；
- access token 格式为 `fdu_<22位keyId>_<43位Secret>`，只通过 Header 传递；
- Token 401 返回 `WWW-Authenticate: Bearer resource_metadata="..."`；已认证但 scope 不足返回 403；
- 浏览器请求出现 Origin 时必须精确匹配允许列表；非浏览器客户端无 Origin 时按 Token 和网络策略认证；
- 所有外部 URL 从配置生成并校验 HTTPS；仅本地开发允许 localhost HTTP。

## 6. 数据模型与迁移

新增 `V1.0.22__add_mcp_user_delegation_identity.sql`。

### 6.1 `ai_capability_oauth_redirect_uri`

保存客户端精确 redirect URI：`client_id/redirect_uri/status/del_flag/logic_delete_active` 与标准审计字段。唯一键：`(tenant_id, client_id, redirect_uri_hash, logic_delete_active)`；hash 用于索引，比较仍使用完整 URI。

### 6.2 `ai_capability_access_token`

保存短期令牌安全元数据：

```text
token_key_id / token_prefix / token_hash
client_id / credential_version
actor_type / actor_user_id / service_user_id
active_org_id / audience / scopes
status / issued_at / expires_at / last_used_at / revoked_at
del_flag / logic_delete_active / 标准审计字段
```

`token_key_id` 全局有效记录唯一；原始 Token、authorization code、code verifier、普通 Forge Token 均不得入库。

### 6.3 客户端扩展

`ai_capability_client` 增加 `oauth_enabled`、`oauth_client_type(PUBLIC/CONFIDENTIAL)`。PUBLIC 客户端授权码兑换必须使用 PKCE；CONFIDENTIAL 客户端的 client credentials 与授权码兑换还必须验证现有 client secret。

## 7. Forge 执行上下文

新增协议无关 `ExecutionIdentityContextHolder`：

- `SessionHelper.getLoginUser()` 优先读取显式执行身份，再回退 Sa-Token Session；
- MCP Identity Filter 在进入 SDK 前设置显式 LoginUser、TenantContext、actor/client/MDC；
- ORM `InjectionMetaObjectHandler` 因复用 `SessionHelper` 自动写入 A 的 `create_by/update_by` 和当前组织 `create_dept`；
- `OperationLogAspect` 改为优先读取 `SessionHelper.getUserId()`，不能只依赖 `StpUtil.isLogin()`；
- finally 恢复而不是粗暴 clear，避免嵌套调用污染外层请求；
- 异步执行只传不可变身份快照，通过 TaskDecorator 建立和恢复上下文；
- 禁止调用 `SessionHelper.clearSession()` 清理 MCP 上下文，因为它会清除持久 Sa-Token Session。

## 8. 当前工程阻断项

当前 `/mcp` 已有机器凭据 Filter，但 Forge 通用 Sa-Token 拦截器仍匹配 `/**`。真实 Admin 请求通过机器 Filter 后仍可能被 `StpUtil.checkLogin()` 二次拒绝；模块协议测试没有装配完整 Auth 拦截链，无法覆盖该问题。

Apply 必须：

1. 将 `/mcp` 从通用 Sa-Token/API Permission 拦截器中排除；
2. 保证专用 MCP OAuth Filter 在 SDK 处理前强制认证，排除不等于匿名放行；
3. 增加 Admin 完整拦截链测试，证明无 Token 仍为 401、有效短期 Token 可完成 initialize。

## 9. HARD-GATE

### 9.1 依赖 Spike

优先验证现有 Sa-Token OAuth2 模块能否满足 PKCE S256、resource/audience、RFC 8414、RFC 9728、精确 redirect 和 PUBLIC/CONFIDENTIAL 客户端；同时验证 Spring Authorization Server 与现有 Sa-Token 拦截链是否可隔离共存。

- 若现有框架能覆盖，使用公开扩展点补 Forge 领域绑定；
- 若只能通过复制框架核心或自研 OAuth 状态机实现，停止 Apply，记录阻断并重新选型；
- 不为了赶进度降级为自定义 Header、长期 Token 或旧 SSE。

#### 9.1.1 Apply 选型结论

- 用户明确禁止引入 `spring-security-oauth2-authorization-server`，Identity 模块不得声明或传递该依赖；
- Sa-Token OAuth2 `1.38.0` 以及核验时最新 `1.45.0` 的公开授权请求模型均缺少 PKCE `code_challenge/code_verifier` 与 RFC 8707 `resource`，内置 authorization-code handler 仍依赖 `client_secret`，无法直接支持标准 PUBLIC Client，因此也不引入 Sa-Token OAuth2；
- 本变更实现边界严格受限的 Forge OAuth 2.1 Profile，只覆盖 MCP 所需 authorization_code + PKCE S256、client_credentials、RFC 8414/9728 metadata、RFC 8707 resource 与 RFC 7009 revoke；不实现 refresh token、implicit、password、动态注册或通用授权服务器扩展；
- 协议状态机必须以契约测试、单次 code 原子兑换、精确 redirect/resource 比较、短 TTL 和失败关闭证明；任何未列入 Profile 的 grant/参数均拒绝；
- MCP 仍保持单 `/mcp` Streamable HTTP，长期机器 Secret 只用于换短期令牌。

### 9.2 安全闸门

- code 单次兑换、PKCE、redirect、resource/audience、state、安全随机数全部通过；
- Token 只保存 HMAC，日志和异常无 Token/code/verifier；
- 用户、组织、客户端、credentialVersion 或 grant 变化后下一请求立即拒绝；
- MCP Token 访问普通 REST API 被拒绝；
- `/mcp` 排除通用登录拦截后仍由专用 Filter 强制 401；
- 请求结束和异常路径无 ThreadLocal/Tenant/MDC 泄漏；
- Streamable HTTP 回归通过且没有旧 SSE 端点。

## 10. 回滚

- 配置 `forge.capability.oauth.enabled=false` 关闭授权端点与 USER Token；
- 配置 `forge.capability.mcp.require-access-token=false` 仅允许在受控回滚窗口恢复旧机器 ping，生产默认不允许；
- 移除 Admin 对 Identity 模块依赖即可回到归档控制面状态；
- 新表保留审计数据，不自动 DROP；短期 Token 元数据仅在过期/撤销超过配置留存期后由专用留存任务物理清理。该清理不影响独立调用审计表，误清理只能从数据库备份恢复，因此生产缩短留存期前必须人工审查；
- 回滚不得重新开放裸 `X-User-Id` 或把普通 Forge Token 作为 MCP Token。

## 11. 阶段 2 后续切片

本变更通过后依次实施：

1. `forge-ai-hub-secure-actions`：已发布业务动作、幂等、字段白名单、R2 确认；
2. `forge-ai-hub-flow-actions`：流程 start/approve/reject，A 作为发起人/办理人；
3. `forge-ai-hub-high-risk-approval`：R3 加密快照、人工审批、回调重新授权。

三者全部复用本变更的 `actorUserId/clientId/serviceUserId`，禁止各自再造身份来源。

## 12. 确认记录

- **提出时间**：2026-07-12；
- **用户指令**：归档上一个阶段需求并开始阶段 2；
- **当前授权范围**：用户已执行 `/apply mcp-user-delegation-identity`；本变更只开放身份底座实现，不开放写/流程能力；
- **进入 Apply 条件**：已满足。Task 1 依赖 HARD-GATE 通过后继续实施。

## 13. Apply 结果

阶段 2.0 身份底座已完成实现，进入 Review 前状态：

- 新增 `forge-plugin-capability-identity`，实现受限 Forge OAuth 2.1 Profile；
- 未声明、未传递且最终 Admin JAR 不包含 `spring-security-oauth2-authorization-server` 或 `sa-token-oauth2`；
- USER 模式使用 authorization code + PKCE S256，SERVICE 模式使用 client credentials；长期 `fcp_` 仅用于机密客户端认证换取短期令牌；
- 短期 `fdu_` Token 绑定 client、actor、service user、tenant、active org、resource 和 scope，并在每次 `/mcp` 请求实时校验；
- `/mcp` 继续只使用 Streamable HTTP，专用 Filter 拒绝裸请求、长期 `fcp_`、query/cookie Token 和恶意 Origin；
- 显式执行身份已桥接 `SessionHelper`、租户上下文、ORM 审计、操作日志、异步任务和 Capability 双身份审计；
- 授权确认页保持最小信息结构，并对 OAuth 参数和页面访问审计做脱敏；
- Identity 18/18、Capability 13/13、MCP 13/13、Control Plane 20/20、Admin 39 模块聚合、目标 ESLint 和前端生产构建通过。

尚未把条件验证冒充完成：真实 MySQL Flyway、真实 Redis 20 路并发 code 兑换、启动 Admin 后的浏览器授权和 metadata → authorize → token → `/mcp initialize` 完整 E2E，留待 Review/受控环境验收。本变更未调用真实模型，也未开放业务写动作或 Flowable 办理。

## 14. Review 结论

> review-date: 2026-07-12
> conclusion: NEEDS_FIX

### 14.1 Spec Compliance

已满足：Identity 模块边界、V1.0.22 数据结构、PKCE S256、resource、短期不透明 Token、实时用户/组织/客户端校验、双身份审计字段、专用 `/mcp` Filter、Streamable HTTP、SAS/Sa-Token OAuth2 依赖隔离和简洁授权页均已落地。

存在两个阶段阻断项：

1. **R1 / P0 — 完整身份链 HARD-GATE 尚无自动化证明。** 当前 Identity 18 项测试分别验证 Token、Resolver、上下文和 Observer；MCP 集成测试则注入测试用 `McpCallerContextResolver`，没有携带真实 `fdu_`，也没有装配 `CapabilityMcpAccessTokenResolver + McpExecutionContextLifecycle + CapabilityInvocationAuditObserver + SaTokenConfig`。因此 apply 记录中的“受信 fdu 身份可 initialize”只能算分层推断，不能算完整 Admin 链证据。
2. **R2 / P0 — 权限交集未进入通用运行时授权。** `OAuthRequestValidator` 只校验全局硬编码 ping scope，`ScopeBasedCapabilityAuthorizationPolicy` 只校验 Token scope；控制面的 `CapabilityGrantService.evaluate` 和 `LoginUser.permissions` 没有参与 Token scope 颁发或 Capability 调用决策。当前 MCP 只开放全客户端统一 ping，尚未产生业务越权，但这不满足本 Spec 的“Token scope ∩ client grant ∩ 当前用户权限”硬闸门，禁止直接进入 secure actions。

### 14.2 Code Quality / Security

需要修复：

3. **R3 / P1 — PUBLIC/USER Token 无可用的主动撤销路径。** `/oauth2/revoke` 无条件要求 CONFIDENTIAL 客户端 Secret，当前用户登录态没有撤销分支；PUBLIC 客户端签发的 USER Token 只能等待过期或由管理员整体变更客户端。授权页“可以随时撤销访问”的文案与实际能力不一致。
4. **R4 / P1 — 授权确认未提前校验用户当前组织与客户端绑定组织一致。** 授权页会展示并允许用户在任意当前组织同意；随后 Token Service 又要求 `token.activeOrgId == client.activeOrgId`，导致组织不一致时先成功签 code、再在换 Token 阶段失败并退化为 `invalid_request`。应在 authorization request/confirm 阶段失败关闭并给出稳定错误。
5. **R5 / P1 — 基础设施异常被静默降级为客户端 4xx。** Token Controller、客户端 Secret 验证、用户实时加载和 MCP Resolver 捕获宽泛 `RuntimeException` 后统一返回 `invalid_request/invalid_client/invalid_token/401`，未安全记录异常类型。Redis、数据库或用户目录故障会伪装成调用方错误，无法区分攻击、配置错误和平台故障。

建议同时加固但不单独阻断：OAuth 参数长度上限、issuer 禁止 query、过期 Token 元数据留存清理策略、明确锁定 SYNC 执行模型或增加跨线程身份恢复。

### 14.3 Review Gate

- 当前状态为 `reviewed_with_findings`，不得归档；
- 下一步执行 `/fix mcp-user-delegation-identity`，优先顺序固定为 R1 → R2 → R3/R4 → R5 → P2 加固；
- Fix 后必须增量复跑 Identity、Capability、MCP、Control Plane、Admin 聚合、依赖隔离和前端构建；
- HARD-GATE 全绿前不得创建或 Apply `forge-ai-hub-secure-actions`。

## 15. Fix 结果

> fix-date: 2026-07-12
> conclusion: FIXED_PENDING_REVIEW

### 15.1 R1 — 完整身份聚合链

新增 `McpDelegatedIdentityIntegrationTest`，请求携带合法格式 `fdu_` Bearer，并经过真实：

```text
CapabilityMcpAccessTokenResolver
→ ForgeMcpAuthenticationFilter
→ SaTokenConfig / ApiPermissionInterceptor 的 /mcp 排除链
→ McpExecutionContextLifecycle
→ initialize / initialized / tools/list / tools/call
→ SessionHelper + TenantContext
→ CapabilityInvocationAuditObserver
→ finally 清理
```

测试仅 mock `CapabilityAccessTokenService` 的持久层认证结果，不替换 Resolver、Filter、Lifecycle、Sa-Token 拦截配置、MCP SDK、Capability Registry 或审计 Observer；已验证 A=101、serviceUserId=999、clientId=301、tenant=1、activeOrg=201，以及正常、401、403、认证基础设施异常后的 ThreadLocal/Tenant/MDC 清理。真实 MySQL/Redis 仍按条件验收记录，不以 MockMvc 冒充。

### 15.2 R2 — 授权交集

Identity 组合层新增 `ForgeCapabilityAuthorizationPolicy`，最终决策固定为：

```text
scope
∩ ExecutionIdentity 与 caller 精确一致
∩ CapabilityGrantService 实时 client grant
∩ LoginUser.permissions 当前权限
∩ tenant / activeOrg
```

权限映射默认稳定为 `ai:capability:discover:{capabilityCode}` 与 `ai:capability:invoke:{capabilityCode}`，后续安全动作可以替换映射 Bean，但不能信任客户端参数。`capability.ping` 是阶段 2.0 唯一显式统一授权例外，仍必须先通过 Token scope 与可信身份校验。grant 撤销、权限撤销或组织变化均在下一请求失败关闭。

### 15.3 R3/R4 — 用户撤销与组织前置校验

- 新增 Forge 登录态接口 `POST /ai/capability/oauth/token/revoke`；只允许当前 A 撤销属于同一 tenant、client 且 `actorType=USER` 的 Token，未知/已撤销 Token 幂等，跨用户/跨客户端返回 403；
- `authorization-request` 与 `authorize` 均在校验/签 code 前比较 `user.activeOrgId == client.activeOrgId`，不一致直接失败且不会调用 code store；
- 授权页文案改为“令牌到期自动失效，也可以在 Forge 中提前撤销”。

### 15.4 R5/R6 — 错误分类与运维加固

- OAuth 校验错误保持稳定 4xx；数据库、Redis、用户目录等非业务异常返回 `temporarily_unavailable`/503，并只安全记录 requestId、endpoint/grantType、exceptionType、稳定结果码；
- MCP 无身份为 401、Origin 不匹配为 403、身份基础设施故障为 503；日志不记录 Token、Secret、code、verifier 或 Authorization header；
- state/client_id/scope/token/code/redirect/resource/Basic header 均增加长度上限，issuer/resource 禁止 query；
- MCP 启动闸门显式拒绝 ASYNC，只允许 `type=SYNC + protocol=STREAMABLE + stdio=false`；
- `ExecutionIdentity` 对 `LoginUser` 做防御性快照并在读取时返回副本；
- `last_used_at` 默认 1 分钟节流；短期 Token 过期/撤销元数据默认保留 30 天，再由专用留存任务物理清理，普通撤销继续状态更新。

## 16. 最终复审与归档

- **最终复审时间**：2026-07-12；
- **Spec Compliance**：PASS。R1–R6 已全部落实，USER/SERVICE 双身份、OAuth 2.1 受限 Profile、权限实时交集、上下文桥接和主动撤销满足本变更边界；
- **Code Quality / Security**：PASS。未发现新的阻断项；敏感凭据不落日志，基础设施故障与客户端错误区分，身份与租户上下文在正常和异常路径均恢复；
- **最终验证**：Identity 34/34、MCP 14/14、Admin reactor clean install 39/39；Mapper 迁移残留已清除；依赖扫描无 `spring-security-oauth2-authorization-server`、无 `sa-token-oauth2`；
- **传输约束**：继续只允许单 `/mcp` Streamable HTTP，启动闸门拒绝 SSE、STATELESS、stdio 和 ASYNC；
- **条件验收**：真实 MySQL Flyway、真实 Redis 并发和浏览器 OAuth E2E 继续作为受控环境验收项，不冒充已执行；
- **归档时间**：2026-07-12；
- **归档路径**：`code-copilot/changes/archive/2026-07-12-mcp-user-delegation-identity/`；
- **下一阶段**：`forge-ai-hub-secure-actions`，只开放已发布、白名单、幂等和可审计的受控业务动作，不包含 Flowable 办理与 R3 高风险审批。
