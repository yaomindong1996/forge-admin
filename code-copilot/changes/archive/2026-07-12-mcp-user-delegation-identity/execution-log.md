# 执行日志 — MCP 用户委托身份

> change: `mcp-user-delegation-identity`
> status: done
> created: 2026-07-12

## 1. 时间线

| 时间 | 阶段 | 事件 | 结论 |
|---|---|---|---|
| 2026-07-12 | Archive | 归档 `forge-ai-hub-control-plane` | 状态 done，验证证据与跳过项完整保留 |
| 2026-07-12 | Direction | 用户要求开始阶段 2 | 用户委托身份作为受控写入/流程前置硬闸门 |
| 2026-07-12 | Research | 读取工程 LoginUser、SessionHelper、Sa-Token、ORM 审计和 MCP Filter | 当前只能证明服务账号，显式执行身份上下文尚不存在 |
| 2026-07-12 | Official Spec | 读取 MCP `2025-06-18` Authorization/Transport | 用户委托必须采用 OAuth 2.1 + PKCE + RFC 9728/8414 + resource audience |
| 2026-07-12 | Finding | 完整 Auth 拦截链仍匹配 `/mcp` | 机器 Filter 通过后可能被 StpUtil 二次拒绝，列为 Apply P0 |
| 2026-07-12 | Proposal | 创建阶段 2.0 四份文档 | 仅文档，不写业务代码，不开放写/流程能力 |
| 2026-07-12 | Apply | 用户执行 `/apply mcp-user-delegation-identity` | 进入实现，仍不开放业务写与流程动作 |
| 2026-07-12 | HARD-GATE | 反编译检查 Sa-Token OAuth2 `1.38.0` | 缺 PKCE/resource/PUBLIC Client，拒绝采用 |
| 2026-07-12 | HARD-GATE 探索 | 临时引入 Spring Authorization Server `1.5.6` 运行公开 API 能力测试 | Reactor 28/28 SUCCESS，仅作为探索证据，随后按用户要求完整撤回 |
| 2026-07-12 | Direction | 用户明确禁止 `spring-security-oauth2-authorization-server` | Identity POM 与测试立即移除该依赖，重新打开实现 HARD-GATE |
| 2026-07-12 | HARD-GATE | 核验 Sa-Token OAuth2 `1.45.0` | 仍缺 PKCE/resource，authorization code 内置 handler 仍依赖 client_secret，不采用 |

## 2. 已确认决策

- 阶段 2 分切片实施；用户委托身份属于阶段 2，但作为第一个独立可验收变更；
- 采用 MCP 官方 OAuth 授权边界，不信任身份 Header，不把普通 Forge Token 透传给 MCP；
- 长期机器 Secret 只用于换短期 SERVICE Token，不直接访问 MCP；
- A 使用 authorization code + PKCE 获取 USER Token；
- Token audience 固定为 canonical `/mcp` resource，每次 HTTP 请求重新验证；
- 通过协议无关显式上下文桥接 SessionHelper/ORM/日志，默认登录链保持不变；
- 本变更不实现任何业务副作用，HARD-GATE 通过后再建设 secure actions/flow actions。
- OAuth 实现采用边界受限的 Forge OAuth 2.1 Profile；Spring Authorization Server 与 Sa-Token OAuth2 均不进入运行依赖。

## 3. Proposal 验证

| 检查 | 结果 |
|---|---|
| 上一变更归档目录与四份文档 | PASS |
| MCP 官方 Authorization/Transport 内容读取 | PASS，规范版本 `2025-06-18` |
| 当前 LoginUser/SessionHelper/ORM/OperationLog 调用链检查 | PASS，已定位上下文桥接点 |
| 当前 MCP/Auth 拦截链检查 | 发现 P0，已进入 Spec/Tasks/Test Spec |
| 下一 Flyway 版本 | `V1.0.22` |
| 业务代码/数据库/服务变更 | 无；Proposal 阶段未执行 |

## 4. Apply HARD-GATE 探索记录

执行命令：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-capability-identity \
  -am test
```

| 阶段 | 结果 |
|---|---|
| 临时 Red | 未引入 SAS 依赖时探索性测试 `testCompile` 失败，缺少 OAuth2/SAS 类型 |
| 临时 Green | 2026-07-12 08:47:35 +08:00 完成；Identity 1/1、MCP 13/13、Control Plane 20/20，Reactor 28/28 SUCCESS |
| 撤回 | 用户禁止该依赖后已删除 Identity POM 依赖和 `AuthorizationFrameworkCapabilityTest`，临时 Green 不作为最终选型 |

## 5. 当前状态

OAuth/MCP Apply 已完成，等待 Review。运行时不包含 SAS/Sa-Token OAuth2；`/mcp` 已切换为短期 `fdu_` Token，长期 `fcp_` 仅用于机密客户端换 Token。

## 6. Apply 实现记录

- 创建 `forge-plugin-capability-identity` 并接入 plugin parent、BOM 和 Admin；
- 创建 V1.0.22，新增 redirect URI、access token 表及客户端 OAuth 字段；
- 实现 RFC 8414/9728 metadata、authorization code + PKCE S256、client credentials、resource、revoke；
- 实现 256-bit 不透明短期 Token、HMAC 持久化、实时用户/组织/客户端校验和立即失效；
- 实现显式执行身份、租户/MDC/异步恢复、OperationLog/ORM 取值和 Capability 双身份审计落库；
- `/mcp` 只接受 Header Bearer `fdu_`，拒绝 `fcp_`、query/cookie Token 与非白名单 Origin；
- 创建简洁授权页并对 OAuth 参数、Token、code、verifier 和 state 做前端审计脱敏；
- 收尾删除已失效且仍引用旧长期 `fcp_` Resolver 的 Admin 测试文件。

## 7. Apply 验证记录

执行时间：2026-07-12 09:34–09:41 +08:00。

| 命令/检查 | 结果 |
|---|---|
| Identity `mvn -Penable-tests -f .../forge-plugin-capability-identity/pom.xml test` | PASS，18/18 |
| Capability `mvn -Penable-tests -f .../forge-plugin-capability/pom.xml test` | PASS，13/13 |
| MCP `mvn -Penable-tests -f .../forge-plugin-mcp/pom.xml test` | PASS，13/13 |
| Control Plane `mvn -Penable-tests -f .../forge-plugin-capability-control-plane/pom.xml test` | PASS，20/20 |
| Admin `mvn -pl forge-admin-server -am package -DskipTests` | PASS，39/39 modules |
| Admin 单模块启用测试生命周期 | 首次因本地仓库缺 Forge 模块 POM 失败；执行 reactor install 后 PASS，无测试编译残留 |
| Identity dependency tree + 全 POM + Admin JAR 扫描 | PASS，无 `spring-security-oauth2-authorization-server`、无 `sa-token-oauth2` |
| Mapper `xmllint`、V1.0.22、placeholder、tenant 0、物理 DELETE、`git diff --check` | PASS |
| 目标 ESLint | PASS，无输出 |
| Node 20.19.0 `pnpm build` | PASS；仅既有组件命名、CSS 注释和 chunk 警告 |

未调用真实模型。未启动任何需要清理的长期服务进程。

## 8. 条件跳过与 Review 入口

- 未对真实 MySQL 执行 Flyway，避免污染用户现有库；
- 未使用真实 Redis 验证 20 路并发 code 兑换，当前自动化只证明使用 HMAC key 和 Lua GET+DEL；
- 未启动 Admin/浏览器执行真实登录授权闭环；
- 未把 Mock/模块协议测试冒充上述真实环境验证；
- 下一步应执行 `/review mcp-user-delegation-identity`，优先审查 OAuth 错误契约、撤销边界、完整 Auth 拦截链与上下文泄漏，再决定是否进入 secure actions。

## 9. Review 执行记录

执行时间：2026-07-12 10:19 +08:00。

### 9.1 两阶段审查

- Spec Compliance：发现 R1 完整身份链证据缺口、R2 client grant/用户权限交集未接线，均为 P0 阻断；
- Code Quality / Security：发现 R3 PUBLIC/USER 主动撤销缺失、R4 当前组织未前置一致性校验、R5 平台异常静默降级为客户端错误；另记录 R6 参数/issuer/留存/SYNC 加固项；
- SAS 约束：继续满足，源码 POM、dependency tree 和隔离测试均无 `spring-security-oauth2-authorization-server`，也无 `sa-token-oauth2`；
- MCP transport：继续只使用单 `/mcp` Streamable HTTP，未发现旧 SSE/STATELESS/stdio 回退。

### 9.2 增量验证证据

```text
Identity: Tests run 18, Failures 0, Errors 0, Skipped 0
MCP:      Tests run 13, Failures 0, Errors 0, Skipped 0
dependency:tree: BUILD SUCCESS，无目标依赖输出
git diff --check / xmllint / Flyway 静态扫描: PASS
```

首次静态命令在 `forge-server` 工作目录下误加 `forge-server/` 前缀导致路径不存在；随后使用正确相对路径复跑并通过。该次命令错误不计入产品失败。

### 9.3 结论

Review 状态为 `NEEDS_FIX`。不归档、不进入 secure actions；下一步执行 `/fix mcp-user-delegation-identity`，按 R1 → R2 → R3/R4 → R5 → R6 修复并增量验证。

## 10. Fix 执行记录

执行时间：2026-07-12 10:28–10:59 +08:00。

### 10.1 实现

- R1：新增真实 `fdu_` 聚合链 MockMvc 测试，装配 Resolver、Filter、Lifecycle、SaTokenConfig、Streamable HTTP SDK、Capability Registry 与实际审计 Observer；
- R2：新增 Identity 组合层治理 Policy，实时组合 Token scope、控制面 grant、当前 `LoginUser.permissions` 和 tenant/activeOrg；
- R3/R4：新增当前用户 USER Token 撤销接口；授权预览/确认在 code 签发前校验当前组织；
- R5：OAuth 与 MCP 分离 4xx、403、401、503，日志只记录安全元数据并保留基础设施异常堆栈；
- R6：参数长度、issuer query、SYNC-only、LoginUser 防御性快照、last_used_at 节流、Token 30 天留存清理。

### 10.2 验证证据

| 命令/检查 | 结果 |
|---|---|
| Identity `mvn -Penable-tests test` | PASS，最终复跑 34 tests（R1–R6 全部覆盖） |
| Capability `mvn -Penable-tests test` | PASS，13/13 |
| MCP `mvn -Penable-tests test` | PASS，14/14；新增 ASYNC 失败关闭 |
| Control Plane `mvn -Penable-tests test` | PASS，20/20 |
| AI `mvn -Penable-tests test` | PASS，84/84；未调用真实模型 |
| Admin `mvn -pl forge-admin-server -am package -DskipTests` | PASS，39/39 modules |
| Identity dependency tree + Admin JAR 扫描 | PASS，无 `spring-security-oauth2-authorization-server`、无 `sa-token-oauth2` |
| Mapper `xmllint` / Flyway placeholder / tenant 0 / `git diff --check` | PASS |
| 目标 ESLint | PASS，无输出 |
| Node 20.19.0 `pnpm build` | PASS，`✓ built in 1m 18s`；仅既有组件命名、CSS 注释和 chunk 警告 |

首次使用 reactor `-Penable-tests -am test` 时，既有 `forge-starter-datascope` 无测试引擎导致生命周期失败，目标模块尚未执行；随后先 `-am install -DskipTests` 安装当前 reactor 产物，再按 Identity/MCP/Capability/Control Plane/AI 目标模块分别启用测试并全部通过。该环境命令问题未伪装为产品通过。

### 10.3 条件跳过与清理

- 真实 MySQL Flyway、真实 Redis 20 路并发和真实浏览器 OAuth 全闭环继续跳过，原因与 Review 前一致；
- 本轮未调用真实大模型；AI 84 项均使用 mock/stub；
- 未启动长期服务，无 PID 需要清理；
- 状态更新为 `fixed_pending_review`，下一步再次 `/review mcp-user-delegation-identity`，当前不归档、不进入 secure actions。

## 11. Mapper 重复加载构建残留修复

执行时间：2026-07-12 11:15–11:17 +08:00。

- 启动异常：`AiCapabilityClientMapper.BaseColumns` 已存在，MyBatis 拒绝重复注册同一 namespace 的 SQL fragment；
- 根因：`AiCapabilityClientMapper.xml` 已迁移到 `forge-plugin-capability-control-plane`，但旧 `forge-plugin-capability/target/classes` 及其已安装到 `~/.m2` 的 JAR 仍残留迁移前资源；`classpath*:mapper/**/*Mapper.xml` 因而同时加载新旧两份 XML；
- 处理：使用 JDK 17 执行 `mvn clean install -pl forge-admin-server -am -DskipTests`，清理 reactor 构建目录并覆盖本地 Maven 仓库；
- 结果：39/39 reactor modules `SUCCESS`；源码、`target/classes`、模块 JAR 和本地 Maven JAR 四层检查均确认 Mapper 只存在于 control-plane 模块；
- 本轮没有修改 Mapper 源码、namespace 或业务逻辑，没有启动服务、执行 Flyway 或调用真实模型。应用需使用本次重新安装的依赖重启。

## 12. 最终复审与归档

执行时间：2026-07-12 11:54–11:56 +08:00。

- 实际代码复核：权限交集、fdu Bearer、Origin、错误分类、上下文清理、SYNC/STREAMABLE 启动闸门符合 Spec；
- Identity：34 tests，0 failures，0 errors；
- MCP：14 tests，0 failures，0 errors；
- 静态扫描：无 `spring-security-oauth2-authorization-server`、无 `sa-token-oauth2`，无旧 SSE/STATELESS/stdio/ASYNC 配置；
- 最终结论：Spec Compliance PASS，Code Quality / Security PASS，状态更新为 `done`；
- 归档路径：`code-copilot/changes/archive/2026-07-12-mcp-user-delegation-identity/`；
- 本轮未启动长期服务、未执行真实 Flyway/Redis/浏览器 E2E、未调用真实大模型。
