# Forge AI 中枢受控流程动作修复计划

> change: `forge-ai-hub-flow-actions`
> source: Review R1–R6
> method: 增量 TDD；先证明 HARD-GATE 失败，再实施最小修复

## 1. R1 — Flow 服务最终授权

1. 为 `FlowTaskServiceImpl` 增加测试：任务已转签、任务租户不匹配、任务已完成时，`approve/reject` 必须在 Flowable `complete` 前失败。
2. 办理命令显式携带可信 `tenantId`，Flow 服务加载 `sys_flow_task` 后核对任务状态、assignee、tenantId，并再次核对 Flowable 当前 assignee。
3. 保留节点 BPMN 动作策略为权威规则，不在 Capability 层复制节点配置。

## 2. R2 — MCP 顶层字段失败关闭

1. 为 `userId/tenantId/activeOrgId/flowModelKey/variables/processInstanceId/unknown` 分别增加零副作用失败测试。
2. `SecureActionMcpHandler` 在读取、投影任何执行字段前校验顶层 key 白名单，仅允许 `capabilityCode/version/recordId/idempotencyKey/arguments`。
3. 错误使用稳定参数校验结果，不回显危险字段值。

## 3. R3/R4 — 远程幂等与恢复、固定 businessKey

1. START 固定使用 `<objectCode>:<recordId>`；已有历史流程时不生成 `:R...`，FLOW_ACTION 重复调用依赖 Flow 服务既有 businessKey 幂等返回原实例。
2. APPROVE/REJECT 向 Flow 服务传递 `requestId/idempotencyKey/tenantId`，Flow 服务持久化任务动作幂等凭证；同请求返回既有结果、异请求拒绝。
3. Admin 对已产生远程结果但本地 SUCCESS/link 失败的日志允许恢复：使用同一幂等凭证重新查询/调用，取得既有远程结果后回填本地链接和结果快照。
4. 数据库变更新增 `V1.0.25__add_flow_task_action_idempotency.sql`，不修改 V1.0.24；保留审计数据并提供开关回滚。

## 4. R5 — 发布版本事实锚定

1. 增加缺少发布版本、版本状态 FAILED/非 PUBLISHED、主表版本漂移测试。
2. `FlowActionSourceMapper.xml` 使用 `ai_business_object_design_version` 的同对象/套件/编码/版本 PUBLISHED 事实作为来源，并从该事实返回 sourceVersion。

## 5. R6 — 目录元数据与覆盖缺口

1. search/describe 输出 `sourceType/behavior/operation`，同步工具输出 Schema。
2. 增加 START 成功/固定 businessKey、binding/version drift、转签/跨租户、远程成功本地失败恢复、目录元数据测试。
3. 复跑 Flow Actions、Secure Actions、Flow plugin、Generator、Control Plane、Identity、MCP、Admin 聚合；不启动真实模型，不执行真实 Flyway。

## 6. 收口

1. 将命令、结果、警告、跳过项和清理情况增量追加到 `execution-log.md`。
2. 将任务状态更新为 `fixed_pending_review`，重新执行两阶段 Review。
3. Review 通过后归档本变更，再创建阶段 2.3 高风险审批提案。

## 7. 二次 Review 增量项

1. R7：SUCCESS 同摘要在任务可办理预检前复用；活动 RUNNING 提前冲突；FAILED/超时 RUNNING 才恢复。
2. R8：增加 MCP USER 到独立 Flow 服务的 60 秒 Sa-Token 委托桥、专用 delegated START 和 Session marker；任何签发失败禁止静态账号降级。
3. 委托 Token 使用唯一 device，空 Token/Session、普通 Session 和非正 clientId 失败关闭。
4. FLOW source binding 增加 targetId 精确关联；Flow 任务结果写回必须成功，否则回滚。
5. 二次 Review 与共享回归全部通过，状态更新为 `reviewed_passed`。
