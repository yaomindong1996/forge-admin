# 测试规格 — 统一能力开放平台

> 变更目录：`code-copilot/changes/unified-capability-open-platform/`
> 依据：`code-copilot/rules/automated-testing-standard.md`（增量优先、证据优先）
> 创建：2026-07-31（本变更首个测试轮次，无历史 test-spec 可复用）

## 1. 变更概述

将 Forge Capability 底座通过 REST 开放网关 `POST /openapi/v1/capabilities/{capabilityCode}/invoke` 开放给外部机器客户端。涉及：

- 后端新模块：`forge-starter-openapi-security`（防重放/限流/幂等通用组件）、`forge-plugin-capability-identity`（客户端凭据与签名认证）、`forge-plugin-capability-open-gateway`（网关认证 + 九步编排）
- 控制面扩展：客户端/授权/发布/调用日志管理接口（control-plane）
- Flyway：`V1.0.74__capability_open_gateway.sql`（表/字典/菜单/权限）+ `V1.0.75__add_capability_registration_dict.sql`（流程动作字典）+ `V1.0.76__add_capability_external_user_delegation.sql`（客户端主体模式与外部身份映射）
- 前端：`forge-admin-ui/src/api/ai/capability.js` + `views/ai/capability/{catalog,client,grant,invocation}.vue` + 能力注册弹窗
- 清理任务：幂等快照过期清理

## 2. 验证范围与优先级

### P0（必须通过）

| 编号 | 验证项 | 方式 | 命令/位置 |
|------|--------|------|-----------|
| P0-1 | OpenApiReplayGuard 单测：appId 缺失 401 / 时间戳超窗 401 / nonce 格式非法 401 / nonce 重复 401 / Redis 不可用与异常失败关闭 503 / 正常放行 | JUnit5 + Mockito | `forge-starter-openapi-security` → `OpenApiReplayGuardTest` |
| P0-2 | CapabilityInvokeOrchestrator 单测：scope 缺失 FORBIDDEN / 授权目录不可用 INTERNAL_ERROR 503 / ACTOR_TYPE_NOT_ALLOWED / 权限缺失 FORBIDDEN / RATE_LIMITED 429 / 写操作缺 Idempotency-Key SCHEMA_INVALID / 幂等命中 idempotentHit / payload 非法顶层字段 SCHEMA_INVALID / READ_ONLY 跳过幂等 / 写路径成功 SUCCESS | JUnit5 + Mockito | `forge-plugin-capability-open-gateway` → `CapabilityInvokeOrchestratorTest` |
| P0-3 | Admin 聚合生产打包 | `JAVA_HOME=... mvn -Penable-tests -pl forge-admin-server -am package -Dforge.tests.skip=true`（forge-server 根） | 47 个 Reactor 模块成功 |
| P0-4 | 本变更 Flyway 脚本静态检查（无占位符） | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.7{4,5,6}__*.sql` | 应无输出；全目录存在 V1.0.72 历史占位符，不能作为本变更失败依据 |
| P0-5 | 前端构建 | `nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`（forge-admin-ui） | 构建成功 |
| P0-6 | 前端 Lint（API、4 页面、注册组件） | `pnpm exec eslint src/api/ai/capability.js src/views/ai/capability/*.vue src/views/ai/capability/components/*.vue` | 零错误 |
| P0-7 | 控制面授权候选项、客户端凭据和 ACTION/FLOW 授权策略 | JUnit5 + Mockito，4 个目标测试类 | 23 条通过 |
| P0-8 | 流程能力注册来源校验 | `FlowActionSourceServiceTest` + `FlowActionCapabilityPublisherTest` | 5 条通过 |
| P0-9 | 签名 AppId 稳定性与网关认证 | `OpenGatewayAuthenticatorTest` | 2 条通过；数值 clientId 同时作为 OAuth client_id 与签名 AppId |

### P1（视环境执行，不可用则记录跳过原因）

| 编号 | 验证项 | 方式 | 前置条件 |
|------|--------|------|----------|
| P1-1 | 端到端：启动 admin 服务 → Flyway V1.0.74 迁移成功 | 启动日志 + `forge_schema_history` | 本地 MySQL/Redis 可用 |
| P1-2 | 端到端：创建客户端 → 授权能力 → HMAC 签名 curl 调用 `/openapi/v1/capabilities/{code}/invoke` 走通 8 错误码契约 | curl | 本地 MySQL/Redis 可用 + 存在已发布能力 |
| P1-3 | 控制台 4 页面（能力目录/机器客户端/能力授权/调用日志）人工走查 | 浏览器 | 前后端均启动 |

## 3. 测试环境与约束

- Java 17：`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home`
- 单测使用 Mockito（spring-boot-starter-test 已在两模块 pom 中），不依赖 MySQL/Redis
- 默认不跑全量 `mvn test`，仅跑本变更两个模块的单测

## 4. 结果记录

执行证据统一记录到同目录 `execution-log.md`。

## 5. 2026-08-01 前端可用性修复增量验证

- 能力目录新增业务动作/流程动作可视化注册，不暴露原始 Schema JSON。
- 机器客户端改用服务账号选择器和组织绑定下拉，认证模式字典缺失时阻止无效提交。
- 客户端创建按 OAUTH/SIGNATURE 模式同步 OAuth 启用状态，覆盖默认 OAuth 可用与纯签名关闭分支。
- 客户端数值 ID 统一作为 OAuth `client_id` 和签名 `X-Forge-App-Id`，OAuth 密钥轮换不再改变签名身份标识。
- 授权候选项改为同权限专用接口，结构化生成字段/操作白名单，ACTION/FLOW 不再被运行时只读策略误拒绝。
- 流程对象选择后实时校验已发布主流程绑定；非平台托管对象不允许注册 START。
- 自动化和静态验证均通过；真实数据库迁移、登录态浏览器操作和外部调用 E2E 保留为部署门禁。

## 6. 2026-08-01 能力注册接口 404 增量验证

- 根因：业务动作/流程动作的管理控制面 Controller 与运行时执行 Bean 共用 `enabled` 条件，默认关闭外部网关时路由也被移除。
- P0：`FlowActionAutoConfigurationTest` 验证开关关闭时保留来源/发布 Bean，不装配执行日志与流程执行适配器；开启时执行 Bean 正常装配。
- P0：`SecureActionAutoConfigurationTest` 验证开关关闭时保留业务动作发布 Bean，不装配 MCP 目录/Handler/工具贡献者；开启时运行时 Bean 正常装配。
- P0：两个 Controller 不再携带运行时 `@ConditionalOnProperty`，前端 404 显示明确的 Admin 更新/重启提示。
- P0：目标模块测试、Admin 聚合编译、前端定向 ESLint 和 `git diff --check`。
- P1：用户重启当前 Admin 进程后，带登录态调用 `GET /ai/capability/flow-action/registration-source`，预期从 404 变为成功数据或可解释的业务校验错误。

## 7. 2026-08-01 外部用户无绑定委托与能力文档增量验证

- P0：`CapabilityClientServiceTest` 覆盖 USER_DELEGATION 不需要 serviceUserId/activeOrgId、拒绝 SIGNATURE，以及 SERVICE/HYBRID 仍强制绑定。
- P0：外部 JWT 验证测试覆盖受信 RS256、未知 issuer、错误 audience、过期 Token、JWK/用户目录失败关闭、首次手机号匹配、姓名不一致、issuer+sub 稳定映射。
- P0：系统用户目录按已验证手机号最多读取两条有效候选；零条或重复候选均返回 `invalid_grant`，禁止任意选取第一条。
- P0：Token Exchange 测试覆盖机密客户端认证、USER_DELEGATION 模式、USER Token 签发及 `/oauth2/userinfo` 身份返回。
- P0：`CapabilityAccessTokenServiceTest` 与流程动作测试覆盖 USER 身份 serviceUserId 可空，SERVICE 身份仍强制服务账号。
- P0：能力 OpenAPI 文档测试覆盖发布版本 Schema、认证方式、幂等头、错误码、actor type，且不包含密钥字段。
- P0：V1.0.76 防重复/tenant_id/主键墓碑静态检查，Identity/Control Plane/Flow Actions 目标模块测试，Admin 聚合编译，前端定向 ESLint 与生产构建。
- P1：用户配置真实 OIDC issuer/JWK、MySQL/Redis 后执行 JWT Token Exchange → userinfo → FLOW_ACTION START/APPROVE，并验证非办理人被拒绝。

## 8. 2026-08-01 Capability Pepper 自动引导增量验证

- P0：Starter Crypto 测试覆盖三个 Pepper 首次生成、32 字节随机强度、互不相同和跨重启复用。
- P0：旧版 `crypto.properties` 不含 Pepper 时，在文件锁内原子补齐并保留既有 Crypto 密钥。
- P0：环境变量/JVM 参数对三个 Pepper 逐项覆盖；`application.yml` 空占位符不得覆盖持久化值。
- P0：显式关闭 `forge.crypto.bootstrap.enabled` 时不生成文件或 Pepper，Capability 既有启动校验继续失败关闭。
- P0：SpringApplication 集成测试确认配置绑定前可见三个属性；Identity 全量测试和 Admin 47 模块聚合编译回归。
