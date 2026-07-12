# Forge AI 中枢高风险动作人工审批

> status: done
> created: 2026-07-12
> complexity: 🔴复杂
> parent-stage: Forge AI 中枢阶段 2.3 — R3 高风险人工审批

## 1. 背景与目标

阶段 2.0～2.2 已完成可信 USER 委托身份、MEDIUM 受控业务动作和流程 START/APPROVE/REJECT。当前 `BUSINESS_ACTION/MEDIUM` 在 MCP elicitation 确认后可同步执行，但资金、权限、关键状态等 R3 动作不能由 Agent 直接完成。

本变更交付阶段 2.3 最小闭环：把显式发布为 `BUSINESS_ACTION/ACTION/HIGH` 的动作转换为可恢复审批请求，保存不可变信封加密快照，启动专用 Flowable 人工审批；审批通过回调必须重新授权并使用原幂等键一次性执行。MCP 新增固定查询工具 `capability.approval.get`，不动态发布审批实例 Tool。

## 2. 成功标准

- HIGH 动作调用只返回 `PENDING_APPROVAL + approvalRequestId`，审批前业务副作用为 0；
- 相同 tenant/client/capability/idempotencyKey 的 20 路并发只创建一条有效审批记录和一个 Flowable 实例；
- 请求参数使用每记录独立 AES-256-GCM DEK 加密，DEK 使用外部 Secret 提供的版本化 256-bit KEK 包装；数据库、日志和响应均不出现明文参数、DEK 或 KEK；
- 审批 businessKey 固定为 `capability-approval:<approvalId>`，专用 BPMN 非空时不得被代码覆盖；
- APPROVED 回调按 approvalId `FOR UPDATE`，重新校验客户端状态/credentialVersion、grant、服务账号、USER A、tenant/org、能力/版本/策略、动作权限与业务状态摘要；
- 重复回调、消息重投、并发恢复最多执行一次；过期、驳回、取消、授权撤销、能力下线、版本漂移和业务状态变化均不执行；
- `capability.approval.get` 只允许原 client + actor 查询安全状态摘要，不返回密文、参数、任务详情或密钥元数据；
- MCP 继续使用协议 `2025-06-18` 的单 Streamable HTTP，不增加旧 SSE/STATELESS/stdio/ASYNC；
- 不引入 Spring Authorization Server、Sa-Token OAuth2、Nacos MCP Registry/Admin，不调用真实模型。

## 3. 范围

### 3.1 包含

- 新增 `forge-plugin-capability-high-risk-approval` 组合模块；
- 新增 `ai_capability_policy` 与 `ai_capability_approval`；
- `BUSINESS_ACTION/HIGH` 专用发布入口与审批策略；
- `CapabilityPayloadCrypto` 和外部 Secret 版本化 KEK 实现；
- `RESERVED → PENDING_APPROVAL → EXECUTING → SUCCESS/FAILED/REJECTED/EXPIRED/CANCELLED` 状态机；
- 专用 Flowable 模型、只读审批表单上下文和流程回调；
- 回调重新授权、业务状态摘要复核和原幂等键执行；
- 固定 `capability.approval.get` MCP 工具；
- Flyway、权限、TDD 与阶段 2 回归。

### 3.2 明确不做

- 不允许 HIGH 动作在 MCP 请求线程直接执行业务副作用；
- 不开放 DELETE、任意 CRUD、任意 SQL/Mapper/URL/Header、任意 Flowable API；
- 不把现有 MEDIUM 能力静默升级为 HIGH，也不允许同一不可变版本改变风险等级；
- 不让客户端传审批人、候选组、flowModelKey、过期时间、tenantId/userId/activeOrgId；
- 不实现通用 KMS SDK；当前只提供 KMS 可替换 SPI 和外部 Secret 版本化 KEK；
- 不建设第二套流程设计器或独立审批任务页；复用现有 Flow 待办和节点配置；
- 不实现消息发送、Nacos、Agent Runtime、真实模型自主决策。

## 4. 架构

```text
/mcp Streamable HTTP + fdu USER A
  -> capability.invoke
  -> 实时 scope/grant/permission/tenant/org + HIGH 版本
  -> MCP elicitation ACCEPT
  -> Capability invocation audit RESERVED
  -> HighRiskApprovalExecutionAdapter
       -> 独立短事务预占 ai_capability_approval(RESERVED)
       -> canonical request digest + business state digest
       -> AES-256-GCM payload + wrapped DEK
       -> capability-approval:<id> 启动专用 Flowable
       -> PENDING_APPROVAL
  -> 返回 approvalRequestId

Flow callback APPROVED
  -> SELECT approval FOR UPDATE
  -> expiry/client/grant/user/capability/version/policy/business state 重新校验
  -> 解密并校验 request digest
  -> BusinessActionExecutionService.executePublished(original idempotencyKey)
  -> SUCCESS/FAILED
```

模块依赖固定为 High Risk → Secure Actions/Control Plane/Identity/Generator/Flow Client/MCP SPI。Capability Core 和 MCP transport 不反向依赖高风险模块。

## 5. 发布与调用契约

### 5.1 HIGH 发布

- source 仍为 `BUSINESS_ACTION`，behavior=`ACTION`，riskLevel=`HIGH`；
- 复用已发布对象版本、动作步骤递归白名单、字段白名单和不可变 Schema；
- HIGH 使用专用发布 API，必须同时创建启用 policy；
- policy 固化 capabilityId/version、专用 flowModelKey、服务端候选组、有效秒数、`approvalMode=FLOWABLE`；
- MEDIUM 发布器拒绝 HIGH，通用 Control Plane 发布 API 继续拒绝 BUSINESS_ACTION。

### 5.2 invoke 输出

```json
{
  "executeStatus": "PENDING_APPROVAL",
  "message": "高风险动作已提交人工审批",
  "correlationId": "<requestId>",
  "idempotentHit": false,
  "approvalRequestId": "<snowflake-id>"
}
```

重复相同请求返回同一 approvalRequestId，并将 `idempotentHit=true`。异摘要同幂等键返回 `IDEMPOTENCY_CONFLICT`。

### 5.3 approval.get

输入只允许 `approvalRequestId`。输出只包含 approvalRequestId、capabilityCode/version、status、resultCode、submittedAt、expiresAt、completedAt、correlationId 和安全 message。

## 6. 加密与密钥规则

- `CapabilityPayloadCrypto` 为协议无关 SPI；默认实现每条记录生成 32 字节随机 DEK；
- payload 使用 AES/GCM/NoPadding、12 字节随机 IV、128-bit auth tag；auth tag 与 ciphertext 分列保存；
- KEK 从 `forge.capability.high-risk.crypto.keys.<keyId>` 外部配置读取 Base64 32 字节值，activeKeyId 指定写入版本；
- DEK 使用 AES Key Wrap 生成 wrappedDek；AAD 绑定 tenantId/approvalId/clientId/capabilityId/version/requestDigest；
- active key 缺失、长度错误、旧 key 不可用、tag/digest 不匹配一律 `APPROVAL_CRYPTO_UNAVAILABLE` 或 `APPROVAL_PAYLOAD_INVALID`，不得执行；
- 高风险功能默认关闭；启用但无有效 KEK 时启动失败关闭；示例配置只写环境变量占位符，不提交真实密钥。

## 7. 审批状态机与重新授权

1. 原子预占唯一幂等域，异摘要冲突；
2. RESERVED 可恢复 Flow 启动，固定 businessKey 防止孤儿流程重复；
3. PENDING_APPROVAL 只接受一次终态回调；
4. REJECTED/CANCELLED/EXPIRED 不解密执行；
5. APPROVED 重新加载客户端、credentialVersion、grant、能力、版本、policy 和 USER A；
6. 重新计算当前业务状态摘要；与提交时不一致则 FAILED/BUSINESS_STATE_CHANGED；
7. 解密后重新执行 JSON Schema、字段白名单和发布动作校验；
8. 使用原 idempotencyKey 调用 `BusinessActionExecutionService.executePublished`；
9. 审批行锁、业务动作事务与最终状态同一本地事务；重复回调读取终态并直接返回。

## 8. Flowable 规则

- 模型 key 固定 `forge_capability_high_risk_approval`，businessType 固定 `capability-approval`；
- 默认 BPMN 只含一个人工审批节点，候选组从服务端 policy 变量注入，不接受 MCP 参数；
- formKey 固定 `forge_capability_high_risk_approval_form`，表单只读展示能力、来源、风险、申请人、字段变更摘要、提交/过期时间；
- 模型不存在时允许创建默认模型；已有非空 BPMN 永不覆盖，只在未部署时部署现有设计；
- `PROCESS_COMPLETED/REJECTED/CANCELED` 回调必须幂等；回调异常保留 PENDING/FAILED 对账证据，不吞异常冒充成功。

## 9. 数据变更

新增下一版本 Flyway：

- `ai_capability_policy`：capability/version、HIGH、flowModelKey、candidateGroup、expireSeconds、status、del_flag 和标准审计字段；
- `ai_capability_approval`：幂等域、双身份、credentialVersion、请求/业务摘要、keyId/wrappedDek/iv/ciphertext/authTag、流程关联、状态、稳定结果和标准审计字段；
- 两表均逻辑删除并使用 `logic_delete_active` 约束未删除唯一记录；
- 新增 HIGH 发布、审批查询和审批提交权限资源，tenant_id=1、显式列名、NOT EXISTS；
- 不写入真实 policy、审批记录、Token、KEK/DEK 或业务数据。

## 10. HARD-GATE

- 没有有效外部 KEK、HIGH policy、USER 委托、scope、grant、权限、发布快照或 elicitation 时，审批记录和业务副作用均为 0；
- HIGH invoke 永远不得进入同步 `executePublished`；
- 审批通过后的任一重新授权失败、过期或业务状态变化均不得执行；
- 相同幂等键与重复回调不得产生第二次业务副作用；
- 明文 payload、comment、Token、Header、DEK/KEK 不得写日志、普通审计或 MCP 输出；
- MCP 只允许 Streamable HTTP；任一 HARD-GATE 失败时阶段 2 不得标记完成。

## 11. 回滚

- `forge.capability.high-risk.enabled=false` 停止 HIGH 发布、提交和回调执行；已存在记录保留可审计但不自动执行；
- 停用 HIGH capability/policy 或撤销 grant 后，待审批请求在回调时失败关闭；
- 旧 keyId 在审批与留存期内必须继续配置，不能先删 KEK 再回滚代码；
- 新表保留，不 DROP；回滚不得把 HIGH 降级为 MEDIUM 同步执行。

## 12. 确认记录

- **确认时间**：2026-07-12；
- **确认方式**：用户要求阶段 2.2 归档后自动进入下一阶段并继续完成；
- **批准范围**：阶段 2.3 HIGH BUSINESS_ACTION 人工审批，不包含 DELETE、消息、Nacos 或 Agent Runtime；
- **执行方式**：Proposal 后直接 Apply，禁止 commit/push，不调用真实模型。

## 13. Apply 实现记录

- 新增 `forge-plugin-capability-high-risk-approval` 并接入 plugin parent、BOM 和 Admin；功能默认关闭，启用时必须提供有效外部 KEK；
- HIGH 发布专用入口先保证固定 Flowable 模型存在，再创建不可变 HIGH capability 与启用 policy；MEDIUM 发布和同步执行保持不变；
- 每条审批请求使用独立 AES-256-GCM DEK，DEK 由版本化 256-bit KEK 通过 AES Key Wrap 包装，AAD 绑定 tenant/approval/client/capability/version/requestDigest；
- HIGH invoke 在 elicitation 之后只预占加密审批记录并启动 `capability-approval:<id>`，返回 `PENDING_APPROVAL + approvalRequestId`，不会在 MCP 请求线程调用业务执行服务；
- 默认 BPMN 只有一个人工审批节点，候选组只来自 policy 变量；已有非空 BPMN 永不覆盖；
- 审批表单复用 `BusinessCodeFormProvider`，只读展示能力、申请人、目标记录、变更摘要和时间，密钥/密文/流程任务信息不返回；
- APPROVED 回调按 tenant/approvalId `FOR UPDATE`，重查 policy、client、credentialVersion、服务账号、USER A、tenant/org、grant、能力版本、动作权限、发布模型、业务状态和加密摘要，然后以原幂等键进入 `EXECUTING` 并至多执行一次；
- 固定 `capability.approval.get` 只允许原 client/actor/serviceUser/tenant/activeOrg 查询安全状态；
- MCP 继续锁定 `2025-06-18`、SYNC、单 Streamable HTTP，未新增旧 SSE、stdio、STATELESS 或 ASYNC。

## 14. 首轮 Review 结论

> review-date: 2026-07-12
> conclusion: NEEDS_FIX

Spec Compliance 已覆盖主体架构，但发现 7 个问题：回调未自动装配且事件类包名错误；幂等复用未绑定原 USER A；回调缺 policy/服务账号/客户端过期等重新授权；HIGH 提交前未复核动作步骤和发布字段；流程模型 lookup 故障被当作不存在；默认 BPMN/只读表单/approval.get 缺失；送审结果的 output Schema 和 Capability 审计仍按同步 SUCCESS 建模。

首轮不允许归档，问题明细和修复顺序见 `fix-plan.md`。

## 15. Fix 实现记录

- 回调完成 Bean 装配并使用正确 `FlowEventContext`；
- 幂等重放精确比较 capabilityVersion、actorUserId、serviceUserId、activeOrgId 和 credentialVersion，跨用户/凭据碰撞失败关闭；
- 回调重查固定 policy/flowModelKey、客户端有效期与凭据版本、服务账号和 USER A 状态/tenant/org，再由实时 Catalog 复核 grant、权限、能力版本和字段交集；
- HIGH adapter 在审批记录产生前重新解析不可变发布动作，验证递归步骤白名单、模型可写字段和 arguments 字段；
- Flow 模型查询非成功直接失败关闭；只有成功空结果才创建默认模型，非空设计永不覆盖；
- 补齐默认 BPMN、只读表单和 `capability.approval.get`，并对 secret-like 表单字段二次遮蔽；
- invoke output Schema 增加可选 approvalRequestId，送审 Capability 审计写 `PENDING_APPROVAL`；
- 补充 20 路并发、跨 USER 幂等冲突、模型保留/故障、加密篡改/轮换、回调重复/撤销、服务账号停用、查询归属和表单脱敏测试。

## 16. 二次 Review 结论

> review-date: 2026-07-12
> conclusion: PASS

### 16.1 Spec Compliance

阶段 2.3 范围已满足：`BUSINESS_ACTION/ACTION/HIGH` 不同步执行；外部版本化 KEK 和不可变密文快照；专用 Flowable 审批；回调重新授权与业务状态复核；原幂等键至多一次执行；固定 ownership 查询工具；单 Streamable HTTP 和禁止依赖边界均通过。

### 16.2 Code Quality

模块依赖方向、Mapper XML、tenant/del_flag、逻辑删除唯一键、异常失败关闭、明文隔离、默认模型保留和条件自动配置通过。Review Fix 后未发现阻断归档的问题。

### 16.3 验证

- High Risk 24/24；Secure Actions 32/32；Flow Actions 19/19；
- Flow Client 2/2；认证委托桥 7/7；Flow Controller 3/3；
- Control Plane 29/29；Identity 35/35；MCP 16/16；
- Admin + Flow Server 44 模块聚合 package PASS；Mapper/Flyway/禁止依赖/旧 transport 静态检查 PASS。

### 16.4 条件边界

未执行真实 MySQL Flyway、真实 Flowable 人工审批 E2E 或真实模型调用；这些必须在有隔离环境和审批账号时完成，当前不会被表述为已联调。

最终状态为 `reviewed_passed`，允许归档阶段 2.3。阶段 2 的代码建设完成，真实环境总体验收仍保留上述条件项。

## 17. 归档结论

> archive-date: 2026-07-12
> archive-status: done

- 归档前复跑 `mvn -pl forge-admin-server,forge-flow/forge-flow-server -am package -DskipTests`，44/44 模块 `BUILD SUCCESS`；
- Mapper XML、V1.0.26 Flyway placeholder/tenant、禁止依赖、旧 transport、尾随空白与 `git diff --check` 均通过；
- 未启动 Admin/Flow 服务，未执行真实 Flyway、Flowable 人工审批 E2E 或真实模型调用；
- 阶段 2.3 归档完成，阶段 2 整体仍需隔离环境总体验收后才能标记完成。
