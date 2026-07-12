# 执行日志 — Forge AI 中枢控制面

> change: `forge-ai-hub-control-plane`
> status: done
> created: 2026-07-11

## 1. 时间线

| 时间 | 阶段 | 事件 | 结论 |
|---|---|---|---|
| 2026-07-11 | Archive | 归档 `forge-ai-hub-foundation` | 阶段 0 done，动态目录限制转入阶段 1 |
| 2026-07-11 | Research | 读取路线图、Capability/MCP、低代码与认证现状 | 阶段 1 拆分为独立控制面切片 |
| 2026-07-11 | SDK Spike | 比较 MCP Java SDK 0.17.0 与 2.0.0 公共 API/字节码 | 两者 tools/list 均为进程静态集合；不升级、不 fork、不反射 |
| 2026-07-11 | Proposal | 创建四份变更文档 | 用户已授权自动进入 Apply |
| 2026-07-11 | Red | 首次编译控制面领域测试 | 因缺少 `CapabilityGrantPolicy`、`CapabilityControlPlaneProperties` 按预期 testCompile 失败 |
| 2026-07-11 | Green | 实现五表、实体/Mapper XML、领域服务、管理 API 和配置 | 控制面最小闭环完成 |
| 2026-07-11 | Architecture Fix | 控制面最初放入核心 Capability 后运行 MCP 回归 | 先后触发 ORM mapperPackage 占位符、Auth/Redis 自动配置污染；拆出独立 control-plane 模块 |
| 2026-07-11 | Regression | 重跑 Capability、Control Plane、MCP、AI、Admin | 测试与 38 模块聚合全绿 |
| 2026-07-11 | Review/Fix | 完成 Spec Compliance 与 Code Quality Review | 修复逻辑撤销、装配、版本快照、grant 绑定、身份和幂等写入问题 |
| 2026-07-12 | Review Fix | 修复凭据并发、认证前租户定位、完整版本指纹与审计内容校验 | Control Plane 由 14 增至 20 tests |
| 2026-07-12 | MCP Auth Wiring | Admin 组合根接入控制面 Bearer 机器凭据 | 可信身份来自 DB 绑定，伪造用户 Header 无效 |
| 2026-07-12 | Archive | 用户明确要求归档上一个阶段并开始阶段 2 | 状态更新为 done，四份文档移入日期归档目录 |

## 2. 关键决策

- 控制面先于低代码只读执行器落地；
- 五张表一次建立，版本快照不推迟；
- 原始 Secret 永不落库，Pepper 无默认值；
- 核心 `forge-plugin-capability` 不依赖 ORM/Auth/Web，持久化控制面独立为 `forge-plugin-capability-control-plane`；
- MCP 只依赖核心 Capability，Admin 同时聚合 MCP 与控制面；
- 当前变更不改变 MCP Tool 集合，继续只发布 ping；
- 下一变更使用统一元工具动态发现/调用授权能力，避免 SDK 静态 tools/list 泄露目录；
- 不引入 Nacos、Agent Runtime、高风险工具或旧 SSE。
- 机器调用记录绑定服务账号；具体人员调用采用后续短期用户委托令牌，同时保留 clientId/serviceUserId，禁止信任自报 userId Header。

## 3. 验证记录

| 时间 | 范围 | 命令 | 结果 | 警告/跳过 |
|---|---|---|---|---|
| 2026-07-11 | Red | `mvn -Penable-tests -pl .../forge-plugin-capability-control-plane -am test` | FAIL（预期） | 缺 `CapabilityGrantPolicy`、`CapabilityControlPlaneProperties`，随后进入 Green |
| 2026-07-11 | MCP 架构回归 1 | `mvn -Penable-tests -pl .../forge-plugin-mcp -am test` | FAIL（已修复） | 控制面错误并入核心模块，传递加载 ORM，mapperPackage 占位符无法解析 |
| 2026-07-11 | MCP 架构回归 2 | 同上 | FAIL（已修复） | 继续传递加载 Auth/Redis 自动配置；据此拆出独立 control-plane 模块 |
| 2026-07-11 | Capability + Control Plane | `mvn -Penable-tests -pl .../forge-plugin-capability-control-plane -am test` | PASS：13 + 14 tests | 0 failure/error/skip；包含 Review 修复后的最终结果 |
| 2026-07-11 | Capability + MCP | `mvn -Penable-tests -pl .../forge-plugin-mcp -am test` | PASS：13 + 13 tests | 0 failure/error/skip；实际握手协议 `2025-06-18`，旧 transport Guard 分支通过 |
| 2026-07-11 | AI | `mvn -Penable-tests -pl .../forge-plugin-ai test` | PASS：84 tests | 0 failure/error/skip；模拟网络失败堆栈是预期异常分支 |
| 2026-07-11 | Admin 聚合 | `mvn -pl forge-admin-server -am package -DskipTests` | PASS：38/38 modules | 已包含新 control-plane 模块；仅保留既有 deprecated/unchecked 编译提示 |
| 2026-07-11 | Mapper XML | `xmllint --noout .../src/main/resources/mapper/*.xml` | PASS：5 files | 无 XML 语法错误 |
| 2026-07-11 | Flyway/安全静态扫描 | `${...}`、tenant 0、物理 DELETE、无列名 INSERT、版本、逻辑删除、Nacos/高风险工具、敏感审计字段、尾随空白 | PASS | `V1.0.21` 唯一；无违规命中 |
| 2026-07-11 | Targeted diff check | `git diff --check --` 本变更已跟踪聚合配置 | PASS | 仓库级 cached check 会命中用户已有 vendor 上游尾随空白，因此未作为本变更失败依据 |
| 2026-07-12 | Control Plane 增量 Fix | `mvn -Penable-tests -pl .../forge-plugin-capability-control-plane -am test` | PASS：13 + 20 tests | 0 failure/error/skip |
| 2026-07-12 | Admin MCP 身份适配 | `mvn -Penable-tests -pl forge-admin-server -am -Dtest=CapabilityMcpCallerContextResolverTest,CapabilityClientServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS：7 + 2 targeted tests；38/38 reactor SUCCESS | 仅既有 deprecated/unchecked 与 commons-logging 提示 |
| 2026-07-12 | MCP 回归 | `mvn -Penable-tests -pl .../forge-plugin-mcp -am test` | PASS：Capability 13 + MCP 13 | 实际握手协议 `2025-06-18`；旧 transport 失败分支符合预期 |
| 2026-07-12 | AI 回归 | `mvn -Penable-tests -pl .../forge-plugin-ai test` | PASS：84 tests | 模拟流式网络异常堆栈为预期分支，未调用真实模型 |
| 2026-07-12 | Admin 聚合 | `mvn -pl forge-admin-server -am package -DskipTests` | PASS：38/38 modules | 仅既有 deprecated/unchecked 编译提示 |

## 4. Review 与增量修复

### 4.1 Spec Compliance

- 五张控制面表、标准审计字段、显式 `@TableLogic`、Mapper XML 租户/逻辑删除条件一致；
- 能力版本、机器客户端、grant、审计和管理 API 与 Spec 一致；
- MCP 仍只发布 `capability.ping`，没有接入低代码执行器、动态业务 Tool、Nacos、AgentScope、任意 SQL/HTTP/文件/代码执行；
- transport 保持 Streamable HTTP，旧 SSE、STATELESS 和 stdio 均失败关闭；
- 本轮没有前端改动，也没有超范围创建管理页面。

### 4.2 Code Quality Findings / Fixes

| 级别 | 发现 | 修复 |
|---|---|---|
| P0 | 默认 MyBatis-Plus `IdentifierGenerator` 未承诺注册为 Spring Bean，构造器注入可能阻断 Admin 启动 | 审计 ID 改用官方 `IdWorker.getId`，消除隐式 Bean 依赖 |
| P1 | grant 撤销只改状态，逻辑唯一键不释放，无法重新授权 | Mapper XML 原子更新 `status=REVOKED, del_flag=1` |
| P1 | 相同版本可在 checksum 不变时漂移 `sourceVersion` | 发布时同时校验 checksum 与 sourceVersion 快照不可变 |
| P1 | 授权策略未二次校验 grant 的 clientId/capabilityId 引用 | 增加 `GRANT_SCOPE_VIOLATION` 失败关闭 |
| P1 | 历史脏数据中的非正数服务账号/组织仍可能通过客户端认证 | 创建和认证双重校验有效服务身份 |
| P2 | `INSERT IGNORE` 会把非幂等类数据库错误也降级为警告 | 改为普通 INSERT + `ON DUPLICATE KEY` 幂等 no-op |
| P2 | 资源 seed 的 `NOT EXISTS` 可能被已逻辑删除资源阻断 | parent、resource、admin role 查询显式过滤 `del_flag=0` |
| P0 | authenticate 使用完整实体 `updateById`，并发旧快照可能恢复 rotate/revoke 前的密钥和状态 | 三条凭据写路径改为 XML 目标字段更新 + `credential_version` CAS，影响行数 0 时失败关闭 |
| P0 | 认证 API 要求认证前 tenantId，且 MCP 没有生产 Resolver 接入控制面 | Secret 增加全局 keyId；Admin 组合根从 Bearer 凭据解析权威 tenant/serviceUser/activeOrg |
| P0 | 未登录 MCP 请求的 `last_used_at` 更新会被租户插件追加 `tenant_id = NULL` | 认证专用 Mapper 方法显式忽略自动租户追加，SQL 仍使用凭据记录中的 tenantId 限定 |
| P1 | 版本 checksum 未覆盖完整执行语义，审计稳定码可能混入原始 Secret/异常 | 完整版本指纹入库并参与 checksum；审计字段采用枚举、格式和长度白名单 |

首轮 Review 后 Control Plane 测试由 7 项增强为 14 项；2026-07-12 增量 Fix 后增强为 20 项，并重新通过 Capability/控制面回归与 Admin 最终聚合。

## 5. 跳过与限制

- 没有受控测试 MySQL 实例，因此未实跑 Flyway；已完成 SQL 静态结构、版本、租户、逻辑删除、seed 和安全检查；
- 本变更无前端文件变化，未重复执行 `pnpm build`，复用阶段 0 成功基线；
- 未执行仓库级全量 staged whitespace 判定：工作区包含用户已有两套 vendor 源码，上游原始尾随空白与本变更无关；已对本变更路径执行 targeted 检查；
- 未启动 Admin、MySQL、Redis 或 Nacos，不存在需要清理的后台服务。

## 6. 服务与外部调用

- Proposal 研究只访问 Maven Central 元数据并下载 MCP SDK `2.0.0` 到本地 Maven 缓存；
- 未启动服务、数据库、Redis 或 Nacos；
- 未调用模型或产生 Token 费用；
- 未 commit/push。

## 7. 当前结论

`forge-ai-hub-control-plane` 已完成 Proposal、Apply、Review、增量 Fix 和归档，状态为 `done`。机器模式当前可可信归因到客户端绑定服务账号。用户于 2026-07-12 明确开始阶段 2；下一独立变更为 `mcp-user-delegation-identity`，先建立 Forge 登录/OAuth 短期用户委托身份，再开放人员责任写入与流程闭环；禁止用调用方自报 Header 伪造实际人员。
