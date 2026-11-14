# 流程创建时指定处理人的任务自动签收 Tasks

> 变更名：`flow-auto-claim-created-task`
> 关联 Spec：`spec.md`
> 当前状态：执行中（apply），T1/T2 已完成，T4 待端到端验证
> 执行规则：每项任务按「测试 → 实现 → 目标验证 → 人工审查 → 记录」执行；不得跳过既有测试基线。

> 状态标记：`[x]` 已实现且有验证证据；`[~]` 部分实现；`[ ]` 尚未完成。

## 执行状态

- [x] Phase 0：基线与环境证据（API 级死锁复现证据已完成，见 execution-log.md 第 3 节）
- [x] Phase 1：核心修复（handleTaskCreated 自动签收）
- [x] Phase 2：自动化测试（新增 5 用例全过；全模块 128 用例 127 过，1 个既有失败与本变更无关）
- [ ] Phase 3：存量死锁任务处理（依赖待澄清 1 结论）
- [~] Phase 4：端到端回归与验收记录（存量解锁+审批链路已实证，新建任务断言待 F1 部署后执行）

## Phase 0：基线与环境证据（P0）

### [~] T0.1 记录修复前基线

- **范围**：`forge-plugin-flow` 模块、`forge-server/db/`、Git 工作树。
- **步骤**：记录 `git status --short`、`FlowTaskEventListener` 关键行现状（第 169 行 PENDING 写死）、`sys_flow_task` 中死锁任务样本 SQL（`status=0 AND assignee IS NOT NULL`）及结果。
- **验收**：基线证据写入本变更 `execution-log.md`；确认修复前死锁样本可复现（前端无签收按钮、审批报 `FLOW_TASK_NOT_ACTIONABLE`）。
- **进展（2026-09-17）**：Git 工作树干净（无未提交修改，CLAIMED 写入点仍 5 处）；死锁样本 API 级复现已完成（claim → `FLOW_TASK_CLAIM_NOT_ALLOWED`，approve → `FLOW_TASK_NOT_ACTIONABLE`），证据见 `execution-log.md` 第 2-3 节；前端「无签收按钮」待 UI 实测（当前为 todo.vue:1490 代码级证据）。

### [x] T0.2 确认测试命令基线

- **步骤**：`cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow test` 或实际可用模块路径；记录编译与现有测试通过情况。
- **验收**：测试命令与结果写入 `execution-log.md`。
- **进展（2026-09-17）**：发现默认 `forge.tests.skip=true`（`maven.test.skip=${forge.tests.skip}`），**必须追加 `-Penable-tests` profile** 才会编译并运行测试；有效命令：`mvn -o -pl forge-framework/forge-plugin-parent/forge-plugin-flow test -Penable-tests`。基线结果见 execution-log.md 第 9 节。

## Phase 1：核心修复（P0）

### [x] T1.1 handleTaskCreated 增加 assignee 判断

- **文件**：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java`（`handleTaskCreated`，第 167-169 行附近）。
- **实现**：
  - assignee 非空且（owner 为空或 owner = assignee）→ `status = CLAIMED`，并设置 `claimTime = LocalDateTime.now()`；
  - 其余情况保持 `status = PENDING`；
  - 与 `handleTaskAssigned`（第 368-378 行）判断条件保持一致，抽取公共判定或添加注释互指。
- **约束**：状态必须使用 `FlowTaskStatus` 枚举，禁止魔法数字（AGENTS.md 5.9）。
- **验收**：代码 diff 仅涉及该分支；候选人模式行为不变。
- **进展（2026-09-17）**：已实现（diff 18+/3-，仅涉及 `handleTaskCreated`），编译通过（`mvn compile` BUILD SUCCESS）。

### [x] T1.2 创建日志补充状态输出

- **文件**：同上，第 244 行「创建待办任务成功」日志。
- **实现**：日志追加 `status` 字段，便于生产排查区分 PENDING/CLAIMED。
- **验收**：日志包含状态且不影响性能。
- **进展（2026-09-17）**：已实现，日志含 `status={}` 占位（`创建待办任务成功：taskId={}, title={}, assignee={}, status={}, candidateUsers={}, candidateGroups={}`）。

## Phase 2：自动化测试（P0）

### [x] T2.1 单元测试：assignee 非空 → CLAIMED

- **文件**：`forge-plugin-flow` 模块新增/扩展监听器测试（参考现有测试目录结构）。
- **步骤**：构造 TASK_CREATED 事件与 mock mapper，assignee 非空；断言插入镜像 `status=1`、`claimTime` 非空。
- **验收**：测试通过；断言覆盖 owner 为空与 owner=assignee 两种。
- **进展（2026-09-17）**：`FlowTaskEventListenerCreatedAutoClaimTest` 已新增，覆盖 owner 为空与 owner=assignee 两组，断言 `CLAIMED + claimTime` 非空；用例全过（`Tests run: 5, Failures: 0, Errors: 0`）。

### [x] T2.2 单元测试：候选人/候选组模式 → PENDING

- **步骤**：assignee 为空（含无候选、有候选两组用例）；断言 `status=0`、`claimTime` 为空。
- **验收**：测试通过；与修复前行为一致。
- **进展（2026-09-17）**：同测试类覆盖无候选、有候选用户两组，另有 owner≠assignee 边界用例，断言 `PENDING + claimTime` 为空；用例全过。

### [x] T2.3 回归测试：运行时签收/转办链路不变

- **步骤**：验证 `handleTaskAssigned`（运行时 setAssignee 场景）与 `claimTask`、`delegate`、`reassignByInitiator` 现有测试全部通过。
- **验收**：既有测试无回归。
- **进展（2026-09-17）**：全模块回归 `Tests run: 128, Failures: 0, Errors: 1`——唯一失败 `FlowModelServiceImplTest.copyModelPreservesMultiLevelReturnSwitch`（「缺少可信租户上下文」）经 `git stash` 修复前重跑确认同为失败，属既有问题，与本变更无关；`FlowTaskServiceImplStateChangeTest` 等签收/转办相关测试全部通过。

## Phase 3：存量死锁任务处理（P1，依赖待澄清 1）

### [ ] T3.1 存量影响面盘点

- **步骤**：统计目标库 `SELECT COUNT(*) FROM sys_flow_task WHERE status=0 AND assignee IS NOT NULL AND (owner IS NULL OR owner = assignee)`；输出清单（task_id、流程、发起时间）。
- **验收**：盘点结果写入 `execution-log.md`，人工确认修复范围。

### [ ] T3.2 数据修复脚本（如确认需要）

- **文件**：`forge-server/db/mysql/migration/V<下一版本>__fix_pending_assigned_flow_tasks.sql`。
- **实现**：`UPDATE sys_flow_task SET status=1, claim_time=NOW() WHERE status=0 AND assignee IS NOT NULL AND (owner IS NULL OR owner=assignee)`；脚本幂等、附影响行数与回滚说明。
- **验收**：在测试库人工执行验证后，方可进入正式迁移；执行记录与回滚方式写入 `execution-log.md`。

## Phase 4：端到端回归与验收记录（P0）

### [~] T4.1 集成验证：指定人员流程

- **步骤**：本地 dev 环境启动 Flow 服务，发起节点 `assignee = ${initiator}` 的流程；断言镜像 `status=1`；以 assignee 身份携带幂等凭证审批成功。
- **验收**：接口返回成功，`sys_flow_task` 状态流转正确。
- **进展（2026-09-17）**：存量同构任务 `fe947251…`（节点 `assignee=${initiator}`）经管理员转派解锁（`0→1`）后，携带幂等凭证审批成功（`1→2`，含必审要点）；演示记录见 `execution-log.md` 第 4-5 节。新建任务断言镜像 `status=1` 的部分待 F1 修复落地后执行。

### [ ] T4.2 集成验证：候选人流程回归

- **步骤**：发起候选人组节点流程；断言 `status=0`；签收后 `status=1`；审批成功。
- **验收**：全链路正常。

### [ ] T4.3 验收记录与知识沉淀

- **步骤**：将测试结果、命令、证据写入 `test-spec.md` / `execution-log.md`；梳理本问题为平台级坑位记录（拟加入 `code-copilot/memory/pitfalls/backend.md`）。
- **验收**：变更目录四件套齐全（spec/tasks/test-spec/execution-log），经人工审查后执行 `/archive`。
