# Fix Plan — Forge AI 中枢高风险动作人工审批

> status: completed
> created: 2026-07-12

## Review Findings

1. **R1 / P0 — 回调尚未进入自动配置且 FlowEventContext 包名错误**：回调不能被 Spring 注册，窄编译失败。
2. **R2 / P0 — 幂等键可能跨 USER A 复用审批 ID**：唯一域按 client/capability 定义，但复用时未重新比较 actor/serviceUser/org/credentialVersion/version。
3. **R3 / P0 — 审批回调重新授权不完整**：缺少 policy/固定模型、客户端过期、服务账号状态和 USER A tenant/org 精确复核。
4. **R4 / P0 — HIGH 提交前只校验 policy**：未重新解析发布动作、步骤白名单和发布模型可写字段。
5. **R5 / P1 — 流程模型查询故障被当成模型不存在**：可能在基础设施故障时错误尝试创建模型。
6. **R6 / P1 — 默认 BPMN、只读表单和 approval.get 未实现**：阶段 2.3 不具备完整人工审批体验和安全查询闭环。
7. **R7 / P1 — invoke 输出与审计仍按同步成功建模**：output Schema 缺 approvalRequestId，送审结果被记为 SUCCESS。

## Fix Result

- R1～R7 全部完成；High Risk 24/24、Secure Actions 32/32 及阶段 2 回归通过；
- 二次 Review 未发现新的阻断项；真实 Flyway/Flowable E2E 仍作为环境验收项保留。
