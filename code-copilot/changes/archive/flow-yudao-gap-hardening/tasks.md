# 任务拆分 - 流程审批核心能力补强
> status: review-ready
> created: 2026-08-26
> spec: [spec.md](./spec.md)
> 执行顺序：P0 候选人内核 → P0 审批人自选/预测 → P0 精确退回 → P1 委派与加减签 → 聚合验收
> 原子性约定：每个 Task 都应能独立编译和验证；涉及资金、权限、状态流转的任务必须在提交前完成人工审查。

## 执行范围和前置条件

- 本变更只实施 `spec.md` 中的新增能力，不重新迁移 `flow-enhancement-suite` 已完成的功能。
- 以下能力作为现有唯一实现保留，只做兼容性回归：
  - `sys_flow_model.allow_multi_return` 多级退回、退回后直送；
  - `PROCESS_START_USER` 发起人自选；
  - `FlowBusinessListDisplayAdapter`、`businessParams`、`displayExtensions` 待办展示扩展；
  - 发起人、处理人、owner 和管理员改派；
  - 模型 Key 自定义、格式校验、租户内唯一和发布后不可修改。
- 不修改 Flowable 原生 `ACT_*` 表，不升级 Flowable 版本，不自动改写已发布流程定义。
- 不使用 Controller 的 `@RequestBody Map` 承接固定字段；只有 `variables`、`nextAssignees` 等动态字段允许使用 Map 属性。
- 所有新持久化查询写入 Mapper XML，并显式处理租户和逻辑删除条件。
- P1 任务不能因已有 `delegate`、`addSign`、`reduceSign` 配置字段而提前向前端暴露可用操作。
- 执行前确认当前工作区已有无关改动（`.DS_Store`、`trip/` 等）并保留，不做清理或重置。

## 任务依赖

```text
Task 0
  └─ Task 1 ─ Task 2 ─ Task 3 ─ Task 4 ─ Task 5
                   └─ Task 6 ─ Task 7 ─ Task 8 ─┐
Task 1 ─────────────┘                            ├─ Task 20
Task 1 ─ Task 9 ─ Task 10 ─ Task 11 ────────────┤
Task 1/2/4 ─ Task 12 ─ Task 13 ─ Task 14 ─ Task 15┘

（P1，须在 P0 稳定且业务确认后）Task 16 ─ Task 17 ─ Task 18 ─ Task 19
```

---

## P0：候选人策略内核和发布门禁

### Task 0：变更基线和已完成功能冻结

- **目标**：建立实施前基线，明确本变更不重复实现既有增强能力。
- **工作内容**：
  - 对照 `flow-enhancement-suite` 的 spec、tasks、实现和测试，记录已完成能力的入口、变量名和兼容约束。
  - 核对本 Spec 中的新增错误码、变量协议和 DTO 字段没有与现有定义冲突。
  - 只做文档静态检查，不修改业务代码。
- **涉及文件**：
  - `code-copilot/changes/flow-enhancement-suite/spec.md`
  - `code-copilot/changes/flow-enhancement-suite/tasks.md`
  - `code-copilot/changes/flow-yudao-gap-hardening/spec.md`
  - `code-copilot/changes/flow-yudao-gap-hardening/tasks.md`
- **验收**：`git diff --check` 通过；任务 1-15 均标注与旧能力的边界。

### Task 1：候选人策略 SPI、上下文和统一解析器

- **目标**：把审批人计算从 `assigneeType` 的集中分支改为可插拔且可复用的策略内核。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/candidate/FlowCandidateStrategy.java`（新增）
  - `.../candidate/FlowCandidateContext.java`（新增）
  - `.../candidate/FlowCandidateStrategyType.java`（新增）
  - `.../candidate/FlowCandidateResolver.java`（新增）
  - 必要时补充现有组织/用户查询 Mapper XML，不在 Service 中拼接复杂 SQL。
- **关键设计**：
  - SPI 至少提供 `getType`、参数校验、按运行任务和按活动预测的计算方法。
  - 首批注册 `custom`、`role`、`dept`、`post`、`initiator`、`leader`、`deptManager`、`spel`、`initiatorSelect`；预留 `approveUserSelect`。
  - 统一归一化为字符串用户 ID，稳定排序、去重并保留历史 XML 兼容。
- **验收**：策略可通过 Spring 注入发现；未知策略、重复注册和必填参数缺失均返回稳定错误；运行时计算和未来节点预测走同一解析入口。

### Task 2：现有审批人计算接入策略内核

- **目标**：将当前审批人解析、发起人自选和低代码/应用流程入口接入 `FlowCandidateResolver`，保持旧协议行为不变。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowNodeConfigServiceImpl.java`
  - `.../service/impl/FlowTaskEventListener.java`
  - `.../service/impl/FlowInstanceServiceImpl.java`
  - `.../service/FlowTaskReceiverResolver.java`（如需调整公共解析接口）
- **关键规则**：
  - 兼容历史 `assigneeType/assigneeValue/assigneeExpr` 和旧 BPMN XML，不自动改写旧模型。
  - 按当前租户过滤用户存在性、启用状态和有效期，禁止跨租户候选人。
  - 支持 `BLOCK`、`START_USER`、`ADMIN`、`AUTO_PASS`、`AUTO_REJECT` 空审批人策略，默认 `BLOCK`。
  - 按 `extConfig.skipStartUser` 处理发起人兼审批人，仅在移除后仍有其他候选人时跳过。
  - `initiatorSelect`/`approveUserSelect` 只从流程变量取人，不再回退到固定 assignee。
- **验收**：旧固定用户、SPEL、角色/部门表达式和 `PROCESS_START_USER` 流程回归通过；候选人为空时按配置执行，默认阻断并记录错误。

### Task 3：候选人策略单元测试

- **目标**：锁定 SPI 注册、参数、租户和空候选人行为，防止预测与运行时分叉。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/test/java/com/mdframe/forge/starter/flow/candidate/FlowCandidateResolverTest.java`（新增）
  - `.../candidate/FlowCandidateStrategyTest.java`（新增）
  - 必要时补充 `FlowNodeConfigServiceImpl` 既有测试。
- **覆盖场景**：注册冲突、未知策略、参数为空、停用/过期用户过滤、跨租户用户、空审批人兜底、发起人跳过、稳定去重和字符串 ID。
- **验收**：模块单测通过，且不依赖真实外部组织服务；外部服务异常时验证失败闭环或明确空审批人策略。

### Task 4：流程发布前候选人校验

- **目标**：在 Flowable Deployment 创建前发现无法计算审批人的模型错误。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/candidate/FlowCandidateDeploymentValidator.java`（新增）
  - `.../service/impl/FlowModelServiceImpl.java`
  - 流程错误码/异常定义所在包（按项目现有异常体系补充）。
- **关键规则**：
  - 遍历主流程及支持的嵌套 UserTask；服务节点、自动通过/拒绝节点不要求人工候选人。
  - 人工节点必须有可识别策略，策略参数必须通过自身校验。
  - 校验 `initiatorSelect` 和 `approveUserSelect` 的节点 Key、多实例集合表达式及 `elementVariable`。
  - 失败时不得创建 Deployment，也不得更新 `FlowModel` 发布状态。
  - 使用 `FLOW_CANDIDATE_STRATEGY_NOT_CONFIGURED`、`FLOW_CANDIDATE_STRATEGY_UNKNOWN`、`FLOW_CANDIDATE_PARAMETER_INVALID`、`FLOW_SELECT_APPROVER_NODE_INVALID`。
- **验收**：合法模型可发布；每种非法模型在部署前失败，数据库无新 Deployment，模型仍保持未发布状态。

### Task 5：发布门禁回归测试和模块验证

- **目标**：验证发布校验不破坏现有模型发布流程。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/test/java/com/mdframe/forge/starter/flow/service/impl/FlowModelServiceImplTest.java`
  - `.../candidate/FlowCandidateDeploymentValidatorTest.java`（新增）
- **覆盖场景**：固定用户、SPEL、角色策略、缺策略、未知策略、缺参数、非法自选节点、部署失败回滚和历史 XML。
- **验证命令**：
  ```bash
  cd forge-server
  mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am test
  ```

---

## P0：审批人自选下一节点

### Task 6：设计器 `approveUserSelect` BPMN 协议

- **目标**：在流程设计器中增加“当前审批人选择下一节点审批人”，并保证 XML 往返稳定。
- **涉及文件**：
  - `forge-admin-ui/src/components/flow-designer/converter/user-task-parser.js`
  - `forge-admin-ui/src/components/flow-designer/converter/user-task-writer.js`
  - `forge-admin-ui/src/components/flow-designer/panel/ApproverAssigneeForm.vue`
  - `forge-admin-ui/src/components/flow-designer/converter/__tests__/user-task-parser-assignee.spec.js`
  - `.../__tests__/json-to-bpmn.spec.js`
  - `.../__tests__/roundtrip.spec.js`
- **关键协议**：
  ```xml
  <bpmn:multiInstanceLoopCharacteristics
      isSequential="false"
      flowable:collection="${PROCESS_APPROVE_USER['<nodeKey>']}"
      flowable:elementVariable="assignee" />
  ```
  - 解析/写回使用模式名 `approveUserSelect`；历史模型缺少该属性时不能误判。
  - 前端和 XML 中用户 ID 保持字符串，不能转换为 JavaScript Number。
- **验收**：新增模式可配置、保存、重新打开；XML → JSON → XML 语义和属性稳定，现有 `initiatorSelect` 不受影响。

### Task 7：审批 DTO、变量合并和后继节点校验

- **目标**：在完成当前任务前接收并校验下一节点审批人，写入 `PROCESS_APPROVE_USER`。
- **涉及文件**：
  - `forge-server/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/dto/FlowTaskApproveDTO.java`
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/FlowTaskService.java`
  - `.../service/impl/FlowTaskServiceImpl.java`
  - 如需拆出路径计算，新增 `.../helper/FlowActivityReachabilityResolver.java`。
- **接口/变量**：
  - DTO 新增 `Map<String, List<String>> nextAssignees`。
  - `PROCESS_APPROVE_USER` 类型为 `Map<String, List<String>>`，Key 为真实后继 UserTask 的 BPMN 节点 Key。
- **处理顺序**：读取历史变量 → 合并本次 `variables`（提交值覆盖同名值）→ 计算真实后继 → 校验只能设置后继 → 校验租户/启用/有效期并去重 → 禁止覆盖已创建节点 → 写回变量后完成任务。
- **错误码**：`FLOW_APPROVE_USER_SELECT_REQUIRED`、`FLOW_APPROVE_USER_SELECT_NODE_NOT_REACHABLE`、`FLOW_APPROVE_USER_SELECT_USER_INVALID`、`FLOW_PROCESS_VARIABLE_MERGE_FAILED`。
- **验收**：空列表、非后继节点、跨租户/停用用户、重复覆盖均拒绝；空变量、部分变量、新变量三种提交均保留正确历史变量；`PROCESS_START_USER` 与新变量互不覆盖。

### Task 8：审批人自选后继单测

- **目标**：覆盖串行、并行和变量边界，确保审批接口不会把审批人写入错误节点。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/test/java/com/mdframe/forge/starter/flow/service/impl/FlowApproveUserSelectTest.java`（新增）
- **覆盖场景**：正常下一节点、并行后继、无后继、空列表、非后继、重复覆盖、跨租户、停用用户、字符串 ID、变量合并和历史 `PROCESS_START_USER` 兼容。
- **验收**：单测验证流程变量最终值和 Flowable 任务创建结果一致；失败时当前任务和变量均不产生部分提交。

---

## P0：下一审批节点与候选人预测

### Task 9：预测 VO、状态枚举和服务能力

- **目标**：提供与实际候选人分配共用解析器的只读预测能力。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/vo/FlowNextApprovalNodeVO.java`（新增）
  - `.../service/FlowTaskService.java`
  - `.../service/impl/FlowTaskServiceImpl.java`
  - `.../dto/TaskFormInfo.java`
  - `.../dto/ProcessNodeInfo.java`
- **关键字段**：`nodeKey`、`nodeName`、`nodeType`、`candidateStrategy`、`candidateSelectable`、`candidateUserIds`、`candidateUserNames`、`status`、`conditionStatus`。
  - `conditionStatus`：`MATCHED`、`NOT_MATCHED`、`UNKNOWN`；无法确定时不能猜测。
  - `TaskFormInfo.nextApprovalNodes` 为只读扩展字段，`returnTargets` 保持兼容。
- **验收**：`getNextApprovalNodes(taskId, variables)` 只读并复用 `FlowCandidateResolver.calculateForActivity`，不复制审批人 switch；预测结果包含真实可达节点和候选人。

### Task 10：预测 API 和前端待办展示

- **目标**：向有权限的审批人展示下一步可能到达的节点及候选人。
- **涉及文件**：
  - `forge-server/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowTaskController.java`
  - `forge-admin-ui/src/api/flow.ts`
  - `forge-admin-ui/src/views/flow/todo.vue`
  - `forge-admin-ui/src/components/flow/FlowTaskDetailShell.vue`
- **接口**：`GET /api/flow/task/{taskId}/next-approvals`。
- **关键规则**：
  - 只读，先做租户、任务归属和数据权限校验。
  - 只返回当前用户有权看到的节点和用户；组织服务不可用时返回 `UNKNOWN` 或明确错误，不伪造候选人。
  - 不改变待办列表已有业务数据扩展协议。
- **验收**：无权 task、跨租户 task 和不存在 task 均拒绝；串行、条件网关、自选节点返回稳定结构；前端展示不泄露其他租户用户。

### Task 11：预测一致性测试

- **目标**：证明预测结果与实际启动的候选人分配使用同一策略内核。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/test/java/com/mdframe/forge/starter/flow/service/impl/FlowNextApprovalNodeTest.java`（新增）
  - 对应前端 API/组件测试文件（按现有测试目录补充）。
- **覆盖场景**：串行 UserTask、条件网关匹配/不匹配/未知、`initiatorSelect`、`approveUserSelect`、空候选人阻断和候选人名称脱敏/权限过滤。
- **验收**：预测候选人集合与实际生成任务一致；条件不可判定时状态为 `UNKNOWN` 而非错误地标记可达。

---

## P0：精确退回和管理员回退

### Task 12：BPMN 退回路径解析器

- **目标**：以 BPMN 执行路径为依据计算可安全退回的活动集合，替代“移动所有活动到单节点”。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/helper/FlowReturnPathResolver.java`（新增）
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/test/java/com/mdframe/forge/starter/flow/helper/FlowReturnPathResolverTest.java`（新增）
- **校验规则**：
  - 目标是同一流程定义中已执行的 UserTask，并沿 BPMN 图从当前节点串行可达。
  - 排除开始/结束事件、服务节点、子流程、调用活动和当前任务本身。
  - 条件网关依据当前变量；无法判断时拒绝猜测。
  - 多实例按 execution 识别，不能只按 `taskDefinitionKey` 去重。
  - 不安全并行分支返回 `FLOW_RETURN_PARALLEL_UNSAFE`，fail-closed。
- **验收**：解析器只读、结果可审计；串行、条件、会签和并行不安全场景均有单测。

### Task 13：用户退回接入共享路径算法

- **目标**：让用户退回使用精确路径解析，同时保留现有多级退回和退回后直送协议。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/FlowTaskService.java`
  - `.../service/impl/FlowTaskServiceImpl.java`
  - `forge-server/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowTaskController.java`
- **关键规则**：
  - 继续支持 `targetActivityId` 和 `allowMultiReturn`；校验与状态迁移分为两个阶段。
  - 只撤回目标路径相关任务；当前任务记退回，其他同路径任务记取消及系统原因。
  - 保留 `FLOW_RETURN_SOURCE_ACTIVITY_ID`、`FLOW_RETURN_TARGET_ACTIVITY_ID` 和退回防自动审批标记。
  - 任何路径校验失败不得调用 Flowable 状态迁移。
- **验收**：串行多级退回、退回后直送、条件路径和并行 fail-closed 行为符合 Spec；本地任务、评论、业务状态和错误日志保持一致。

### Task 14：管理员回退接入共享算法与审计

- **目标**：管理员回退和用户退回使用同一安全路径、锁和本地状态同步语义。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowInstanceServiceImpl.java`
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java`
  - `forge-server/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowMonitorController.java`
  - 对应 `FlowMonitorRollbackDTO` 或服务接口（保持明确 DTO）。
- **关键规则**：
  - 替换 `moveActivityIdsToSingleActivityId(getCurrentActivityIds(...), targetActivityId)` 直移逻辑，改为调用 `FlowReturnPathResolver`。
  - 使用租户内流程锁；同步 `sys_flow_task`、`sys_flow_business`、评论、取消原因和错误审计。
  - 并行不安全时拒绝，不强行合并分支。
- **验收**：管理员和普通用户对同一流程得到一致的路径判断、状态、评论和错误码；失败可重试且不会留下部分迁移状态。

### Task 15：退回状态和路径回归测试

- **目标**：覆盖流程引擎状态、本地镜像和业务回调的一致性。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/test/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImplStateChangeTest.java`
  - `.../service/impl/FlowInstanceServiceImplRollbackTest.java`（新增）
  - `.../helper/FlowReturnPathResolverTest.java`
- **覆盖场景**：串行多级退回、条件分支、并行 fail-closed、会签 execution、退回后直送、管理员回退、当前任务退回标记、其他任务取消、业务状态、评论、错误审计和流程锁。
- **验收**：
  ```bash
  cd forge-server
  mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am test
  ```
  失败场景确认 Flowable、本地任务和业务表不存在半完成状态。

---

## P1：真实委派、前后加签和减签（业务确认后实施）

> P1 不阻塞 P0。若没有明确业务场景、权限模型和 Flowable 7.0.1 回归结果，保留现有转派行为，不新增前端入口。

### Task 16：真实委派与现有转派语义分离

- **目标**：实现 Flowable `DelegationState.PENDING` 委派，同时不破坏现有 `/delegate` 转派兼容语义。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/FlowTaskService.java`
  - `.../service/impl/FlowTaskServiceImpl.java`
  - `forge-server/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowTaskController.java`
  - `.../dto/FlowTaskDelegateDTO.java`
- **关键规则**：
  - 现有 `/api/flow/task/delegate` 继续直接修改 assignee；新增明确命名接口（如 `/delegate-task`）承载真实委派。
  - 委派保留原处理人为 owner，调用 `delegateTask`；被委派人只能 `resolveTask`，不能直接完成主任务。
  - `PENDING`、owner、assignee 在待办详情和历史可见；resolve 后恢复 owner 的待办状态。
- **验收**：委派、resolve、通过/驳回/终止的状态边界和权限均有测试；旧客户端调用结果不变。

### Task 17：本地任务委派/加签镜像字段迁移

- **目标**：为委派和父子加签任务提供本地展示、查询和审计镜像，不覆盖 Flowable 原生状态。
- **涉及文件**：
  - `forge-server/db/migration/V<next>__add_flow_task_delegation_and_sign_fields.sql`（实施时根据迁移历史确定版本）
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowTask.java`
  - `.../resources/mapper/FlowTaskMapper.xml`
- **字段/索引**：`delegation_state varchar(20)`、`parent_task_id varchar(64)`、`scope_type varchar(20)`，以及 `(tenant_id, process_instance_id, parent_task_id)` 索引；字段可空，历史任务回填为空。
- **验收**：Flyway 脚本具备防重复保护；实体显式处理逻辑删除字段；普通审批查询不受影响；迁移结果可通过 `forge_schema_history` 核对。

### Task 18：前加签、后加签和减签

- **目标**：基于 Flowable 父子任务关系实现完整加签树生命周期。
- **涉及文件**：
  - `forge-server/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/dto/FlowTaskSignDTO.java`（新增）
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/FlowTaskSignService.java`（新增）
  - `.../service/impl/FlowTaskSignServiceImpl.java`（新增）
  - `forge-server/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowTaskSignController.java`（新增）
- **关键规则**：
  - 受节点 `addSign`/`reduceSign` 权限控制，用户必须通过租户、启用状态和有效期校验。
  - 前加签：主任务等待，子任务完成后恢复；后加签：主任务处理中，子任务完成后按节点策略完成/恢复。
  - 减签只能取消当前加签树子任务，不能删除普通审批任务；禁止重复 assignee/owner。
  - 每个动作、子任务完成和父任务恢复产生评论、本地镜像和事件。
- **验收**：Flowable 状态、父子关系、本地任务状态、评论和事件完全一致；异常时 fail-closed。

### Task 19：委派/加减签回归测试

- **目标**：验证 P1 不影响 P0 和现有转派。
- **涉及文件**：按实现新增服务、Controller 和测试文件，至少包含 `FlowTaskDelegationTest`、`FlowTaskSignServiceTest`。
- **覆盖场景**：委派进入 `PENDING`、被委派人 resolve、转派兼容接口、前加签、后加签、减签、父子任务递归、重复用户、权限、租户、启用状态、评论、本地事件和失败回滚。
- **验收**：只有 P1 业务开关开启且权限通过时可调用；模块测试和 Flowable 7.0.1 集成回归通过。

---

## 最终聚合任务

### Task 20：P0 集成验证、文档回填和交付检查

- **目标**：完成 P0 全链路验证，记录结果并决定 P1 是否进入实施。
- **验证命令**：
  ```bash
  (cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am test)

  (source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && \
    pnpm exec vitest run src/components/flow-designer && pnpm build)

  git diff --check
  rg -n '\$\{[^}]+\}' forge-server/db/migration
  ```
- **检查内容**：
  - P0 候选人策略、发布门禁、审批人自选、预测、精确退回和管理员回退验收标准全部通过。
  - 旧 `flow-enhancement-suite` 回归测试保持通过，未引入平行变量、配置表或 API。
  - Mapper XML、Flyway（如 P1 已启用）、DTO、枚举和租户/逻辑删除约束通过静态审查。
  - 不自动启动 Admin、Flow、MySQL、Redis；不执行会修改真实流程数据的 E2E。
  - 将命令、结果、警告、跳过项和服务清理情况追加到 `test-spec.md`、`execution-log.md`；同步更新 `tasks.md` 状态和 `spec.md` 迁移完成定义。
- **交付门槛**：P0 全部通过后才能评估 P1；P1 未实施时，前端不得显示真实委派/加减签为可用操作。

## 暂缓项

- `startUserDeptLeader` / `startUserDeptLeaderMulti`、`formUser` / `formDeptLeader`：P1，待候选人内核稳定和业务确认后实施。
- `userGroup`：P2，暂不新增独立用户组表。
- 流程附件权限/生命周期、节点级 HTTP Trigger、复杂子流程/跨流程复制：另立 Spec。
- 真实 E2E：需要用户提供可用的 Admin、Flow、MySQL、Redis 环境后单独执行，不作为本轮自动验收前置条件。
