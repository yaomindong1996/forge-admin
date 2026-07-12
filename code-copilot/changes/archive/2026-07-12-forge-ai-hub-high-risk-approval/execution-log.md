# 执行日志 — Forge AI 中枢高风险动作人工审批

> change: `forge-ai-hub-high-risk-approval`
> status: done
> created: 2026-07-12

## 1. 启动记录

| 时间 | 阶段 | 事件 | 结论 |
|---|---|---|---|
| 2026-07-12 | Archive | 归档 `forge-ai-hub-flow-actions` | 二次 Review PASS；Flow Actions 19/19，MCP 16/16，聚合 43/43 |
| 2026-07-12 | Direction | 用户要求继续完成下一阶段 | 进入阶段 2.3 HIGH 人工审批 |
| 2026-07-12 | Proposal | 读取路线图 R3 状态机和阶段 2 闸门 | 采用加密快照 + 专用 Flowable + 回调重新授权 |

## 2. 已冻结边界

- 只处理显式 HIGH 的已发布 BUSINESS_ACTION；
- Agent 只提交审批，不直接执行；
- 外部 Secret 版本化 KEK，不复用前端报文密钥；
- 专用 Flowable 和现有待办，不建设第二套设计器；
- 固定 approval.get，不动态发布审批实例 Tool；
- 不含 DELETE、消息、Nacos、Agent Runtime 或真实模型调用。

## 3. Proposal 结论

状态为 `proposed`。用户已授权 Proposal 后自动 Apply；下一步按 tasks 增量 TDD 实施。

## 4. Apply 记录

| 时间 | 范围 | 结论 |
|---|---|---|
| 2026-07-12 | 模块/装配 | 新增 high-risk-approval，接入 plugin parent、BOM、Admin；44 模块聚合 package PASS |
| 2026-07-12 | HIGH 发布/路由 | 专用发布入口、真实 riskLevel、policy、HIGH adapter 完成；MEDIUM 不回归 |
| 2026-07-12 | 信封加密 | AES-256-GCM + 每记录 DEK + AESWrap KEK + AAD + key rotation 完成 |
| 2026-07-12 | 审批状态机 | RESERVED/PENDING_APPROVAL/EXECUTING/终态、唯一幂等域和固定 businessKey 完成 |
| 2026-07-12 | Flowable | 默认单节点审批、非空 BPMN 保留、USER A delegated start、只读表单和幂等回调完成 |
| 2026-07-12 | 查询工具 | 固定 `capability.approval.get` 完成，只按完整原始身份元组查询安全摘要 |

## 5. 首轮 Review 与 Fix

首轮发现 7 项问题并已写入 `fix-plan.md`：回调未装配、跨 USER 幂等复用、回调重新授权缺口、HIGH 提交前动作复核缺口、模型查询失败误判、默认流程/表单/查询工具缺失、送审审计结果码错误。

Fix 后新增：

- 回调重查 policy、固定 flowModelKey、客户端过期/credentialVersion、服务账号、USER A tenant/org、grant、能力版本、业务状态；
- 幂等复用精确比较 actor/serviceUser/org/credentialVersion/capabilityVersion，跨用户碰撞返回 `IDEMPOTENCY_CONFLICT`；
- HIGH 在 elicitation 前重新解析发布动作、递归步骤和可写字段；
- 模型 lookup 失败关闭，只有成功返回空模型时创建默认 BPMN；已有非空 BPMN 永不覆盖；
- `capability.invoke` 可选输出 approvalRequestId，Capability 审计最终 resultCode 为 `PENDING_APPROVAL`；
- 只读审批表单对 token/header/password/secret/API key 类字段二次遮蔽。

## 6. 最终验证证据

| 命令/范围 | 结果 |
|---|---|
| high-risk 模块 `mvn -Penable-tests test` | 24/24 PASS |
| secure-actions 模块 `mvn -Penable-tests test` | 32/32 PASS |
| flow-actions 模块 `mvn -Penable-tests test` | 19/19 PASS |
| flow-client / flow-server / starter-auth | 2/2、3/3、7/7 PASS |
| control-plane / identity / MCP | 29/29、35/35、16/16 PASS |
| `mvn -pl forge-admin-server,forge-flow/forge-flow-server -am package -DskipTests` | 44/44 模块 BUILD SUCCESS |
| `xmllint` + SQL placeholder/tenant + 禁止依赖/transport 扫描 | PASS，无违规命中 |

测试启用说明：仓库默认 `forge.compiler.skip=true`、`forge.tests.skip=true`，模块测试必须使用 `-Penable-tests`。一次使用 `-am` 的测试尝试在无测试引擎的上游 datascope 模块触发 Surefire groups 配置失败，随后按项目既有方式先安装依赖、再执行模块级测试；该尝试未影响最终通过结论。

## 7. 条件限制与清理

- 未启动 Admin 或 Flow 服务，未触发真实 Flyway；V1.0.26 仅做静态验收；
- 未执行真实 Flowable 待办的人工 APPROVE/REJECT E2E；默认模型、委托入口和回调通过模块测试与聚合编译验证；
- 未调用真实大模型；本阶段不包含模型调用；
- 本轮未启动后台服务，无 PID 需要停止；未 commit、未 push。

## 8. 归档前最终验收（2026-07-12 17:42）

- 复跑 `mvn -pl forge-admin-server,forge-flow/forge-flow-server -am package -DskipTests`：44/44 模块 `BUILD SUCCESS`，总耗时 19.567s；
- `xmllint --noout` 校验两个 High Risk Mapper XML：PASS；
- V1.0.26 无 Flyway `${...}` 占位符、`tenant_id=0`、禁止依赖或旧 transport 配置；
- 相关未跟踪文件尾随空白检查与全工作区已跟踪差异 `git diff --check`：PASS；
- 编译仅有工程既有 deprecated/unchecked 提示，不阻断归档；
- 未启动任何服务，未触发真实 Flyway、Flowable 待办或大模型调用，无 PID 需清理。

归档结论：阶段 2.3 代码验收完成，状态置为 `done`；阶段 2 总体环境验收仍为后续 HARD-GATE。
