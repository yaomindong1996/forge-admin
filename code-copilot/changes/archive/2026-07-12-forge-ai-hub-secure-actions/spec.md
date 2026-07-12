# Forge AI 中枢受控业务动作

> status: done
> created: 2026-07-12
> complexity: 🔴复杂
> parent-stage: Forge AI 中枢阶段 2.1 — 受控写入

## 1. 背景与目标

`mcp-user-delegation-identity` 已归档，MCP 每个请求都能建立可信 USER/SERVICE 双身份，并实时计算 Token scope、客户端 grant、用户权限、租户和当前组织交集。当前 MCP 仍只发布 `capability.ping`，不能安全发现或执行业务写动作。

本变更交付阶段 2.1 的最小可用闭环：把低代码应用中已经发布的、显式允许 AI 调用的业务动作发布为 `BUSINESS_ACTION` 能力，通过固定元工具 `capability.search`、`capability.describe`、`capability.invoke` 动态发现和调用。写入必须经过发布快照、授权、字段白名单、幂等和 MCP elicitation 人工确认，不能退化为任意 CRUD、任意 Mapper、任意 SQL、任意 URL 或任意代码执行。

## 2. 成功标准

- MCP 仍为单 `/mcp` Streamable HTTP；固定发布 `capability.ping/search/describe/invoke`，不动态创建顶层业务 Tool；
- `capability.search/describe` 只返回当前 client grant、当前用户权限、tenant/activeOrg 同时允许的已发布业务动作；
- 业务动作只从 `ai_business_object_design_version.publish_status=PUBLISHED` 的不可变快照解析，禁止读取已变更草稿作为执行定义；
- 当前阶段只允许 `UPDATE_FIELD`、`CREATE_RECORD` 及其安全组合；拒绝 `START_FLOW`、`SEND_MESSAGE`、`DOMAIN_ACTION`、外部 HTTP/URL、脚本和任意 SQL；
- 能力版本策略和 grant 实时生效；撤销 client、grant、用户权限或组织成员关系后下一请求立即失败；
- 输入字段必须同时属于能力版本 `policy_snapshot.allowedFields`、grant `field_policy.allowedFields` 和目标发布模型字段；`additionalProperties=false`；
- `capability.invoke` 强制 16～128 位幂等键；相同键与相同请求返回已有结果，相同键与不同请求指纹拒绝；
- MEDIUM/ACTION 必须由支持 MCP elicitation 的客户端在同一次调用中返回 `ACCEPT`；`DECLINE/CANCEL` 或客户端不支持 elicitation 时不产生业务副作用；
- 执行身份沿用 A，ORM `create_by/update_by/create_dept`、业务动作日志和 Capability 调用审计记录 A/client/serviceUser/tenant/org；
- 返回结构化结果和稳定错误码，不返回 SQL、异常堆栈、Token、Header、完整业务敏感数据或原始请求；
- Identity、MCP、Control Plane、Generator、Secure Actions 和 Admin 聚合回归通过，不调用真实大模型。

## 3. 范围

### 3.1 包含

- 新增 `forge-plugin-capability-secure-actions` 组合模块；
- MCP Tool contributor 扩展点和四个固定 Tool 聚合；
- 业务动作能力专用发布 API，自动生成 Schema、策略快照和 source binding；
- 发布动作快照解析、安全步骤验证、字段白名单验证；
- 授权目录查询、describe 和 invoke；
- MCP elicitation R2 确认；
- 业务动作幂等指纹加固；
- Capability 双身份审计与业务动作执行日志关联；
- Flyway 权限资源和必要索引/字段迁移；
- TDD、模块测试和聚合验证。

### 3.2 明确不做

- 不开放任意表 CRUD、任意 Mapper、任意 SQL、任意 HTTP、任意文件或任意代码执行；
- 不实现 Flowable start/approve/reject，留给 `forge-ai-hub-flow-actions`；
- 不发送站内信、短信、邮件或三方消息；
- 不开放 `DOMAIN_ACTION`、库存数量台账等领域副作用；
- 不实现 R3 高风险审批、加密快照或异步人工审批；
- 不新增复杂前端工作台；本阶段只增加必要管理 API；
- 不接 Nacos MCP Registry/Admin、AgentScope 或 Agent Runtime；
- 不使用旧 SSE、STATELESS、stdio 或 ASYNC MCP Server；
- 不引入 Spring Authorization Server 或 Sa-Token OAuth2；
- 不调用真实模型。

## 4. 架构

```text
/mcp Streamable HTTP
  -> fdu Bearer + ExecutionIdentity
  -> fixed tools: capability.search / describe / invoke
  -> SecureActionCatalogService
       -> ai_capability + version + grant（控制面）
       -> published BusinessObjectDesignVersion（发布快照）
       -> action permission + tenant/org
  -> SecureActionPolicy（版本白名单 ∩ grant 白名单 ∩ 发布模型字段）
  -> MCP elicitation ACCEPT
  -> BusinessActionExecutionService.executePublished(...)
  -> ai_business_action_execution_log + ai_capability_invocation_log
```

`forge-plugin-mcp` 只增加协议级 `McpToolContributor` 聚合扩展点，不依赖 Generator 或控制面。Secure Actions 模块负责把 MCP、控制面、Identity 和 Generator 组合起来；Generator 不反向依赖 Capability，避免循环依赖。

## 5. 能力契约

### 5.1 业务动作 source binding

- `source_type=BUSINESS_ACTION`；
- `source_key={suiteCode}/{objectCode}/{actionCode}`，每段只允许 `[A-Za-z0-9_-]`；
- `source_version={publishVersion}`；
- `behavior=ACTION`；
- `risk_level=MEDIUM`；
- `visibility=DISCOVERABLE`；
- `policy_snapshot` 至少保存 `allowedFields`、`requiredFields`、`allowedStepTypes`、`confirmationMode=MCP_ELICITATION`、`publishedObjectVersion`；
- 输入 Schema 只包含 `recordId`、`idempotencyKey` 和 `arguments`，其中 `arguments.properties` 由字段白名单生成且 `additionalProperties=false`。

### 5.2 固定元工具

| Tool | 行为 | 注解 |
|---|---|---|
| `capability.search` | 查询当前调用方可发现动作，limit 1～50 | readOnly=true, destructive=false, idempotent=true |
| `capability.describe` | 返回已授权版本、输入/输出 Schema、风险和确认要求 | readOnly=true, destructive=false, idempotent=true |
| `capability.invoke` | 校验并执行一个已授权动作 | readOnly=false, destructive=true, idempotent=true |

工具结果同时提供文本 JSON 和 `structuredContent`。业务错误作为 Tool result `isError=true` 返回；协议级认证仍由 HTTP 401/403/503 处理。

## 6. 安全与业务规则

1. `capability.invoke` 只接受能力编码、可选版本、记录 ID、幂等键和 `arguments`；suite/object/action、tenant/user/org/client 只能从服务端 source binding 与可信身份获得；
2. 只允许 USER Token 执行业务动作。SERVICE Token 在本阶段可 search/describe，但 invoke 返回 `USER_DELEGATION_REQUIRED`；
3. 发布和调用时都递归校验动作步骤，只允许 `UPDATE_FIELD`、`CREATE_RECORD`；嵌套/未知步骤失败关闭；
4. grant 创建 ACTION 能力时必须存在非空字段策略，且只能缩小版本白名单；
5. R2 确认摘要包含动作名称、对象、记录 ID、字段名和请求指纹，不在服务日志中记录字段值；
6. elicitation 的 `ACCEPT` 只对当前同步调用有效，不签发可重放确认 Token；
7. 幂等唯一域保持 tenant/object/record/action/idempotencyKey；服务额外比较 canonical request SHA-256，不同请求不能复用已有成功结果；
8. 发布快照版本、能力版本和 grant resolvedVersion 必须一致；草稿变更不影响已发布动作；
9. 任意 DB/Redis/目录故障返回稳定不可用错误，不伪装成无权限或参数错误；
10. 审计不保存 arguments、elicitation 内容、Token、Header 或原始异常消息。

## 7. 数据变更

新增 `V1.0.23__add_secure_business_action_capabilities.sql`：

- 增加受控业务动作发布和调用权限资源；
- 为业务动作执行日志补充 `capability_request_id/client_id/service_user_id/actor_type`，便于双日志关联；
- 字段按数据库规范具备防重复迁移；
- 不插入真实 capability、client、grant、Token 或业务动作数据；
- 不修改已执行的 V1.0.21/V1.0.22。

## 8. HARD-GATE

- 未经过发布快照、grant、用户动作权限、字段交集、幂等和 elicitation ACCEPT 的请求不能产生写副作用；
- SERVICE Token、HIGH 风险、流程、消息、领域动作、未知步骤全部失败关闭；
- 相同幂等键不同请求必须拒绝；
- 认证/授权/确认/执行异常后 ExecutionIdentity、TenantContext 和 MDC 无泄漏；
- MCP 仍只有 `/mcp` Streamable HTTP；
- 任一硬闸门失败时不得进入 `forge-ai-hub-flow-actions`。

## 9. 回滚

- `forge.capability.secure-actions.enabled=false` 关闭三个元工具和业务动作发布 API，保留 ping 与身份底座；
- 已发布 capability/grant 可在控制面停用/撤销；
- 新增审计字段保留，不自动删除；
- 回滚不得恢复任意 CRUD Tool、裸 Header 身份、长期 fcp 直连或旧 SSE。

## 10. 确认记录

- **确认时间**：2026-07-12；
- **确认方式**：用户要求归档 `mcp-user-delegation-identity` 并开始新阶段开发；
- **批准范围**：阶段 2.1 受控业务动作，不包含流程办理、消息和 R3 高风险审批；
- **执行方式**：Proposal 后直接进入 Apply，禁止 commit/push。

## 11. Review 结论

> review-date: 2026-07-12
> conclusion: NEEDS_FIX

### 11.1 Spec Compliance

已满足：单 `/mcp` Streamable HTTP、固定元工具、USER 委托身份、发布快照、ACTION/MEDIUM、MCP elicitation、幂等请求摘要、结构化结果、禁用旧 SSE/SAS/Sa-Token OAuth2 及 V1.0.23 静态迁移约束均已落地。

存在六个 Spec 不合规项：

1. **R1 / P0 — grant 字段交集没有进入实际输入 Schema。** `SecureActionCatalogService` 计算了能力版本与 grant 的 `allowedFields` 交集，但 `SecureActionDescriptor.inputSchema` 仍直接使用能力版本原始 Schema；`SecureActionMcpHandler` 只用该原始 Schema 校验 `arguments`，没有再按交集检查字段。因此被 grant 排除、但仍存在于版本 Schema 的字段仍可进入业务动作执行，违反 §2、§6.4 和 HARD-GATE。
2. **R2 / P0 — resolved capability version 与 source binding 混用。** `SecureActionCatalogMapper.xml` 从 `ai_capability_version v` 选择授权版本和策略，却从 `ai_capability c` 返回当前 `source_key/source_version`。能力升级或重绑定后，PINNED grant 会混用“旧版本 Schema/策略 + 当前动作 binding”；版本号不同时异常失效，版本号碰巧相同时可能执行错误动作，违反 §6.8。
3. **R3 / P0 — 动作步骤没有递归失败关闭。** `SecureActionStepValidator` 只检查 `actionConfig.steps` 的顶层 `stepType`，没有递归检查 `stepList`、嵌套 `steps`、`childSteps` 或步骤配置中的未知步骤，违反 §6.3 和 HARD-GATE。
4. **R4 / P1 — 目录/DB 故障没有稳定映射为不可用。** 目录数据解析异常被包装为 `BusinessException` 后映射为 `INVALID_ARGUMENT`，普通 Mapper/数据库异常映射为 `EXECUTION_FAILED`；两者都不能表达目录/授权/审计基础设施不可用，且前者会把平台故障伪装成调用参数错误，违反 §6.9。
5. **R5 / P1 — Capability 审计失败被吞并仍返回业务成功。** 业务动作已提交后才写 Capability 调用审计，审计异常只记录 warn，调用仍返回成功，因此不能保证成功标准要求的 A/client/serviceUser/tenant/org 双日志审计闭环。
6. **R6 / P1 — elicitation 展示的“请求指纹”没有绑定请求内容。** 当前短哈希仅由 `requestId + idempotencyKey` 生成，不包含 capability/version/recordId/arguments，不能作为 §6.5 所要求的请求指纹。

### 11.2 Review Gate

- Spec Compliance 结论为 FAIL；按两阶段 Review 规则，不进入 Code Quality Review；
- 当前状态为 `reviewed_with_findings`，不得归档或进入 Flow Actions；
- 下一步执行 `/fix forge-ai-hub-secure-actions`，优先顺序固定为 R1 → R2 → R3 → R4/R5 → R6；
- Fix 后必须为每项补充 Red/Green 证据，并增量复跑 Secure Actions、Generator、Control Plane、Identity、MCP 和 Admin 聚合。

## 12. Fix 结果

> fix-date: 2026-07-12
> conclusion: FIXED_PENDING_REVIEW

1. **R1 已修复。** 目录把能力版本 Schema 的 `arguments.properties/required` 裁剪到版本策略与 grant 策略交集；invoke 同时显式拒绝交集之外的字段，指定发布模型仍作为第三道校验。
2. **R2 已修复。** 目录 SQL 从 resolved `ai_capability_version` 读取 `source_key/source_version/risk_level`，并要求 capability/version 同时满足 BUSINESS_ACTION/ACTION/MEDIUM/DISCOVERABLE，PINNED/FOLLOW_MAJOR 不再混用主表当前 binding。
3. **R3 已修复。** 发布和调用共用递归容器扫描；顶层兼容 `steps/stepList`，任意 `childSteps`、嵌套 `steps/stepList`、未知或禁用步骤全部失败关闭。
4. **R4 已修复。** 新增安全基础设施异常，目录查询、授权查询、目录数据和审计存储分别稳定映射为 `CATALOG_UNAVAILABLE`、`AUTHORIZATION_UNAVAILABLE`、`AUDIT_UNAVAILABLE`，日志只记录 requestId、错误码和异常类型。
5. **R5 已修复。** elicitation ACCEPT 后、业务写入前必须先按 requestId 与可信双身份写入 `EXECUTION_PENDING` 审计；最终结果只能按相同身份更新。预留失败不进入执行，最终审计失败返回 `AUDIT_UNAVAILABLE` 并保留待完成记录，幂等重试可补齐最终状态。
6. **R6 已修复。** elicitation 指纹改为 capabilityCode/version/recordId/idempotencyKey/arguments 的递归排序 canonical JSON SHA-256 短摘要；键顺序不影响摘要，请求内容变化必然改变摘要。

Fix 验证：Secure Actions 27/27、Control Plane 27/27、Generator 专项 7/7、MCP 16/16、Identity 35/35、Admin 40/40 聚合通过；Mapper XML 和静态安全扫描通过。当前状态为 `fixed_pending_review`，等待再次 Review，复审通过前不归档或进入 Flow Actions。

## 13. 修复后复审与归档结论

> review-date: 2026-07-12
> conclusion: PASS

### 13.1 Spec Compliance

- R1：运行时 `arguments` Schema 与显式字段校验均使用能力版本、grant 和发布模型字段交集，PASS；
- R2：授权版本的 source binding、Schema、policy、风险和可见性统一来自 resolved `ai_capability_version`，PASS；
- R3：发布和调用共用步骤校验器，顶层与嵌套 `steps/stepList/childSteps`、未知和禁用步骤均失败关闭，PASS；
- R4：目录、授权和审计故障稳定返回 `CATALOG_UNAVAILABLE`、`AUTHORIZATION_UNAVAILABLE`、`AUDIT_UNAVAILABLE`，PASS；
- R5：elicitation 后、业务写入前先预留 Capability 审计，最终状态按完整可信身份条件更新，PASS；
- R6：elicitation 指纹绑定 capability/version/recordId/idempotencyKey/arguments 的 canonical SHA-256，PASS；
- 单 `/mcp` Streamable HTTP、USER 委托身份、发布快照、幂等和禁用旧传输/认证依赖边界保持，PASS。

### 13.2 Code Quality

- 未发现 Critical、Important 或阻塞归档的 Minor 问题；
- SQL 位于 Mapper XML，目录分页为稳定 keyset，异常日志不记录参数值和原始异常消息；
- 审计、幂等、发布快照和执行身份均保持失败关闭，模块依赖方向未形成循环依赖。

### 13.3 验证与归档

- 2026-07-12 14:30 复跑 Secure Actions：27/27 PASS；
- `SecureActionCatalogMapper.xml` 解析、V1.0.23 Flyway placeholder 扫描和相关 `git diff --check` PASS；
- 复用 Fix 阶段 Control Plane 27/27、Generator 7/7、MCP 16/16、Identity 35/35、Admin 40/40 成功基线；
- 真实 MySQL Flyway、真实外部 MCP elicitation 客户端和真实模型调用仍按条件跳过，不冒充 PASS；
- HARD-GATE 全部通过，允许归档并进入 `forge-ai-hub-flow-actions`。
