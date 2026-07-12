# Forge AI 中枢受控流程动作

> status: reviewed_passed
> created: 2026-07-12
> complexity: 🔴复杂
> parent-stage: Forge AI 中枢阶段 2.2 — 流程闭环

## 1. 背景与目标

阶段 2.0 已建立 MCP USER/SERVICE 双身份，阶段 2.1 已交付已发布业务动作、字段白名单、幂等、elicitation 和双审计。当前 Agent 仍不能通过能力目录安全发起或办理 Flowable 流程。

本变更交付阶段 2.2 的最小流程闭环：把业务对象当前启用的真实 FLOW 绑定发布为不可变 `FLOW_ACTION` 能力，继续通过固定 `capability.search/describe/invoke` 发现和调用。首批只开放 `START`、`APPROVE`、`REJECT`，实际发起人/办理人固定为可信 USER 委托身份 A；任务、业务对象、流程模型、候选/签收状态和当前业务状态必须由服务端实时解析，不能信任客户端自报身份或任意 taskId。

## 2. 成功标准

- MCP 继续只有单 `/mcp` Streamable HTTP，固定元工具不变，不新增旧 SSE、STATELESS、stdio 或 ASYNC Server；
- `capability.search/describe/invoke` 同时支持 `BUSINESS_ACTION/ACTION/MEDIUM` 与 `FLOW_ACTION/FLOW/MEDIUM`，仍按 Token scope、client grant、当前用户权限、tenant/activeOrg 实时过滤；
- 流程能力只能从启用业务对象、已发布对象版本和当前启用 FLOW 绑定发布，`flowModelKey`、bindingId、objectCode、operation 固化到不可变能力版本；
- START 只通过平台 `BusinessFlowService.startDocumentFlow` 发起，固定 `businessKey=<objectCode>:<recordId>`，不接受客户端 flowModelKey、variables、发起人或组织；
- APPROVE/REJECT 只办理当前 A 已签收、未完成、属于能力对象与流程模型的真实任务；taskId 只是定位符，不是授权依据；
- `BusinessTaskActionDTO.userId` 在服务层不再覆盖当前可信用户，FlowClient 办理人始终从当前执行身份解析；
- 三种操作均要求 16～128 位幂等键和同一次 MCP elicitation `ACCEPT + confirm=true`；同键同请求返回既有结果，同键异请求拒绝；
- 20 路并发相同幂等请求最多产生一次流程发起或任务完成副作用；
- Capability 审计、流程动作日志和流程实例/任务可通过 requestId、client、actor、serviceUser、tenant/org 关联；
- 错误返回稳定代码，不泄露 Flowable 内部异常、Token、Header、完整变量或业务敏感数据；
- Flow Actions、Secure Actions、Generator、Control Plane、Identity、MCP 与 Admin 聚合验证通过，不调用真实模型。

## 3. 范围

### 3.1 包含

- 新增 `forge-plugin-capability-flow-actions` 组合模块；
- 将阶段 2.1 固定元工具网关抽象为可扩展受控能力执行器，保留业务动作行为；
- `FLOW_ACTION` 专用发布 API、Schema、策略快照和 source binding；
- `START/APPROVE/REJECT` 运行时解析、USER 办理人校验和真实 FLOW 绑定复核；
- 流程动作幂等预留、结果快照和双身份关联日志；
- MCP elicitation R2 确认与稳定错误映射；
- 修复 `BusinessFlowService.completeBusinessTask` 接受 DTO userId 覆盖当前用户的问题；
- Flyway 权限资源和流程动作幂等日志表；
- TDD、模块测试和聚合验证。

### 3.2 明确不做

- 不开放 claim、delegate、return、withdraw、terminate、转办、加签、减签或任意 Flowable API；
- 不允许客户端传 flowModelKey、processDefinitionId、processInstanceId、tenantId、userId、activeOrgId 或任意 variables；
- 不通过任意 taskId 绕过当前 A 的签收人与任务归属校验；
- 不建设第二套节点配置 UI；节点表单、字段权限、审批/驳回规则继续归真实流程设计器 BPMN 节点；
- 不覆盖或重发用户已编辑的 BPMN XML；
- 不把代码业务的自定义 submit 方法自动推导为通用 START；本阶段 START 仅覆盖平台托管且可由 `BusinessFlowService.startDocumentFlow` 加载的已发布业务对象；
- 不实现 R3 高风险审批、消息能力、Nacos MCP Registry/Admin 或 Agent Runtime；
- 不引入 Spring Authorization Server 或 Sa-Token OAuth2；
- 不调用真实模型。

## 4. 架构

```text
/mcp Streamable HTTP
  -> fdu USER + ExecutionIdentity(A/client/serviceUser/tenant/org)
  -> capability.search / capability.describe / capability.invoke
  -> GovernedCapabilityExecutionAdapter
       -> BUSINESS_ACTION adapter（阶段 2.1，保持行为）
       -> FLOW_ACTION adapter（本阶段）
  -> resolved ai_capability_version + grant policy
  -> MCP elicitation ACCEPT
  -> Capability 审计预留
  -> FlowActionExecutionService 幂等预留
       START   -> BusinessFlowService.startDocumentFlow
       APPROVE -> task access/object/model 校验 -> completeBusinessTask
       REJECT  -> task access/object/model 校验 -> completeBusinessTask
  -> ai_capability_flow_action_log + ai_capability_invocation_log + Flowable
```

`forge-plugin-capability-secure-actions` 继续拥有固定元工具和公共执行闸门；新增协议无关的受控执行适配 SPI。Flow Actions 模块依赖 Secure Actions、Generator 和 Flow Client，只注册 `FLOW_ACTION` 解析/执行适配器，MCP 模块不依赖 Flowable 或 Generator。

## 5. 能力契约

### 5.1 FLOW_ACTION source binding

- `source_type=FLOW_ACTION`；
- `source_key={suiteCode}/{objectCode}/{operation}`，operation 仅 `START/APPROVE/REJECT`；
- `source_version={publishedObjectVersion}`；
- `behavior=FLOW`、`risk_level=MEDIUM`、`visibility=DISCOVERABLE`；
- `policy_snapshot` 固化 `bindingId`、`flowModelKey`、`operation`、`publishedObjectVersion`、`permission`、`confirmationMode=MCP_ELICITATION`；
- grant `field_policy.allowedOperations` 必须非空且只能缩小版本操作集合；每个能力版本首期只对应一个 operation。

### 5.2 输入契约

- 公共字段：`capabilityCode`、可选 `version`、`recordId`、`idempotencyKey`、`arguments`；
- START：`recordId` 必填，`arguments` 必须为空对象；
- APPROVE：`recordId`、`arguments.taskId` 必填，`arguments.comment` 可选且最多 500 字；
- REJECT：`recordId`、`arguments.taskId`、非空 `arguments.comment` 必填且最多 500 字；
- 所有对象 `additionalProperties=false`，ID 按字符串处理；
- 输出沿用 `executeStatus/message/correlationId/idempotentHit`，不返回完整 Flowable 任务或变量。

## 6. 安全与业务规则

1. 只有 USER Token 可 invoke；SERVICE Token 仍可发现但不能产生流程副作用；
2. 发布时和调用时都复核业务对象启用、发布版本、FLOW bindingId 和 flowModelKey；漂移返回 `FLOW_BINDING_MISMATCH`，不自动跟随草稿或新绑定；
3. START 不接受 flowModelKey/title/variables，记录和权限由 `BusinessFlowService` 读取已发布运行配置与真实绑定；
4. APPROVE/REJECT 必须先加载 task detail，校验 status 未完成、assignee 等于 A、businessKey 可规范化为能力 objectCode + recordId、processDefKey 等于能力 flowModelKey；
5. DTO 中的 userId 永远忽略，FlowClient userId 固定使用当前 A；
6. REJECT comment 必填；APPROVE comment 可选；不允许客户端写任意流程 variables；
7. elicitation 摘要只展示操作、对象、recordId、taskId 安全尾号和 canonical 请求短指纹，不展示业务字段值；
8. 幂等唯一域为 tenant/client/capability/operation/idempotencyKey；request digest 绑定版本、recordId、taskId、comment；
9. 流程动作日志先预留 `RUNNING` 再产生副作用；活动 RUNNING 返回冲突，SUCCESS 返回既有结果；同摘要 FAILED 或超过 30 秒的 RUNNING 使用同幂等键进入受控恢复，异摘要始终拒绝；
10. Capability 审计预留失败、流程日志预留失败、授权目录故障或 Flow 服务不可用均失败关闭；
11. 任务完成后的业务状态同步继续走现有 BusinessFlowService、Flowable 回调和业务 Provider，不在能力层复制状态机；
12. 日志不得记录 comment 正文、流程 variables、Token、Header 或原始供应商/Flowable异常消息。

## 7. 数据变更

新增 `V1.0.24__add_capability_flow_actions.sql`：

- 创建 `ai_capability_flow_action_log`，包含标准租户/审计字段、`del_flag`、request digest、幂等键、operation、task/process 安全引用、双身份和结果摘要；
- 使用生成列 `logic_delete_active` 与唯一索引约束未删除幂等记录；
- 增加发布/调用受控流程动作权限资源并授权内置 admin；
- 所有内置数据 `tenant_id=1`、显式列名、NOT EXISTS/信息架构防重复；
- 不插入真实流程能力、grant、client、Token 或业务数据；
- 不修改已执行的 V1.0.21～V1.0.23。

Review 修复新增 `V1.0.25__add_flow_task_action_idempotency.sql`：

- 为 `sys_flow_task` 增加动作幂等键、规范请求摘要和动作类型；
- 三个字段与任务完成状态在 Flow 服务同一事务内写入，用于跨服务失败后的同请求恢复；
- 使用 `information_schema` 防重复增加字段和索引，不修改已执行的 V1.0.24。

## 8. HARD-GATE

- 任一可信身份、scope、grant、权限、tenant/org、发布对象、FLOW binding、任务签收人、对象/流程归属、幂等或 elicitation 校验失败时，不得调用 FlowClient；
- 客户端 userId/flowModelKey/variables 不得进入执行链；
- 同幂等键并发不得重复启动流程或完成任务；
- APPROVE/REJECT 不得办理 A 未签收的任务；
- 流程设计器 BPMN 节点配置仍为运行时权威来源；
- MCP 仍只有单 `/mcp` Streamable HTTP；
- 任一硬闸门失败时不得进入 `forge-ai-hub-high-risk-approval`。

## 9. 回滚

- `forge.capability.flow-actions.enabled=false` 关闭 FLOW_ACTION 发布和执行适配器，保留阶段 2.1 业务动作；
- 已发布 FLOW_ACTION capability/grant 可由控制面停用或撤销；
- 新日志表和审计数据保留，不自动 DROP；
- 回滚不得恢复 DTO userId 覆盖、任意 taskId、裸 Header 身份或旧 SSE。

## 10. 确认记录

- **确认时间**：2026-07-12；
- **确认方式**：用户要求归档阶段 2.1 并自动开始、执行下一阶段；
- **批准范围**：阶段 2.2 START/APPROVE/REJECT，不包含 R3、消息、流程管理 API 或代码业务自定义 START SPI；
- **执行方式**：Proposal 后直接 Apply，禁止 commit/push。

## 11. Apply 实现记录

- 新增 `forge-plugin-capability-flow-actions`，完成 plugin parent、BOM、Admin 聚合和条件自动配置；
- `FLOW_ACTION/FLOW/MEDIUM` 继续通过固定 `capability.search/describe/invoke` 暴露，没有增加动态顶层 Tool 或第二 MCP 端点；
- 发布快照固化 `bindingId/flowModelKey/publishedObjectVersion/operation`，调用前实时复核当前启用、已发布对象和真实 FLOW 绑定；
- START 只调用 `BusinessFlowService.startDocumentFlow`；APPROVE/REJECT 在 elicitation 前通过 `getActionableTaskFormContext` 确认任务已由 A 签收，并在执行时再次校验；
- `BusinessFlowService.completeBusinessTask` 已忽略 DTO `userId`，FlowClient 办理人只取当前可信用户；
- 流程幂等预留使用独立事务，本地流程编排与 SUCCESS 日志更新处于同一事务；最终日志更新失败时回滚本地事务并以独立事务落 FAILED，避免记录长期停留 RUNNING；
- FlowClient 可独立部署，远程流程副作用不属于本地数据库事务。该边界失败时返回 `FLOW_AUDIT_UNAVAILABLE` 并保留失败/待对账证据，不宣称分布式原子提交；START 的 businessKey 和任务完成状态仍阻止重复业务副作用；
- elicitation 不显示 comment 或完整 taskId，只显示 operation、对象、recordId、任务安全尾号和请求短指纹；
- 未引入 Spring Authorization Server、Sa-Token OAuth2、Nacos MCP Registry/Admin、旧 SSE 或真实模型调用。

## 12. Review 结论

> review-date: 2026-07-12
> conclusion: NEEDS_FIX

### 12.1 Spec Compliance

已满足：单 `/mcp` Streamable HTTP、固定元工具、FLOW_ACTION/FLOW/MEDIUM、专用发布入口、USER 委托身份、FLOW binding 快照、START/APPROVE/REJECT 范围、MCP elicitation、流程动作日志表、逻辑删除唯一键、稳定错误摘要和禁止依赖边界均已落地。

存在六个 Spec 不合规项：

1. **R1 / P0 — 最终 Flow 服务没有重新绑定可信办理人和任务租户。** Admin 侧在 elicitation 前和执行前调用 `getActionableTaskFormContext` 检查 A 已签收，但 `FlowTaskServiceImpl.approve/reject` 收到 `userId` 后只校验节点动作策略，没有比较 `task.assignee == userId`，也没有用任务 `tenantId` 与可信租户做最终校验。确认后转签的 TOCTOU 竞态，或内部 Flow 接口被直接调用时，仍可能由非当前办理人完成任务，违反 §2、§6.4、§8 HARD-GATE。
2. **R2 / P0 — 顶层伪造参数被静默丢弃而不是失败关闭。** `SecureActionMcpHandler.invoke` 从原请求重建 `targetInput` 时只复制 `recordId/idempotencyKey/arguments`，因此顶层 `userId/tenantId/activeOrgId/flowModelKey/variables/processInstanceId` 和任意未知字段不会进入运行时 Schema 校验。MCP Tool 广告的 `additionalProperties=false` 不能替代服务端校验，当前实现违反 §3.2、§5.2、§6.3/6.6 与 HARD-GATE。
3. **R3 / P0 — 远程 Flow 副作用与本地幂等/审计仍可能形成孤儿流程。** 流程日志先以 `REQUIRES_NEW` 提交 RUNNING，本地事务随后包含远程 `FlowClient` 调用和 SUCCESS 日志更新。独立 Flow 服务一旦已提交、而本地 link/日志事务失败，远程流程或任务已变化，但本地记录会转 FAILED 或缺少实例 link；同键重试冲突，换键 START 又可能重复发起。当前没有远程幂等键、可恢复 outbox 或对账回填路径，违反 §2 的并发/关联成功标准和 §6.8～6.10。
4. **R4 / P1 — 固定 businessKey 契约存在实现偏差。** `BusinessFlowService.resolveFlowBusinessKeyForStart` 在记录已有历史 link 时把真正传给 Flowable 的 businessKey 改为 `<objectCode>:<recordId>:R...`，而本 Spec 冻结为 `<objectCode>:<recordId>`。若允许重新发起，应在能力契约和任务归属校验中显式建模 execution key；否则 FLOW_ACTION START 应拒绝有历史实例的记录。
5. **R5 / P1 — FLOW_ACTION 来源没有锚定实际已发布版本记录。** `FlowActionSourceMapper.xml` 只检查 `ai_business_object.design_status/last_publish_version`，没有验证同一 objectId、suiteCode、objectCode、publishVersion 的 `ai_business_object_design_version.publish_status=PUBLISHED` 记录存在。能力 sourceVersion 因而来自可漂移的主表数字，而不是可审计的发布版本事实，违反 §2、§5.1 的已发布对象版本要求。
6. **R6 / P1 — 目录输出和 P0 测试证据未达到 Tasks/Test Spec。** `capability.search/describe` 没有返回 Task 1 要求的 `sourceType/behavior/operation`；现有测试也没有覆盖 START 执行、顶层伪造参数拒绝、binding/version 漂移、任务转签/跨租户、远程成功后本地失败恢复。11/11 Flow Actions 与 29/29 Secure Actions 虽通过，但不能证明 test-spec §1 的这些 HARD-GATE。

### 12.2 Review Gate

- Spec Compliance 结论为 FAIL；按两阶段 Review 规则，本轮不进入 Code Quality Review；
- 当前状态更新为 `reviewed_with_findings`，不得归档，也不得把阶段 2.2 或整个阶段 2 标记为完成；
- 下一步执行 `/fix forge-ai-hub-flow-actions`，顺序固定为 R1 → R2 → R3/R4 → R5 → R6；
- Fix 后为每项补 Red/Green 证据，增量复跑 Flow Actions、Secure Actions、Flow/Generator、Control Plane、Identity、MCP 和 Admin 聚合，并再次执行 `/review`。

## 13. Fix 实现记录

> fix-date: 2026-07-12
> status: fixed_pending_review

- **R1**：Flow 服务办理前对 `sys_flow_task` 执行 `FOR UPDATE`，最终复核可信 tenant、原 assignee、任务状态，并再次比较 Flowable 当前 assignee；同请求完成态仅向同一办理人返回幂等成功。
- **R2**：`capability.invoke` 在任何字段投影、目录查询和 elicitation 前校验顶层字段白名单，伪造身份、租户、流程控制字段及 unknown 均返回 `INVALID_ARGUMENT`。
- **R3**：APPROVE/REJECT 把幂等键与规范摘要传到 Flow 服务并和任务状态同事务落库；本地 FAILED 或超时 RUNNING 可用同摘要恢复，活动 RUNNING 和异摘要拒绝。START 依赖 Flow 服务固定 businessKey 的既有幂等，远程成功而本地 link 失败时重试回填。
- **R4**：新增 `startDocumentFlowForCapability`，FLOW_ACTION START 永远使用 `<objectCode>:<recordId>`，普通人工流程的历史重发规则保持兼容。
- **R5**：来源 SQL 精确 JOIN `ai_business_object_design_version`，按 tenant/objectId/suiteCode/objectCode/publishVersion 且 `publish_status='PUBLISHED'` 锚定发布事实。
- **R6**：search/describe 实际结果和 output Schema 补齐 `sourceType/behavior/operation`；补充 START、顶层危险字段、跨租户/转签、跨用户幂等、FAILED/超时恢复和发布版本契约测试。
- 修复未新增旧 SSE、STATELESS、stdio、ASYNC、Nacos MCP Registry/Admin、Spring Authorization Server、Sa-Token OAuth2 或真实模型调用。

## 14. 二次 Review 结论

> review-date: 2026-07-12
> conclusion: PASS

### 14.1 新增审查项

- **R7 / P0 — 成功幂等重放被任务可办理预检阻断**：SUCCESS 同摘要现在直接复用安全结果；活动 RUNNING 在任务查询前返回 `IDEMPOTENCY_CONFLICT`；FAILED 与超时 RUNNING 才进入受控恢复。
- **R8 / P0 — MCP USER 身份无法跨独立 Flow 服务透传**：Admin 为可信 USER ExecutionIdentity 签发 60 秒内部 Sa-Token，Token Session 绑定 LoginUser、tenant、activeOrg、client 和委托标记；签发失败禁止降级静态服务账号。Flow 增加专用 delegated START，发起人、办理人和租户最终只取服务端 Session。
- delegated START 同时要求 `ai:businessFlow:start` 与内部委托标记；普通用户 Token 即使具有发起权限也不能调用内部入口。
- 每次委托签发使用唯一 device 会话，避免 Sa-Token 共享/并发策略复用或替换普通登录 Token；空 Token、空 Session、非正 clientId 全部失败关闭。
- FLOW binding 同时按 `target_id` 与 `target_code` 锚定真实业务对象，避免不同套件重名对象误绑定。
- Flowable 完成后的 `sys_flow_task` 状态与幂等凭证写回必须影响一行，否则抛错回滚同一事务。

### 14.2 两阶段结论

- Spec Compliance：R1～R8 和 HARD-GATE 均通过；单 `/mcp` Streamable HTTP、USER A、发布快照、确认、幂等与双审计边界保持不变。
- Code Quality：模块依赖方向、异常失败关闭、Session 隔离、Mapper XML、逻辑删除和迁移规则通过；未发现阻断归档的问题。
- 验证：Flow Actions 19/19、Flow 最终授权 7/7、Flow Client 2/2、认证委托桥 7/7、Flow Controller 3/3、Secure Actions 31/31、Identity 35/35、Control Plane 29/29、MCP 16/16；Admin + Flow Server 聚合 43/43 PASS。
- 条件边界：未执行真实 Flyway 和真实 Flowable START/APPROVE/REJECT E2E，也未调用真实模型；这些不影响代码归档，但仍是阶段 2 总体验收的环境验证项。

最终状态为 `reviewed_passed`，允许归档阶段 2.2。
