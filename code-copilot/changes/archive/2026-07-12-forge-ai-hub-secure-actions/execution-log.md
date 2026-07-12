# 执行日志 — Forge AI 中枢受控业务动作

> change: `forge-ai-hub-secure-actions`
> status: done
> created: 2026-07-12

## 1. 启动记录

| 时间 | 阶段 | 事件 | 结论 |
|---|---|---|---|
| 2026-07-12 | Archive | 归档 `mcp-user-delegation-identity` | Identity 34/34、MCP 14/14、Admin 39/39，状态 done |
| 2026-07-12 | Direction | 用户要求开始新阶段开发 | 进入阶段 2.1 secure actions Proposal/Apply |
| 2026-07-12 | Mapping | 检查 Capability/MCP/Control Plane/Generator | 复用已发布业务动作，但必须增加发布快照、字段交集、R2 elicitation 和幂等指纹加固 |
| 2026-07-12 | Protocol | 检查 MCP SDK 0.17.0 公开 API | `McpSyncServerExchange.createElicitation` 可用于同调用 R2 确认，无需旧 SSE 或自建确认 Token |

## 2. 已冻结边界

- 固定元工具而非动态顶层业务 Tool；
- 仅 BUSINESS_ACTION/ACTION/MEDIUM；
- 仅 UPDATE_FIELD/CREATE_RECORD；
- 只允许 USER Token invoke；
- 字段白名单三方交集；
- elicitation ACCEPT 后同步执行；
- 不含 Flowable、消息、领域动作、R3、Nacos、Agent Runtime 和真实模型调用。

## 3. Apply 进展

执行时间：2026-07-12 12:04–12:10 +08:00。

- MCP：新增 `McpToolContributor` 与启动期聚合器；固定工具按名称稳定排序，重复名称失败关闭；ping 改为默认 contributor；
- MCP 验证：16 tests，0 failures，0 errors；既有 STREAMABLE/SYNC Guard 回归保持；
- Generator：新增发布版本 Mapper XML、发布动作解析和 `executePublished` 入口；
- 幂等：请求指纹改为 canonical JSON 的 SHA-256，不再把参数值写入日志；相同幂等键不同请求拒绝；
- Generator 验证：`BusinessActionExecutionServiceTest` 3/3，模块 compile PASS；
- 模块：创建 `forge-plugin-capability-secure-actions`，接入 plugin parent、BOM 和 Admin；默认未启用，避免未完成 Tool 误开放；
- 聚合编译：Secure Actions `-am compile`，33/33 reactor modules SUCCESS；
- 本轮未启动服务、未执行 Flyway、未调用真实模型、未 commit/push。

## 4. Apply 收尾验证

执行时间：2026-07-12 12:57–13:18 +08:00。

### 4.1 增量实现与缺陷修正

- Secure Actions：完成业务动作专用发布、ACTION grant、动态授权目录、`search/describe/invoke`、MCP elicitation、字段三方交集和双审计关联；
- Schema：修复发布 Schema 缺少 Draft 2020-12 声明的问题，三个固定元工具补齐 `outputSchema`、annotations、text JSON 和 `structuredContent`；
- 权限：固定元工具显式校验 `capability:discover` / `capability:invoke` 或能力级 scope，再叠加 client grant、平台权限、动作权限、tenant/activeOrg；OAuth metadata 与请求校验器同步支持受治理目录的通用 scope；
- 目录：从固定 51 条改为 `capability_code + id` keyset 分批扫描，权限过滤后仍能准确返回 `hasMore`，不把全量授权加载进内存；
- 执行：调用前重新从指定发布版本读取动作和模型快照，校验版本字段 ∩ grant 字段 ∩ 发布模型可写字段；创建动作允许省略 `recordId`；
- 错误与审计：增加 `INSUFFICIENT_SCOPE`、`POLICY_MISMATCH`、`IDEMPOTENCY_CONFLICT` 稳定错误码；Schema 越权映射为 `INVALID_ARGUMENT` 并在审计中只记录 Schema 路径；审计写入异常只记录 requestId、capabilityCode、exceptionType；
- Generator：发布快照与 canonical SHA-256 幂等指纹生效，业务动作日志记录 capability requestId、clientId、serviceUserId、actorType，不保存参数值。

### 4.2 执行证据

| 验证项 | 命令/范围 | 结果 |
|---|---|---|
| 当前 Reactor 依赖安装 | `mvn -pl ...forge-plugin-capability-secure-actions -am install -DskipTests` | 33/33 SUCCESS |
| Secure Actions | 模块 `mvn -Penable-tests test` | 18/18 PASS |
| Generator 专项 | `BusinessActionExecutionServiceTest,BusinessObjectActionServicePublishedTest` | 7/7 PASS |
| Control Plane | 模块 `mvn -Penable-tests test` | 24/24 PASS |
| MCP | 模块 `mvn -Penable-tests test` | 16/16 PASS；协议 `2025-06-18`、STREAMABLE/SYNC Guard 保持 |
| Identity | 模块 `mvn -Penable-tests test` | 35/35 PASS |
| Admin 聚合 | `mvn -pl forge-admin-server -am package -DskipTests` | 40/40 SUCCESS |
| 禁用依赖 | Admin `dependency:tree` includes 扫描 | 无 Spring Authorization Server、无 Sa-Token OAuth2 |
| XML/SQL | `xmllint` + placeholder/tenant/DELETE 扫描 | PASS；V1.0.23 未实库执行 |
| 传输 | MCP/Identity/Secure Actions 静态扫描 | 仅 Guard/负向测试出现 SSE、STATELESS、stdio、ASYNC；运行配置仍为单 `/mcp` Streamable HTTP |

### 4.3 基线差异与跳过项

- 首次从 Secure Actions 子模块直接测试时解析到本地仓库旧 Generator 构件，导致新发布快照类型不可见；通过 Reactor `-am install -DskipTests` 安装当前工作区构件后解决，产品代码无需兼容旧构件；
- 首次执行 `-am test` 时，Generator 全量基线 289 项中存在 2 failures + 1 error：`FormulaExecutionEngineLookupTest`、`FormulaValueMaskerTest`、`LowcodeRuntimeConfigBuilderTest`。这些测试不在本轮改动路径；本轮 Generator 专项 7/7 和 Admin 聚合编译均通过，未修改无关公式/低代码逻辑；
- 未启动 Admin，避免 Flyway 自动修改真实数据库；V1.0.23 仅做静态验收；
- 未接入真实支持 elicitation 的外部 MCP 客户端，使用 SDK exchange mock 覆盖 ACCEPT/DECLINE/CANCEL/不支持 elicitation；
- 未调用真实大模型，未启动后台服务，未产生需清理 PID，未 commit/push。

## 5. Apply 结论

阶段 2.1 受控业务动作已达到 `applied_pending_review`：写副作用必须依次通过可信 USER 身份、Token scope、实时 client grant、用户权限、租户/组织、发布快照、字段交集、幂等键和 MCP elicitation `ACCEPT + confirm=true`。流程、消息、领域动作、高风险动作、旧 SSE/STATELESS/stdio/ASYNC 仍失败关闭。

## 6. Review 执行记录（2026-07-12）

本轮只读审查实现，没有修改产品代码、启动服务、执行 Flyway、调用真实模型或写数据库。

| 检查 | 结果 |
|---|---|
| 固定 Tool、Streamable HTTP、USER/Scope/权限、发布快照、elicitation、幂等、禁用依赖 | PASS |
| R1 grant 字段交集进入运行时 Schema | FAIL HARD-GATE；descriptor 保留版本原始 inputSchema，grant 缩小未生效 |
| R2 resolved version source binding | FAIL HARD-GATE；SQL 混用 `v.version/policy` 与 `c.source_key/source_version` |
| R3 递归步骤失败关闭 | FAIL HARD-GATE；validator 只遍历顶层 `actionConfig.steps` |
| R4 基础设施不可用错误 | FAIL；目录解析故障会成为 `INVALID_ARGUMENT`，普通 DB 故障成为 `EXECUTION_FAILED` |
| R5 Capability 双审计闭环 | FAIL；审计异常被吞，业务写入后仍可返回成功 |
| R6 elicitation 请求指纹 | FAIL；当前摘要只包含 requestId 与 idempotencyKey |

Review 结论：`NEEDS_FIX`。Spec Compliance 未通过，按两阶段规则未进入 Code Quality Review；当前状态为 `reviewed_with_findings`，不得归档或进入 Flow Actions。下一步执行 `/fix forge-ai-hub-secure-actions`。

## 7. Fix 执行记录（2026-07-12）

### 7.1 实现结果

- R1：运行时输入 Schema 裁剪到版本/grant 交集，invoke 增加显式字段集合校验；
- R2：授权目录统一读取 resolved version 的 source binding，并同时约束版本行为、风险和可见性；
- R3：递归扫描步骤容器，顶层兼容 `steps/stepList`，任何嵌套步骤和未知/禁用类型拒绝；
- R4：新增 `CATALOG_UNAVAILABLE/AUTHORIZATION_UNAVAILABLE/AUDIT_UNAVAILABLE`，安全日志不包含原始异常消息或参数；
- R5：Capability 审计改为写前预留、相同 requestId + 双身份条件更新；审计故障不再静默返回业务成功；
- R6：elicitation 使用 canonical request SHA-256 指纹，不记录字段值。

### 7.2 验证证据

| 验证项 | 结果 |
|---|---|
| Reactor 当前构件安装 | `-pl ...forge-plugin-capability-secure-actions -am install -DskipTests`，33/33 SUCCESS |
| Secure Actions | 27 tests，0 failures，0 errors |
| Control Plane | 27 tests，0 failures，0 errors |
| Generator 专项 | 7 tests，0 failures，0 errors |
| MCP | 16 tests，0 failures，0 errors；协议仍为 2025-06-18 + Streamable HTTP/SYNC Guard |
| Identity | 35 tests，0 failures，0 errors |
| Admin 聚合 | 最终复跑 40/40 reactor modules SUCCESS |
| XML/静态安全 | 两个 Mapper `xmllint` PASS；trailing whitespace、禁用认证依赖、arguments 日志、Secure Actions 传输漂移扫描 PASS |

首次 Secure Actions 增量测试在 testCompile 阶段因 AssertJ 当前版本的 `IteratorAssert` 不支持 `containsExactly` 失败；改为显式收集字段列表后复跑 27/27 通过。该失败未进入产品运行逻辑。

### 7.3 条件跳过与清理

- 未启动 Admin 或其它后台服务，未产生需清理 PID；
- 未执行 V1.0.23 真实 MySQL 迁移，继续保留静态校验证据；
- 未接真实外部 MCP elicitation 客户端，使用 SDK exchange mock；
- 未调用真实大模型，未 commit/push。

Fix 结论：R1–R6 已修复并完成增量验证，状态更新为 `fixed_pending_review`。下一步再次执行 `/review forge-ai-hub-secure-actions`；复审通过前不归档或进入 Flow Actions。

## 8. 修复后复审与归档（2026-07-12）

### 8.1 两阶段复审

- Spec Compliance：R1–R6 逐项核对实际代码和测试，全部 PASS；
- Code Quality：未发现 Critical、Important 或阻塞归档的 Minor 问题；
- HARD-GATE：发布快照、授权字段交集、幂等、elicitation、USER 双身份和写前审计均保持失败关闭，允许进入 Flow Actions。

### 8.2 增量证据

| 验证项 | 结果 |
|---|---|
| Secure Actions 专项 | 2026-07-12 14:30，27/27 PASS |
| Mapper XML | `xmllint --noout` PASS |
| Flyway placeholder | V1.0.23 扫描无输出 |
| 文本质量 | 相关 `git diff --check` PASS |
| 既有回归基线 | Control Plane 27/27、Generator 7/7、MCP 16/16、Identity 35/35、Admin 40/40 |

第一次 placeholder 命令从子模块使用了错误的相对路径并返回“文件不存在”，随后改为正确的 `../../../db/migration/...` 路径复跑，无输出；该命令问题不涉及产品代码。

### 8.3 归档结论

- 状态更新为 `done`；
- 归档目录：`code-copilot/changes/archive/2026-07-12-forge-ai-hub-secure-actions/`；
- 未启动服务、未执行真实 Flyway、未调用真实模型、未产生待清理 PID，未 commit/push。
