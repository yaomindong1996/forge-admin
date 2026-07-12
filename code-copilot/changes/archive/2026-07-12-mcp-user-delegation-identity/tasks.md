# 任务拆分 — MCP 用户委托身份

> 变更名：`mcp-user-delegation-identity`
> 状态：done
> 原则：OAuth/MCP HARD-GATE → 数据模型 → 授权码/令牌 → Forge 上下文 → Streamable HTTP → 验证
> 执行约束：TDD；禁止 commit/push；未确认 Spec 前不写业务代码

## 前置条件

- [x] `forge-ai-hub-control-plane` 已归档为 `done`；
- [x] 读取 MCP `2025-06-18` Authorization 与 Transport 官方规范；
- [x] 确认阶段 2 首个硬闸门是可信 USER/SERVICE 双身份；
- [x] 确认下一 Flyway 版本为 `V1.0.22`；
- [x] 用户确认本 Spec 后进入 Apply。

## Apply 结果快照

| Task | 状态 | 结果 |
|---|---|---|
| 1 | 完成（受限 Profile） | 禁止 SAS；Sa-Token OAuth2 也未引入；依赖树、隔离测试和 Admin JAR 扫描均无对应依赖 |
| 2–4 | 完成 | Identity 模块、V1.0.22、精确 redirect、PKCE code、USER/SERVICE 短期 Token 与实时失效已实现 |
| 5–6 | 完成 | 显式执行身份、双身份审计、MCP 专用认证 Filter 与单 `/mcp` Streamable HTTP 已实现 |
| 7 | 完成 | 简洁授权确认页、登录返回、后端可信 redirect 和前端审计脱敏已实现 |
| 8 | 完成（条件项除外） | 模块测试、聚合打包、前端构建和静态扫描通过；真实 MySQL/Redis/浏览器 E2E 待 Review |

## Review Findings（2026-07-12）

- [x] **R1 / P0**：新增真实 `fdu_` 经 Filter、Identity Lifecycle、MCP initialize/call、双身份审计和 finally 清理的聚合测试；覆盖完整 Sa-Token 排除链。
- [x] **R2 / P0**：把 client grant 和 `LoginUser.permissions` 接入 Token scope/Capability 调用授权，保留 ping 的显式统一授权例外，禁止未来业务能力只凭 Token 自报 scope 放行。
- [x] **R3 / P1**：为 PUBLIC/USER Token 提供安全的当前用户撤销路径，并修正文案/元数据/测试。
- [x] **R4 / P1**：授权请求和确认阶段校验 `user.activeOrgId == client.activeOrgId`，组织不一致不签发 code。
- [x] **R5 / P1**：区分 OAuth 客户端错误与基础设施错误；安全记录异常类型和 requestId，不记录 Token/code/verifier/Secret。
- [x] **R6 / P2**：补参数长度、issuer query、Token 留存清理和 SYNC/跨线程身份恢复加固。

## Task 1：OAuth/MCP 依赖 HARD-GATE Spike

**目标**：在写实现前确定可证明合规的授权服务器方案，禁止手写不完整 OAuth。

**检查文件**：

- `forge-server/forge-framework/forge-dependencies/pom.xml`
- `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/pom.xml`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-mcp/pom.xml`
- `forge-server/forge-admin-server/pom.xml`

**步骤**：

- [x] 检查 Sa-Token OAuth2 `1.38.0` 与 `1.45.0` 的公开模型/handler，不运行真实网络授权；
- [x] 确认两版均不能直接满足 PKCE/resource/PUBLIC Client，拒绝以 Sa-Token OAuth2 冒充 MCP 合规；
- [x] 按用户约束移除 `spring-security-oauth2-authorization-server` 与对应能力测试；
- [x] 建立 Forge OAuth 2.1 Profile 契约测试，锁定 PKCE、resource、redirect、code 单次兑换、PUBLIC/CONFIDENTIAL 与不支持 grant 的失败关闭；
- [x] 运行 dependency tree，证明 Identity 不含 Spring Authorization Server/Sa-Token OAuth2、不改变 Undertow/Spring AI；
- [x] Green 标准：受限 Profile 的实现级 MUST 行为有自动化证据，`/mcp` 已切换为短期令牌；真实 Redis 并发和完整 E2E 作为条件验收项保留。

**命令**：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-capability-identity -am test
```

## Task 2：身份模块与 Flyway 数据结构

**创建/修改文件**：

- Create `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability-identity/pom.xml`
- Create `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability-identity/src/main/java/com/mdframe/forge/plugin/capability/identity/**`
- Modify `forge-server/forge-framework/forge-plugin-parent/pom.xml`
- Modify `forge-server/forge-framework/forge-dependencies/pom.xml`
- Modify `forge-server/forge-admin-server/pom.xml`
- Create `forge-server/db/migration/V1.0.22__add_mcp_user_delegation_identity.sql`

**步骤**：

- [x] Red：表、实体、`@TableLogic`、Mapper XML、唯一键和 tenant 条件按规范实现并完成静态验证；
- [x] 新增 `ai_capability_oauth_redirect_uri`、`ai_capability_access_token`；
- [x] 扩展 client 的 `oauth_enabled/oauth_client_type`，默认关闭 OAuth，历史机器客户端不自动开放 USER 授权；
- [x] 新增 Entity/Mapper/XML，所有租户业务查询显式 `tenant_id` 与 `del_flag=0`；
- [x] access token 仅保存 keyId/prefix/HMAC，Flyway 不插入真实客户端、Token 或 redirect URI；
- [x] 执行 `xmllint`、placeholder、tenant 0、物理 DELETE、无列名 INSERT 和版本唯一检查。

## Task 3：元数据、授权请求与 PKCE Code

**创建文件**：

- `.../identity/oauth/McpProtectedResourceMetadataController.java`
- `.../identity/oauth/McpAuthorizationServerMetadataController.java`
- `.../identity/oauth/CapabilityAuthorizationController.java`
- `.../identity/oauth/DelegationAuthorizationCodeStore.java`
- `.../identity/oauth/RedisDelegationAuthorizationCodeStore.java`
- `.../identity/oauth/OAuthRequestValidator.java`

**步骤**：

- [x] Red：非 S256、缺 resource、错误 redirect、错误 client、错误 scope 均失败关闭；
- [x] 实现 RFC 9728/RFC 8414 元数据，外部 URL 只从受控配置生成；
- [x] 授权请求绑定 client、A、tenant、activeOrg、scopes、resource、redirect、challenge、state；
- [x] code 使用 256 bit 随机值，Redis key 使用服务端 HMAC，TTL 120 秒；
- [x] 使用 Lua 原子 get-and-delete；真实 Redis 20 路并发兑换留作条件验收；
- [x] 拒绝在日志/异常/DB 中出现 code、verifier、普通 Forge Token。

## Task 4：短期访问令牌与实时身份校验

**创建文件**：

- `.../identity/token/CapabilityAccessTokenCodec.java`
- `.../identity/token/CapabilityAccessTokenService.java`
- `.../identity/token/CapabilityTokenController.java`
- `.../identity/token/CapabilityTokenAuthenticator.java`
- `.../identity/security/CapabilitySecurityPrincipal.java`

**步骤**：

- [x] Red：错误 code/verifier/client secret/resource、code 重放、过期、撤销、credentialVersion 变化全部失败；
- [x] authorization_code 生成 USER Token，client_credentials 只为 CONFIDENTIAL 客户端生成 SERVICE Token；
- [x] Token 使用 `fdu_<keyId>_<secret>`，TTL 默认 600 秒、最大 900 秒，HMAC 常量时间比较；
- [x] 每次请求重新加载 `IUserLoadService`，严格校验用户状态、租户成员、当前组织，禁止组织回退；
- [x] SERVICE Token 禁止绑定超级管理员；USER Token 必须是完成当前登录与授权确认的 A；
- [x] revoke、客户端吊销和密钥轮换立即使下一请求失效。

## Task 5：Forge 显式执行身份上下文

**创建/修改文件**：

- Create `forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/context/ExecutionIdentityContextHolder.java`
- Modify `forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/session/SessionHelper.java`
- Modify `forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/aspect/OperationLogAspect.java`
- Test `forge-server/forge-framework/forge-starter-parent/forge-starter-orm/src/test/java/.../InjectionMetaObjectHandlerTest.java`

**步骤**：

- [x] 显式身份上下文正常、异常和嵌套恢复测试通过；ORM 审计经 `SessionHelper` 统一取值；
- [x] `SessionHelper` 优先读取显式身份，普通 Sa-Token 登录行为保持不变；
- [x] 上下文 Scope 保存旧值并在 close/finally 恢复，支持嵌套；
- [x] TenantContext、actor/client/MDC 同步建立和恢复；
- [x] OperationLog 使用 `SessionHelper` 获取 A，不再只检查 `StpUtil.isLogin()`；
- [x] 异步传播使用不可变快照，线程池复用时恢复原上下文。

## Task 6：MCP 专用 OAuth Filter 与 Streamable HTTP

**创建/修改文件**：

- Modify `forge-server/forge-framework/forge-plugin-parent/forge-plugin-mcp/src/main/java/com/mdframe/forge/plugin/mcp/security/ForgeMcpAuthenticationFilter.java`
- Create `.../identity/mcp/CapabilityMcpAccessTokenResolver.java`
- Create `.../identity/mcp/McpExecutionContextLifecycleFilter.java`
- Modify `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/config/SaTokenConfig.java`
- Remove/replace `forge-server/forge-admin-server/src/main/java/com/mdframe/forge/admin/bridge/CapabilityMcpCallerContextResolver.java`

**步骤**：

- [x] MCP 模块协议链证明裸请求 401、长期 fcp Secret 拒绝、受信 fdu 身份可 initialize；完整 Admin 实例 E2E 留作条件验收；
- [x] `/mcp` 排除通用 Sa-Token/API 权限拦截，但专用 OAuth Filter 更早强制认证；
- [x] 401 添加 RFC 9728 `WWW-Authenticate`，scope 不足返回 403；
- [x] 每次 POST/GET 都验证 Bearer，query token 拒绝，Origin 精确校验；
- [x] 请求属性只保存验证后的 Principal/LoginUser，arguments 中身份字段不能覆盖；
- [x] 保持单 `/mcp` Streamable HTTP，拒绝 SSE/STATELESS/stdio。

## Task 7：简洁授权确认页

**创建/修改文件**：

- Create `forge-admin-ui/src/views/ai/mcp-authorize.vue`
- Create `forge-admin-ui/src/api/ai/capability-oauth.js`
- Modify `forge-admin-ui/src/router/index.js`
- Modify `forge-admin-ui/src/router/guards/tab-guard.js`

**步骤**：

- [x] 未登录时携带原始 OAuth 参数跳转登录，登录后只返回授权确认页；
- [x] 页面只展示客户端、请求 scope、当前租户/组织、有效期和同意/拒绝，不做统计卡或装饰设计；
- [x] state 只原样回传给 redirect URI，不进入页面审计敏感参数；
- [x] 同意后使用后端返回的精确 redirect URI 导航，拒绝返回标准 `access_denied`；
- [x] redirect URI 不由前端拼接或信任 query 原值；
- [x] Node 20.19.0 下执行目标 ESLint 与生产 build。

## Task 8：安全回归、文档与 Review

**步骤**：

- [x] 按 `test-spec.md` 完成 Review R1–R6 的 Red/Green、授权交集、撤销和上下文泄漏增量测试；真实 Redis 并发仍为条件验收；
- [x] 重跑 Capability、Control Plane、MCP、AI 与 Admin 39 模块聚合；
- [x] 检查 dependency tree、XML、Flyway、Token/Secret/code/verifier 日志、旧 SSE 和 Nacos 漂移；
- [x] 使用 MockMvc 完成合法 `fdu_` → `/mcp initialize` → list/call → 双身份审计聚合链；metadata → authorize → token 的真实 Redis/DB 全闭环仍为条件验收；
- [x] 当前没有为本变更准备的受控 MySQL/Redis 测试实例，已如实记录跳过，不以 mock 冒充；
- [x] 更新四份变更文档和 decisions；
- [x] 已完成 Spec Compliance Review 和 Code Quality Review；结论 NEEDS_FIX，见 R1–R6；
- [x] Review R1–R6 已修复并完成增量验证；等待再次 `/review`，复审通过前不进入 `forge-ai-hub-secure-actions`。

## Fix 增量任务（2026-07-12）

- [x] R1：真实 Resolver/Filter/Lifecycle/Sa-Token 排除链/MCP/Observer 聚合测试；
- [x] R2：scope + client grant + 当前用户权限 + tenant/org 交集策略及撤销即时失败测试；
- [x] R3：当前登录用户 USER Token 撤销接口，幂等与跨主体拒绝；
- [x] R4：授权预览和确认两处 activeOrg 前置一致性校验；
- [x] R5：OAuth/MCP 业务错误与基础设施错误分级、安全日志；
- [x] R6：参数长度、issuer query、SYNC、不可变快照、last_used_at 节流和 Token 留存清理；
- [x] Identity 34、Capability 13、MCP 14、Control Plane 20、AI 84、Admin 39/39、前端 ESLint/build 全绿；
- [ ] 再次执行 `/review mcp-user-delegation-identity`。
